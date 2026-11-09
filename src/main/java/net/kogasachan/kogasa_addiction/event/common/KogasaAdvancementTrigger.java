package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingEquipmentChangeEvent;
import net.minecraftforge.event.entity.player.*;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 成就检查的统一触发器。
 *
 * <p>不在每个业务方法里散落 checkAndGrant；这里用 LOWEST 优先级监听会改变装备/背包
 * 的事件，并加一个 5 秒一次的 tick 兜底。实际扫描做去抖，避免铁砧/容器事件风暴。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KogasaAdvancementTrigger {

    private static final int DEBOUNCE_TICKS = 100;
    private static final Map<UUID, Long> LAST_SCAN = new ConcurrentHashMap<>();

    private KogasaAdvancementTrigger() {
    }

    private static void request(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        long now = serverPlayer.level().getGameTime();
        Long last = LAST_SCAN.get(serverPlayer.getUUID());
        if (last != null && now - last < DEBOUNCE_TICKS) return;
        LAST_SCAN.put(serverPlayer.getUUID(), now);
        KogasaAdvancementHelper.scanAllAndGrant(serverPlayer);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (event.player.tickCount % DEBOUNCE_TICKS != 0) return;
        request(event.player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilRepair(AnvilRepairEvent event) {
        request(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        request(event.getPlayer());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onCrafted(PlayerEvent.ItemCraftedEvent event) {
        request(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onSmelted(PlayerEvent.ItemSmeltedEvent event) {
        request(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPickup(EntityItemPickupEvent event) {
        request(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onContainer(PlayerContainerEvent event) {
        request(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onEquipmentChange(LivingEquipmentChangeEvent event) {
        if (event.getEntity() instanceof Player player) request(player);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onXp(PlayerXpEvent.LevelChange event) {
        request(event.getEntity());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() != null) request(event.getPlayer());
    }
}
