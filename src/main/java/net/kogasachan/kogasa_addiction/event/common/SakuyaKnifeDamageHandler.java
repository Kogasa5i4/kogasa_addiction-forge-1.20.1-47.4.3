package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.entity.projectile.SakuyaKnifeEntity;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 把咲夜飞刀的强化/成长倍率并入父类那一次伤害结算。
 *
 * <p>旧实现在 {@code SakuyaKnifeEntity#onHitEntity} 里先调 {@code super.onHitEntity} 造成基础伤害，
 * 再额外调一次 {@code target.hurt(...)} 补差额。但父类那次伤害已经把 {@code invulnerableTime}
 * 置为 20 并把 {@code lastHurt} 记为本次伤害，第二次 hurt 会命中
 * {@code invulnerableTime > 10 && amount <= lastHurt → return false} 的分支：
 * 倍率 ≤ 2 时额外伤害完全丢失，> 2 时也只补上差值。而且那个实现把基数硬编码成 8.0F，
 * 忽略了父类里按附魔追加的伤害。
 *
 * <p>改为在 {@link LivingHurtEvent} 里直接放大这一次伤害，因此：
 * 只结算一次（不会再被无敌帧吃掉）、基数取父类算出的真实伤害（含附魔）、
 * 击杀归属与伤害类型都不变。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SakuyaKnifeDamageHandler {

    private SakuyaKnifeDamageHandler() {
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        // 只处理本模组飞刀直接造成的伤害；玩家手持飞刀近战攻击时 directEntity 是玩家，不会进来
        if (!(event.getSource().getDirectEntity() instanceof SakuyaKnifeEntity knife)) return;

        ItemStack knifeStack = knife.tridentItem;
        if (knifeStack == null || knifeStack.isEmpty()) return;

        double multiplier = KogasaEquipmentHelper.getTotalMultiplier(knifeStack);
        if (multiplier <= 1.0) return;

        event.setAmount((float) (event.getAmount() * multiplier));
    }
}
