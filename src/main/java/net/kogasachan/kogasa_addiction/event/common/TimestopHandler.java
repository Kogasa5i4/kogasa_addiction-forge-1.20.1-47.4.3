package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TimestopHandler {

    public static boolean IS_FROZEN = false;
    public static UUID OWNER_UUID = null;

    // 记录时停结束的时间点（世界时间）
    private static long TIMESTOP_END_TIME = 0;

    private static final double RADIUS_SQUARED = 20.0 * 20.0;

    // ==================== 时停触发与停止逻辑 ====================

    public static void tryActivateTimestop(ServerPlayer player) {
        // 如果已经有时停在运行，则不允许重复触发
        if (IS_FROZEN) {
            player.displayClientMessage(Component.translatable("message.kogasa_addiction.timestop.active").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 计算玩家身上的咲夜能力装备件数
        int count = getSakuyaEquipmentCount(player);
        if (count <= 0) {
            player.displayClientMessage(Component.translatable("message.kogasa_addiction.timestop.no_equip").withStyle(ChatFormatting.RED), true);
            return;
        }

        // 发动时停
        IS_FROZEN = true;
        OWNER_UUID = player.getUUID();

        // 计算持续时间（毫秒转世界刻，1秒 = 20刻）
        int seconds = count * Config.SAKUYA_TIMESTOP_SECONDS_PER_EQUIPMENT.get();
        long durationTicks = seconds * 20L;
        TIMESTOP_END_TIME = player.level().getGameTime() + durationTicks;

        // 播放音效与提示
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BELL_RESONATE, SoundSource.PLAYERS, 1.0F, 0.5F);
        KogasaAdvancementHelper.grant(player, "perfect_elegant");
        player.displayClientMessage(Component.translatable("message.kogasa_addiction.timestop.start", seconds).withStyle(ChatFormatting.BLUE), false);
    }

    // 在 ServerTickEvent 中检查是否超时
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!IS_FROZEN || OWNER_UUID == null) return;

        ServerPlayer owner = event.getServer().getPlayerList().getPlayer(OWNER_UUID);
        if (owner == null) {
            stopTimestop();
            return;
        }

        // 检查时间是否已到
        if (owner.level().getGameTime() >= TIMESTOP_END_TIME) {
            stopTimestop();
            owner.displayClientMessage(Component.translatable("message.kogasa_addiction.timestop.end").withStyle(ChatFormatting.GRAY), true);
            return;
        }

        // --- 这里保留你之前的逻辑：冻结非生物实体 ---
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level != owner.level()) continue;
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof LivingEntity) continue;
                if (entity.distanceToSqr(owner) <= RADIUS_SQUARED) {
                    entity.setDeltaMovement(Vec3.ZERO);
                    entity.setNoGravity(true);
                    entity.setOldPosAndRot();
                }
            }
        }
    }

    // ==================== 冻结生物实体 ====================

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!IS_FROZEN) return;
        LivingEntity entity = event.getEntity();
        if (entity.getUUID().equals(OWNER_UUID)) return;
        if (entity instanceof Player) return;
        if (isWithinRadius(entity)) {
            event.setCanceled(true);
        }
    }

    // ==================== 辅助方法 ====================

    private static boolean isWithinRadius(Entity entity) {
        if (OWNER_UUID == null || entity.getServer() == null) return false;
        ServerPlayer owner = entity.getServer().getPlayerList().getPlayer(OWNER_UUID);
        if (owner == null || entity.level() != owner.level()) return false;
        return entity.distanceToSqr(owner) <= RADIUS_SQUARED;
    }

    private static int getSakuyaEquipmentCount(Player player) {
        int count = 0;
        // 检查主副手
        if (hasSakuyaAbility(player.getMainHandItem())) count++;
        if (hasSakuyaAbility(player.getOffhandItem())) count++;
        // 检查四件护甲
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            if (hasSakuyaAbility(player.getItemBySlot(slot))) count++;
        }
        return count;
    }

    private static boolean hasSakuyaAbility(ItemStack stack) {
        return stack.getItem() instanceof IKogasaEquipment &&
                KogasaAbilityData.hasAbility(stack, KogasaAbility.SAKUYA); // 确保你的枚举里有 SAKUYA
    }

    public static void stopTimestop() {
        IS_FROZEN = false;
        OWNER_UUID = null;
        TIMESTOP_END_TIME = 0;
    }
}