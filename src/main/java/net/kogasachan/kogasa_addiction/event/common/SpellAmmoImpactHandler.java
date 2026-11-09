package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.util.KogasaAmmoService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraftforge.event.entity.ProjectileImpactEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 需要瞄准目标的法术弹药：箭矢命中后让发射者朝命中点施法。 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SpellAmmoImpactHandler {

    private SpellAmmoImpactHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onImpact(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)) return;
        if (!(arrow.getOwner() instanceof ServerPlayer player)) return;
        boolean cast = KogasaAmmoService.castOnImpact(arrow, event.getRayTraceResult(), player);
        if (cast) {
            // 1.20.1 中 setCanceled 对 ProjectileImpactEvent 已弃用；
            // STOP_AT_CURRENT_NO_DAMAGE 表示停在当前命中点且不结算普通箭伤害。
            event.setImpactResult(ProjectileImpactEvent.ImpactResult.STOP_AT_CURRENT_NO_DAMAGE);
            arrow.discard();
        }
    }
}
