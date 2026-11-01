package net.kogasachan.kogasa_addiction.ability.tooltip;

import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * AYA（传统的幻想记者）的 tooltip：新闻值、倍率与三类记录计数。
 */
public final class AyaAbilityTooltip implements KogasaAbilityTooltip {

    @Override
    public void append(KogasaAbilityTooltipContext ctx) {
        double news = KogasaAbilityData.getNewsValue(ctx.stack());
        int killed = KogasaAbilityData.getKilledMobCount(ctx.stack());
        int mined = KogasaAbilityData.getMinedBlockCount(ctx.stack());
        int damage = KogasaAbilityData.getDamageTakenCount(ctx.stack());

        ctx.add(Component.translatable(
                        "tooltip.kogasa_addiction.aya.news_value",
                        KogasaNumberFormatHelper.formatPercentTrimmed(news, 2),
                        "×" + KogasaNumberFormatHelper.formatTrimmed(1.0 + news, 3))
                .withStyle(ChatFormatting.GOLD));
        ctx.add(Component.translatable(
                        "tooltip.kogasa_addiction.aya.records",
                        killed, mined, damage)
                .withStyle(ChatFormatting.GRAY));
    }
}
