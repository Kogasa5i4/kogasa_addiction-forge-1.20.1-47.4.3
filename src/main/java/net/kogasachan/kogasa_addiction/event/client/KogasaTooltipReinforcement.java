package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.util.KogasaDisplayHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 强化信息区块（按住 Shift）：强化次数、成长倍率、已吸收材料、吸收值。
 */
public final class KogasaTooltipReinforcement {

    private KogasaTooltipReinforcement() {
    }

    public static void apply(ItemStack stack, List<Component> tooltip,
                             int re, int maxRe, List<String> used, double absorbed) {
        if (Screen.hasShiftDown()) {
            // 强化次数
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.reinforcement", re, maxRe)
                    .withStyle(ChatFormatting.GOLD));

            // 成长倍率
            double growthMult = KogasaEquipmentHelper.getGrowthMultiplier(stack);

            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.multiplier.growth",
                            KogasaNumberFormatHelper.formatTrimmed(growthMult, 2))
                    .withStyle(ChatFormatting.YELLOW));

            // 综合倍率：跟随 Shift 区，不再放在 Alt 的能力信息里
            double totalMult = KogasaEquipmentHelper.getTotalMultiplier(stack);
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.multiplier.total",
                            KogasaNumberFormatHelper.formatTrimmed(totalMult, 3))
                    .withStyle(ChatFormatting.GOLD));

            // 已吸收装备
            if (used.isEmpty()) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.reinforcement.none")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (String id : used) {
                    Component itemName = KogasaDisplayHelper.getItemDisplayName(id);
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.reinforcement.entry", itemName)
                            .withStyle(ChatFormatting.GRAY));
                }
            }

            // 吸收值
            if (absorbed > 0) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.absorbed",
                                KogasaNumberFormatHelper.formatTrimmed(absorbed, 1))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
