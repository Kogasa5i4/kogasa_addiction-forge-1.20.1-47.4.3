package net.kogasachan.kogasa_addiction.event;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KoishiAbilityHandler {

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 找出攻击者（支持近战 + 远程）
        ServerPlayer player = resolveAttacker(event);
        if (player == null) return;

        ItemStack held = player.getMainHandItem();
        if (!isKoishiEquipment(held)) return;

        // 75% 概率造成 1.5 倍伤害
        if (player.getRandom().nextFloat() >= Config.KOISHI_CRIT_CHANCE.get().floatValue()) {
            return;
        }

        float multiplier = Config.KOISHI_CRIT_MULTIPLIER.get().floatValue();
        event.setAmount(event.getAmount() * multiplier);

        DamageSource magicSource = player.damageSources().indirectMagic(player, player);
        event.getEntity().hurt(magicSource, event.getAmount() * multiplier);
        event.setCanceled(true);

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

    /**
     * 从伤害来源解析攻击者，支持近战和弹射物
     */
    private static ServerPlayer resolveAttacker(LivingHurtEvent event) {
        Entity attacker = event.getSource().getEntity();
        if (attacker instanceof ServerPlayer sp) return sp;

        if (event.getSource().getDirectEntity() instanceof Projectile proj
                && proj.getOwner() instanceof ServerPlayer owner) {
            return owner;
        }
        return null;
    }

    private static boolean isKoishiEquipment(ItemStack stack) {
        if (!(stack.getItem() instanceof IKogasaEquipment)) return false;
        return KogasaAbilityData.hasAbility(stack, KogasaAbility.KOISHI);
    }
}