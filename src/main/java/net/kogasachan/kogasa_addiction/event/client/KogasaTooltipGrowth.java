package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 成长信息区块（按住 Ctrl，仅成长解锁后显示）：成长百分比、成长附魔进度。
 */
public final class KogasaTooltipGrowth {

    private KogasaTooltipGrowth() {
    }

    public static void apply(ItemStack stack, List<Component> tooltip, int re, double growth) {
        // 仅成长解锁后显示
        if (re < ModConfigCache.growthUnlockReinforcements) {
            return;
        }

        if (Screen.hasControlDown()) {
            String percent = KogasaNumberFormatHelper.formatPercentTrimmed(growth, 2);
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.growth", percent)
                    .withStyle(ChatFormatting.GREEN));

            // 成长附魔进度
            int grantedEnchants = KogasaEquipmentData.getGrowthEnchantsGranted(stack);
            int maxEnchants = Config.MAX_GROWTH_ENCHANTS.get();
            if (Config.ENABLE_GROWTH_ENCHANT.get() && maxEnchants > 0) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.growth_enchant", grantedEnchants, maxEnchants)
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.ctrl")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
