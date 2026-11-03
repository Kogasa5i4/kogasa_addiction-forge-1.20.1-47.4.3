package net.kogasachan.kogasa_addiction.ability.tooltip;
import net.kogasachan.kogasa_addiction.event.client.ClientConfigCache;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * CIRNO（冰之小妖精）的 tooltip：复活消耗与可用性。
 */
public final class CirnoAbilityTooltip implements KogasaAbilityTooltip {

    @Override
    public void append(KogasaAbilityTooltipContext ctx) {
        if (!ClientConfigCache.get(Config.CIRNO_REVIVE_ENABLED)) {
            ctx.add(Component.translatable(
                            "tooltip.kogasa_addiction.cirno.revive_disabled")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        double cost = ClientConfigCache.get(Config.CIRNO_REVIVE_GROWTH_COST);
        boolean canRevive = ctx.growth() >= cost;

        MutableComponent reviveLine = Component.translatable(
                "tooltip.kogasa_addiction.cirno.revive_status",
                KogasaNumberFormatHelper.formatTrimmed(cost, 2));
        reviveLine.withStyle(canRevive ? ChatFormatting.GREEN : ChatFormatting.RED);
        ctx.add(reviveLine);
    }
}
