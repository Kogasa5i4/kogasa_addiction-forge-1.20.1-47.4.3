package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.*;

import java.util.List;

/**
 * tooltip 里的"数值行"区块：
 * <ul>
 *   <li>本模组装备：删除原版攻速行、替换攻击伤害行为实际伤害（含空手 +1）、
 *       插入自定义攻速行；</li>
 *   <li>挖掘速度行：按 {@code miningSpeedTooltipMode}（0/1/2）决定显示范围；</li>
 *   <li>蓄力时间/弹射物速度行：按 {@code rangedStatsTooltipMode}（0/1/2）决定显示范围。</li>
 * </ul>
 *
 * <p><b>定位策略</b>：原版属性区块固定是"空行 + 在主手时： + 若干以空格开头的属性行"，
 * 并且位于附魔行之后。整合包里附魔描述模组常把"攻击伤害/攻击速度"写进附魔行
 * （锋利的描述就含"攻击伤害"），所以**不能用"搜索包含关键字的第一个行"**来定位，
 * 必须在"在主手时："表头之后的属性行范围内查找，并直接记住替换后的行下标。
 */
public final class KogasaTooltipStats {

    private static final String MAIN_HAND_HEADER_KEY = "item.modifiers.mainhand";
    private static final String ATTACK_DAMAGE_KEY = "attribute.name.generic.attack_damage";
    private static final String ATTACK_SPEED_KEY = "attribute.name.generic.attack_speed";

    private KogasaTooltipStats() {
    }

    public static void apply(ItemStack stack, List<Component> tooltip, boolean isKogasa) {
        Item item = stack.getItem();

        // ========== 1. 在"在主手时："属性区块内删除原版攻速行、替换攻击伤害行 ==========
        int damageLineIdx = -1;
        if (isKogasa) {
            damageLineIdx = replaceAttributeLines(tooltip, stack);
        }

        // ========== 2. 插入自定义攻速行（紧跟在攻击伤害行之后） ==========
        int speedLineIdx = -1;
        if (isKogasa && hasAttackSpeed(item)) {
            double speed = KogasaEquipmentHelper.getDisplayAttackSpeed(stack);
            ChatFormatting color = KogasaEquipmentHelper.isAttackSpeedCapped(stack)
                    ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN;
            MutableComponent speedLine = Component.translatable(
                    "tooltip.kogasa_addiction.vanilla_attack_speed",
                    KogasaNumberFormatHelper.formatTrimmed(speed, 1)).withStyle(color);

            int insertAt = insertionPoint(tooltip, damageLineIdx);
            tooltip.add(insertAt, speedLine);
            speedLineIdx = insertAt;
        }

        // ========== 3. 挖掘速度（0/1/2 配置，紧跟攻速行） ==========
        int miningSpeedMode = ClientConfigCache.get(Config.MINING_SPEED_TOOLTIP_MODE);
        boolean showMiningSpeed = item instanceof DiggerItem
                && (miningSpeedMode == 1 || (miningSpeedMode == 2 && isKogasa));
        if (showMiningSpeed) {
            Component miningLine = Component.translatable(
                            "tooltip.kogasa_addiction.mining_speed",
                            KogasaNumberFormatHelper.formatTrimmed(getMiningSpeed(stack), 2))
                    .withStyle(ChatFormatting.DARK_GREEN);

            if (speedLineIdx >= 0) {
                tooltip.add(speedLineIdx + 1, miningLine);
            } else {
                tooltip.add(miningLine);
            }
        }

        // ========== 4. 蓄力时间 / 弹射物速度（0/1/2 配置） ==========
        int rangedMode = ClientConfigCache.get(Config.RANGED_STATS_TOOLTIP_MODE);
        if (isRangedWeaponOrTrident(stack)
                && (rangedMode == 1 || (rangedMode == 2 && isKogasa))) {
            appendRangedStats(stack, tooltip);
        }
    }

    /**
     * 只在"在主手时："属性区块内处理：
     * 删除攻速行、把攻击伤害行替换成实际伤害（含空手 +1），返回替换后的行下标（找不到返回 -1）。
     */
    private static int replaceAttributeLines(List<Component> tooltip, ItemStack stack) {
        String damageKey = translated(ATTACK_DAMAGE_KEY);
        String speedKey = translated(ATTACK_SPEED_KEY);

        int damageLineIdx = -1;

        int header = findMainHandHeader(tooltip);
        if (header < 0) {
            // 没有原版属性区块（例如弓弩）：这些物品本来也没有攻速/攻击伤害行，直接跳过
            return -1;
        }

        int end = attributeBlockEnd(tooltip, header);
        for (int i = header + 1; i < end && i < tooltip.size(); i++) {
            String text = tooltip.get(i).getString();

            if (text.contains(speedKey)) {
                tooltip.remove(i);
                i--;
                end--;
                continue;
            }

            if (damageLineIdx < 0 && text.contains(damageKey)) {
                double damage = KogasaEquipmentHelper.getDisplayAttackDamage(stack) + 1.0;
                tooltip.set(i, Component.translatable(
                                "tooltip.kogasa_addiction.vanilla_attack_damage",
                                KogasaNumberFormatHelper.formatTrimmed(damage, 1))
                        .withStyle(tooltip.get(i).getStyle())
                        .withStyle(ChatFormatting.DARK_GREEN));
                damageLineIdx = i;
            }
        }
        return damageLineIdx;
    }

    /** 攻速行的插入位置：优先紧跟攻击伤害行，其次属性区块末尾，最后才追加到 tooltip 末尾 */
    private static int insertionPoint(List<Component> tooltip, int damageLineIdx) {
        if (damageLineIdx >= 0 && damageLineIdx < tooltip.size()) {
            return damageLineIdx + 1;
        }
        int header = findMainHandHeader(tooltip);
        if (header >= 0) {
            return attributeBlockEnd(tooltip, header);
        }
        return tooltip.size();
    }

    /** 找到"在主手时："表头（精确匹配，避免命中附魔描述） */
    private static int findMainHandHeader(List<Component> tooltip) {
        String header = translated(MAIN_HAND_HEADER_KEY);
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().trim().equals(header)) {
                return i;
            }
        }
        return -1;
    }

    /** 属性区块结束下标：从表头往后，连续的"以空格开头的属性行" */
    private static int attributeBlockEnd(List<Component> tooltip, int header) {
        int end = header + 1;
        while (end < tooltip.size() && isAttributeLine(tooltip.get(end))) {
            end++;
        }
        return end;
    }

    /**
     * 原版属性行由 {@code CommonComponents.space()} 开头（" +7 攻击伤害"），
     * 附魔行没有这个前导空格，用它来区分最稳。
     */
    private static boolean isAttributeLine(Component line) {
        String text = line.getString();
        return !text.isEmpty() && text.charAt(0) == ' ' && !text.isBlank();
    }

    private static String translated(String key) {
        return Component.translatable(key).getString();
    }

    private static boolean hasAttackSpeed(Item item) {
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof HoeItem
                || item instanceof TridentItem;
    }

    private static float getMiningSpeed(ItemStack stack) {
        if (!(stack.getItem() instanceof DiggerItem digger)) return 1.0F;

        float speed = digger.getTier().getSpeed();
        if (stack.getItem() instanceof IKogasaEquipment) {
            speed = KogasaEquipmentHelper.getDestroySpeed(stack, speed);
        }
        return speed;
    }

    /** 弓/弩/三叉戟（用于蓄力/弹速 tooltip 的范围判断） */
    private static boolean isRangedWeaponOrTrident(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof TridentItem;
    }

    /**
     * 原版风格的两行：浅灰"在主手或副手时："+ 绿色" 数值 文本"。
     */
    private static void appendRangedStats(ItemStack stack, List<Component> tooltip) {
        int ticks = KogasaEquipmentHelper.getEffectiveChargeTicks(stack);
        double velocity = KogasaEquipmentHelper.getDisplayProjectileVelocity(stack);
        if (ticks <= 0 && velocity <= 0.0D) return;

        tooltip.add(Component.translatable("tooltip.kogasa_addiction.ranged.when_in_hand")
                .withStyle(ChatFormatting.GRAY));

        if (ticks > 0) {
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.ranged.charge_time",
                            KogasaNumberFormatHelper.formatTrimmed(ticks / 20.0D, 2))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }

        if (velocity > 0.0D) {
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.ranged.projectile_velocity",
                            KogasaNumberFormatHelper.formatTrimmed(velocity, 2))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }
    }
}
