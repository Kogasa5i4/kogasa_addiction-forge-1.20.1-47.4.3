package net.kogasachan.kogasa_addiction.ability.tooltip;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.event.client.ClientConfigCache;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;

/**
 * KOISHI（紧闭的恋之瞳）的 Alt 提示：只保留“远程能力已关闭”这一状态指示；
 * 具体暴击率/倍率/特攻说明已移到 JEI 能力锭分类。
 */
public final class KoishiAbilityTooltip implements KogasaAbilityTooltip {

    @Override
    public void append(KogasaAbilityTooltipContext ctx) {
        boolean isRangedWeapon = ctx.stack().getItem() instanceof BowItem
                || ctx.stack().getItem() instanceof CrossbowItem;

        if (isRangedWeapon && !ClientConfigCache.get(Config.ENABLE_KOISHI_RANGED)) {
            ctx.add(Component.translatable("tooltip.kogasa_addiction.koishi.ranged_disabled")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
