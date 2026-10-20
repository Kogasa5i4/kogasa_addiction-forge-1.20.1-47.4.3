package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaRangedAbilityHandler {

    private static final String STREAK_KEY = "KogasaRangedHitStreak";
    private static final String LAST_HIT_TIME_KEY = "KogasaLastRangedHitTime";
    private static final int STREAK_RESET_TICKS = 60; // 3秒

    @SubscribeEvent
    public static void onProjectileHit(LivingHurtEvent event) {
        if (!Config.ENABLE_KOGASA_RANGED.get()) return;

        DamageSource source = event.getSource();
        if (!(source.getDirectEntity() instanceof AbstractArrow arrow)) return;

        Entity owner = arrow.getOwner();
        if (!(owner instanceof ServerPlayer player)) return;

        ItemStack weapon = KogasaAbilityHelper.getHeldKogasaEquipment(player);
        if (weapon.isEmpty()) return;
        if (!KogasaAbilityData.hasAbility(weapon, KogasaAbility.KOGASA)) return;

        long now = player.level().getGameTime();
        CompoundTag data = player.getPersistentData();
        long lastHit = data.getLong(LAST_HIT_TIME_KEY);
        int resetTicks = Config.KOGASA_STREAK_RESET_SECONDS.get() * 20;

        int streak;
        if (now - lastHit <= resetTicks) {
            streak = data.getInt(STREAK_KEY) + 1;
        } else {
            streak = 0;
        }

        int max = Config.KOGASA_STREAK_MAX.get();
        if (max > 0 && streak > max) streak = max;

        data.putInt(STREAK_KEY, streak);
        data.putLong(LAST_HIT_TIME_KEY, now);

        double bonus = Config.KOGASA_STREAK_BONUS.get();
        float multiplier = 1.0F + (float) (streak * bonus);
        event.setAmount(event.getAmount() * multiplier);
    }
}
