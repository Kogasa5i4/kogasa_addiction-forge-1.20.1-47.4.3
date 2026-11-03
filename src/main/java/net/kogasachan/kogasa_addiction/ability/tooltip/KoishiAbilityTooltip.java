package net.kogasachan.kogasa_addiction.ability.tooltip;
import net.kogasachan.kogasa_addiction.event.client.ClientConfigCache;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

/**
 * KOISHI（紧闭的恋之瞳）的 tooltip：近战暴击 / 远程穿甲暴击与特殊目标。
 */
public final class KoishiAbilityTooltip implements KogasaAbilityTooltip {

    @Override
    public void append(KogasaAbilityTooltipContext ctx) {
        boolean isRangedWeapon = ctx.stack().getItem() instanceof BowItem
                || ctx.stack().getItem() instanceof CrossbowItem;

        if (isRangedWeapon) {
            if (ClientConfigCache.get(Config.ENABLE_KOISHI_RANGED)) {
                double mult = ClientConfigCache.get(Config.KOISHI_RANGED_CRIT_MULTIPLIER);
                double specialTargetMult = ClientConfigCache.get(Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER);
                ctx.add(Component.translatable(
                                "tooltip.kogasa_addiction.koishi.ranged_crit_info",
                                KogasaNumberFormatHelper.formatPercentTrimmed(ClientConfigCache.get(Config.KOISHI_RANGED_CRIT_CHANCE), 0),
                                KogasaNumberFormatHelper.formatTrimmed(mult, 2))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
                ctx.add(Component.translatable(
                                "tooltip.kogasa_addiction.koishi.ranged_special_targets",
                                KogasaNumberFormatHelper.formatTrimmed(specialTargetMult, 2),
                                KogasaNumberFormatHelper.formatTrimmed(specialTargetMult * specialTargetMult, 2))
                        .withStyle(ChatFormatting.GRAY));
            } else {
                ctx.add(Component.translatable(
                                "tooltip.kogasa_addiction.koishi.ranged_disabled")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        } else {
            double mult = ClientConfigCache.get(Config.KOISHI_CRIT_MULTIPLIER);
            ctx.add(Component.translatable(
                            "tooltip.kogasa_addiction.koishi.crit_info",
                            KogasaNumberFormatHelper.formatPercentTrimmed(ClientConfigCache.get(Config.KOISHI_CRIT_CHANCE), 0),
                            KogasaNumberFormatHelper.formatTrimmed(mult, 2))
                    .withStyle(ChatFormatting.LIGHT_PURPLE));
        }
    }
}
