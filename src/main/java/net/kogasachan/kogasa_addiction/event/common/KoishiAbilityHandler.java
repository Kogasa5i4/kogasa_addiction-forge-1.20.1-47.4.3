package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityHelper;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KoishiAbilityHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        DamageSource source = event.getSource();
        // 只处理真正的近战伤害：远程伤害由 KoishiRangedAbilityHandler 独立结算，
        // 避免同一击同时吃近战暴击与远程暴击，也避免在 LivingHurtEvent 内再次 hurt 造成嵌套结算。
        if (!(source.getDirectEntity() instanceof ServerPlayer player)) return;
        if (source.is(KoishiRangedAbilityHandler.KOISHI_RANGED)) return;

        ItemStack held = player.getMainHandItem();
        if (!KogasaAbilityHelper.isEquipmentWithAbility(held, KogasaAbility.KOISHI)) return;

        // 按配置概率造成暴击（默认 75% 概率、1.5 倍伤害）
        if (player.getRandom().nextFloat() >= Config.KOISHI_CRIT_CHANCE.get().floatValue()) {
            return;
        }

        float multiplier = Config.KOISHI_CRIT_MULTIPLIER.get().floatValue();
        event.setAmount(event.getAmount() * multiplier);

        // 暴击粒子
        if (event.getEntity().level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.CRIT,
                    event.getEntity().getX(),
                    event.getEntity().getY() + event.getEntity().getBbHeight() * 0.5,
                    event.getEntity().getZ(),
                    8, 0.3, 0.3, 0.3, 0.1);
        }

        // 暴击音效
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.PLAYERS, 0.8F, 1.2F);
    }
}