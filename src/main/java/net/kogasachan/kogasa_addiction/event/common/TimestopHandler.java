package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.kogasachan.kogasa_addiction.network.TimestopStatePacket;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 咲夜能力：时间停止。
 *
 * <p>状态模型是**每次发动一个独立会话**（{@link #SESSIONS}: owner UUID → 结束时刻），
 * 而不是原来的全服单例。这样：
 * <ul>
 *   <li>不同玩家可以各自发动自己的时停，互不阻塞；</li>
 *   <li>每个会话有独立的结束时间与作用半径；</li>
 *   <li>客户端按玩家同步"自己的时停是否在生效"。</li>
 * </ul>
 *
 * <p>冻结分两条路：
 * <ul>
 *   <li>生物：{@link LivingEvent.LivingTickEvent} 取消 tick（兜底）；</li>
 *   <li>所有实体（含掉落物、箭矢、矿车等非生物实体）：由
 *       {@code ServerLevelEntityFreezeMixin} 在 {@code ServerLevel#tickNonPassenger} 头部取消，
 *       这是真正的"完全不 tick"，而不是只把速度清零。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TimestopHandler {

    /**
     * 进行中的时停会话：拥有者 UUID → 结束时刻（世界 gameTime）。
     * 只在服务端主线程读写。
     */
    private static final Map<UUID, Long> SESSIONS = new HashMap<>();

    /**
     * 每 tick 刷新的拥有者快照，供 mixin 里的 {@link #shouldFreeze(Entity)} 快速判断，
     * 避免对每个实体都去 ServerPlayerList 查表。
     */
    private static final List<ServerPlayer> ACTIVE_OWNERS = new ArrayList<>();

    /** 本 tick 生效的时停半径平方（配置每 tick 读一次，避免每个实体都读） */
    private static double activeRadiusSquared = 0.0D;

    private static final String FROZEN_TAG = "KogasaTimeStopFrozen";

    /** 玩家持久数据：下一次允许发动时停的世界时间（gameTime，0 表示从未用过） */
    private static final String TAG_TIMESTOP_READY_AT = "KogasaTimestopReadyAt";

    private TimestopHandler() {
    }

    // ==================== 查询接口（供 mixin / 其它 handler 使用） ====================

    /** 当前是否有任意玩家处在时停中 */
    public static boolean isTimestopActive() {
        return !SESSIONS.isEmpty();
    }

    /** 该玩家自己是否正在时停中 */
    public static boolean isTimestopActiveFor(UUID playerId) {
        return playerId != null && SESSIONS.containsKey(playerId);
    }

    /**
     * 该实体当前是否应被冻结。
     * 每 tick 对每个被 tick 的实体调用一次，必须足够轻量。
     */
    public static boolean shouldFreeze(Entity entity) {
        if (SESSIONS.isEmpty() || ACTIVE_OWNERS.isEmpty()) return false;
        // 玩家（含发动者本人）不被冻结
        if (entity instanceof Player) return false;

        for (ServerPlayer owner : ACTIVE_OWNERS) {
            if (owner == null || owner.isRemoved()) continue;
            if (entity.level() != owner.level()) continue;
            if (entity.distanceToSqr(owner) <= activeRadiusSquared) return true;
        }
        return false;
    }

    // ==================== 时停触发 ====================

    public static void tryActivateTimestop(ServerPlayer player) {
        UUID playerId = player.getUUID();

        // 自己已经有一个会话在进行
        if (SESSIONS.containsKey(playerId)) {
            player.displayClientMessage(Component.translatable(
                    "message.kogasa_addiction.timestop.active").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 计算玩家身上的咲夜能力装备件数
        int count = getSakuyaEquipmentCount(player);
        if (count <= 0) {
            player.displayClientMessage(Component.translatable(
                    "message.kogasa_addiction.timestop.no_equip").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 冷却检查：用世界时间并存在玩家的持久数据里，重登 / 换维度都不会清零
        CompoundTag data = player.getPersistentData();
        long now = player.level().getGameTime();
        long readyAt = data.getLong(TAG_TIMESTOP_READY_AT);
        if (now < readyAt) {
            long remainSeconds = (readyAt - now + 19L) / 20L;
            player.displayClientMessage(Component.translatable(
                    "message.kogasa_addiction.timestop.cooldown", remainSeconds)
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 计算持续时间（秒转世界刻，1 秒 = 20 刻）
        int seconds = count * Config.SAKUYA_TIMESTOP_SECONDS_PER_EQUIPMENT.get();
        long endTime = now + seconds * 20L;

        SESSIONS.put(playerId, endTime);

        // 冷却从"本次时停结束"开始计时
        int cooldownSeconds = Config.SAKUYA_TIMESTOP_COOLDOWN_SECONDS.get();
        data.putLong(TAG_TIMESTOP_READY_AT, endTime + cooldownSeconds * 20L);

        broadcastState();

        // 播放音效与提示
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.5F);
        KogasaAdvancementHelper.grant(player, "perfect_elegant");
        player.displayClientMessage(Component.translatable(
                "message.kogasa_addiction.timestop.start", seconds).withStyle(ChatFormatting.BLUE), false);
    }

    // ==================== 每个 tick 维护会话 ====================

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;

        if (SESSIONS.isEmpty()) {
            if (!ACTIVE_OWNERS.isEmpty()) ACTIVE_OWNERS.clear();
            return;
        }

        MinecraftServer server = event.getServer();
        activeRadiusSquared = Math.pow(Config.SAKUYA_TIMESTOP_RADIUS.get(), 2);
        ACTIVE_OWNERS.clear();

        boolean changed = false;
        Iterator<Map.Entry<UUID, Long>> it = SESSIONS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());

            // 拥有者掉线：直接结束这个会话
            if (owner == null) {
                it.remove();
                changed = true;
                continue;
            }

            // 到时间了：结束并提示
            if (owner.level().getGameTime() >= entry.getValue()) {
                it.remove();
                changed = true;
                owner.displayClientMessage(Component.translatable(
                        "message.kogasa_addiction.timestop.end").withStyle(ChatFormatting.GRAY), true);
                continue;
            }

            ACTIVE_OWNERS.add(owner);
        }

        if (changed) {
            broadcastState();
        }

        // 所有会话都结束了 → 清理残留在实体上的冻结标记与重力状态
        if (SESSIONS.isEmpty()) {
            ACTIVE_OWNERS.clear();
            clearFrozenMarks(server);
        }
    }

    // ==================== 生物 tick 兜底冻结 ====================

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (SESSIONS.isEmpty()) return;
        if (shouldFreeze(event.getEntity())) {
            event.setCanceled(true);
        }
    }

    // ==================== 客户端状态同步 ====================

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendStateTo(player);
        }
    }

    /**
     * 逐玩家下发"你自己的时停是否在生效"，而不是全服一个布尔量，
     * 这样多人服里别人发动时停不会让本地 tooltip 显示"不可用"。
     */
    private static void broadcastState() {
        MinecraftServer server = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            sendStateTo(player);
        }
    }

    private static void sendStateTo(ServerPlayer player) {
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TimestopStatePacket(isTimestopActiveFor(player.getUUID())));
    }

    // ==================== 崩溃 / 关服残留清理 ====================

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        clearStaleFreeze(event.getEntity());
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        for (ServerLevel level : event.getServer().getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                clearStaleFreeze(entity);
            }
        }
    }

    /**
     * 服务器停止（退出世界 / 关服）时复位全部会话。
     * 这些状态原本是 JVM 级静态，会跨世界残留：单人游戏退出世界再进新世界时 UUID 不变，
     * 结束时刻仍是旧世界的 gameTime，会导致新世界开场就自动进入时停。
     */
    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        boolean hadSessions = !SESSIONS.isEmpty();

        SESSIONS.clear();
        ACTIVE_OWNERS.clear();
        activeRadiusSquared = 0.0D;

        if (hadSessions) {
            clearFrozenMarks(event.getServer());
        }
    }

    private static void clearStaleFreeze(Entity entity) {
        // 冻结标记只会写非玩家实体，避免给所有实体创建 ForgeData
        if (entity instanceof Player) return;

        CompoundTag data = entity.getPersistentData();
        if (data.getBoolean(FROZEN_TAG)) {
            entity.setNoGravity(false);
            data.remove(FROZEN_TAG);
        }
    }

    private static void clearFrozenMarks(MinecraftServer server) {
        if (server == null) return;

        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                clearStaleFreeze(entity);
            }
        }
    }

    // ==================== 辅助方法 ====================

    private static int getSakuyaEquipmentCount(Player player) {
        int count = 0;
        // 检查主副手
        if (hasSakuyaAbility(player.getMainHandItem())) count++;
        if (hasSakuyaAbility(player.getOffhandItem())) count++;
        // 检查四件护甲
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (hasSakuyaAbility(player.getItemBySlot(slot))) count++;
        }
        return count;
    }

    private static boolean hasSakuyaAbility(ItemStack stack) {
        return stack.getItem() instanceof IKogasaEquipment
                && KogasaAbilityData.hasAbility(stack, KogasaAbility.SAKUYA);
    }
}
