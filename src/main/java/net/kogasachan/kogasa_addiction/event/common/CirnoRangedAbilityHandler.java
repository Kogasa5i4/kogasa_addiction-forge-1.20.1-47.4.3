package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CirnoRangedAbilityHandler {

    @SubscribeEvent
    public static void onProjectileHit(LivingHurtEvent event) {
        if (!Config.ENABLE_CIRNO_RANGED.get()) return;

        Entity direct = event.getSource().getDirectEntity();

        ServerPlayer player = null;
        if (direct instanceof Arrow arrow
                && arrow.getOwner() instanceof ServerPlayer sp) {
            player = sp;
        } else if (direct instanceof FireworkRocketEntity rocket
                && rocket.getOwner() instanceof ServerPlayer sp) {
            player = sp;
        }
        if (player == null) return;

        ItemStack weapon = KogasaAbilityHelper.getHeldKogasaEquipment(player);
        if (weapon.isEmpty()) return;
        if (!KogasaAbilityData.hasAbility(weapon, KogasaAbility.CIRNO)) return;

        boolean hasPotionAmmo = KogasaAbilityData.getAmmoType(weapon)
                == KogasaAbilityData.AmmoType.POTION;

        if (!hasPotionAmmo) {
            int duration = Config.CIRNO_SLOWNESS_DURATION_TICKS.get();
            int amplifier = Config.CIRNO_SLOWNESS_LEVEL.get() - 1;
            event.getEntity().addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN, duration, amplifier, false, true));
        }
    }
}