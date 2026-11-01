package net.kogasachan.kogasa_addiction.ability.tooltip;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * KOGASA（愉快的遗忘之伞）的 tooltip：强化倍率与综合倍率。
 *
 * <p>命名特意不用 KogasaAbilityTooltip —— 那个名字是渲染器接口，
 * 与接口同名的实现类会造成循环继承。
 */
public final class KogasaUmbrellaAbilityTooltip implements KogasaAbilityTooltip {

    @Override
    public void append(KogasaAbilityTooltipContext ctx) {
        double reMult = KogasaEquipmentHelper.getReinforcementMultiplier(ctx.stack());
        double totalMult = KogasaEquipmentHelper.getTotalMultiplier(ctx.stack());

        ctx.add(Component.translatable(
                        "tooltip.kogasa_addiction.multiplier.reinforcement",
                        KogasaNumberFormatHelper.formatTrimmed(reMult, 2))
                .withStyle(ChatFormatting.YELLOW));

        ctx.add(Component.translatable(
                        "tooltip.kogasa_addiction.multiplier.total",
                        KogasaNumberFormatHelper.formatTrimmed(totalMult, 3))
                .withStyle(ChatFormatting.GOLD));
    }
}
