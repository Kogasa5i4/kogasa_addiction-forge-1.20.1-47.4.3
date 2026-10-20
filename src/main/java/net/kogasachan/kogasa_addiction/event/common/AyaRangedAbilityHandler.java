package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AyaRangedAbilityHandler {

    @SubscribeEvent
    public static void onProjectileHit(LivingHurtEvent event) {
        if (!Config.ENABLE_AYA_RANGED.get()) return;

        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) return;

        Entity owner = arrow.getOwner();
        if (!(owner instanceof ServerPlayer player)) return;

        ItemStack weapon = KogasaAbilityHelper.getHeldKogasaEquipment(player);
        if (weapon.isEmpty()) return;
        if (!KogasaAbilityData.hasAbility(weapon, KogasaAbility.AYA)) return;

        int speedDuration = Config.AYA_SPEED_DURATION_SECONDS.get() * 20;
        int speedAmplifier = Config.AYA_SPEED_LEVEL.get() - 1;
        int hasteDuration = Config.AYA_HASTE_DURATION_SECONDS.get() * 20;
        int hasteAmplifier = Config.AYA_HASTE_LEVEL.get() - 1;

        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED, speedDuration, speedAmplifier, false, true));
        player.addEffect(new MobEffectInstance(
                MobEffects.DIG_SPEED, hasteDuration, hasteAmplifier, false, true));
    }
}
