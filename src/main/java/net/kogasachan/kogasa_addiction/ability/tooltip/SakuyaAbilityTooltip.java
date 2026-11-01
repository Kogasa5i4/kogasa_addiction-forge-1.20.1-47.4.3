package net.kogasachan.kogasa_addiction.ability.tooltip;

import net.kogasachan.kogasa_addiction.client.ClientTimestopState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

/**
 * SAKUYA（完美而潇洒的从者）的 tooltip：时停可用状态。
 */
public final class SakuyaAbilityTooltip implements KogasaAbilityTooltip {

    @Override
    public void append(KogasaAbilityTooltipContext ctx) {
        if (!ClientTimestopState.isFrozen()) {
            ctx.add(Component.translatable("tooltip.kogasa_addiction.timestop.available")
                    .withStyle(ChatFormatting.BLUE));
        } else {
            ctx.add(Component.translatable("tooltip.kogasa_addiction.timestop.unavailable")
                    .withStyle(ChatFormatting.RED));
        }
    }
}
