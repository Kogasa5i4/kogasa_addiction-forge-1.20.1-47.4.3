package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.tooltip.KogasaAbilityTooltipContext;
import net.kogasachan.kogasa_addiction.ability.tooltip.KogasaAbilityTooltips;
import net.kogasachan.kogasa_addiction.util.KogasaDisplayHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * 能力信息区块（按住 Alt）：能力列表与槽位提示、各能力专属描述（分发到
 * ability/tooltip 注册层）、弹药信息、远程武器属性（穿透/多发/不消耗）。
 */
public final class KogasaTooltipAlt {

    private KogasaTooltipAlt() {
    }

    public static void apply(ItemStack stack, List<Component> tooltip, double growth) {
        if (Screen.hasAltDown()) {
            List<KogasaAbility> abilities = KogasaAbilityData.getAbilities(stack);
            int maxSlots = KogasaAbilityData.getMaxAbilitySlots(stack);
            int usedSlots = abilities.size();

            // 一次性解析出集合，后面 6 个能力分支都复用它，
            // 避免每帧对同一件装备重复解析 NBT 能力列表
            Set<KogasaAbility> abilitySet = abilities.isEmpty()
                    ? EnumSet.noneOf(KogasaAbility.class)
                    : EnumSet.copyOf(abilities);

            // 标题
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.abilities", usedSlots, maxSlots)
                    .withStyle(ChatFormatting.AQUA));

            // 能力列表 / 空提示
            boolean sealed = KogasaEquipmentData.isLocked(stack);
            if (abilities.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.abilities.none")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (KogasaAbility ability : abilities) {
                    MutableComponent line;
                    if (sealed) {
                        // 封印能力：✘ 标记 + 删除线 + 暗灰，醒目区分"已注入但未生效"
                        line = Component.literal("  ✘ ")
                                .append(ability.getDisplayName().copy()
                                        .withStyle(Style.EMPTY.withStrikethrough(true)))
                                .withStyle(ChatFormatting.DARK_GRAY);
                        line.append(Component.translatable(
                                        "tooltip.kogasa_addiction.abilities.sealed")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    } else {
                        line = Component.literal("  ✦ ")
                                .append(ability.getDisplayName())
                                .withStyle(ChatFormatting.LIGHT_PURPLE);
                    }
                    tooltip.add(line);
                }
            }
            // 封印状态：显示解封进度
            if (sealed) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.abilities.sealed_progress",
                                KogasaNumberFormatHelper.formatPercentTrimmed(growth, 0),
                                KogasaNumberFormatHelper.formatPercentTrimmed(
                                        Config.LOCKED_EQUIPMENT_UNLOCK_GROWTH.get(), 0))
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            // 只要还有空余能力槽就提示注入方式（无论当前是否已经注入过能力）
            if (usedSlots < maxSlots) {
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.abilities.how_to")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
            // 槽位已用满且下一个槽仍在成长可达范围内 → 显示解锁提示
            if (usedSlots >= maxSlots) {
                double nextSlotGrowth = KogasaAbilityData.getGrowthForNextSlot(maxSlots);
                if (nextSlotGrowth <= Config.MAX_GROWTH.get()) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.abilities.full_hint",
                                    KogasaNumberFormatHelper.formatPercentTrimmed(nextSlotGrowth, 0))
                            .withStyle(ChatFormatting.DARK_GRAY));
                }
            }

            // ===== 各能力专属描述：由 ability/tooltip 包中的注册渲染器生成 =====
            // 新增能力只需实现 KogasaAbilityTooltip 并在 KogasaAbilityTooltips 注册，
            // 不再需要在这里加 if 分支。遍历枚举固定了显示顺序（与原实现一致）。
            // 封印期间能力整体不生效：专属描述（含咲夜"时停可用"等状态行）一并隐藏，
            // 只保留上方的封印进度提示。
            if (!sealed) {
                for (KogasaAbility ability : KogasaAbility.values()) {
                    if (abilitySet.contains(ability)) {
                        KogasaAbilityTooltips.append(ability,
                                new KogasaAbilityTooltipContext(stack, growth, tooltip));
                    }
                }
            }

            // ===== 弹药 =====
            if (KogasaAbilityData.hasAmmo(stack)) {
                var ammoType = KogasaAbilityData.getAmmoType(stack);
                if (ammoType == KogasaAbilityData.AmmoType.POTION) {
                    ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(stack);
                    if (potionId != null) {
                        KogasaAbilityData.PotionKind kind = KogasaAbilityData.getAmmoPotionKind(stack);
                        Component potionName = KogasaDisplayHelper.getPotionDisplayName(potionId, kind);
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ammo.potion", potionName)
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    var potion = ForgeRegistries.POTIONS.getValue(potionId);
                    if (potion != null) {
                        for (var effect : potion.getEffects()) {
                            tooltip.add(KogasaDisplayHelper.buildEffectLine(effect));
                        }
                    }
                } else if (ammoType == KogasaAbilityData.AmmoType.FIREWORK) {
                    tooltip.add(Component.translatable("tooltip.kogasa_addiction.ammo.firework")
                            .withStyle(ChatFormatting.GOLD));

                    // 解析烟花 NBT 并显示详情
                    ItemStack fireworkStack = KogasaAbilityData.getFireworkStack(stack);
                    if (!fireworkStack.isEmpty()) {
                        tooltip.addAll(KogasaDisplayHelper.getFireworkInfo(fireworkStack));
                    }
                }
            }

            // ===== 远程武器专属：穿透、多发、不消耗 =====
            if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
                int pierceLevel = KogasaEquipmentHelper.getBowPierceLevel(stack);
                int arrowCount = KogasaEquipmentHelper.getBowArrowCount(stack);

                if (pierceLevel > 0) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.ranged.pierce", pierceLevel)
                            .withStyle(ChatFormatting.AQUA));
                }
                if (arrowCount > 1) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.ranged.multishot", arrowCount)
                            .withStyle(ChatFormatting.AQUA));
                }

                // ===== 概率不消耗箭矢 =====
                double noConsumeChance = KogasaEquipmentHelper.getNoConsumeArrowChance(stack);
                if (noConsumeChance > 0.0) {
                    if (noConsumeChance >= 1.0) {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ranged.no_consume.max")
                                .withStyle(ChatFormatting.GOLD));
                    } else {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ranged.no_consume",
                                        KogasaNumberFormatHelper.formatPercentTrimmed(noConsumeChance, 0))
                                .withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    // 未解锁：显示还差多少成长值
                    double start = Config.NO_CONSUME_ARROW_START_GROWTH.get();
                    if (start > 0) {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ranged.no_consume.locked",
                                        KogasaNumberFormatHelper.formatPercentTrimmed(growth, 0),
                                        KogasaNumberFormatHelper.formatPercentTrimmed(start, 0))
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.alt")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
