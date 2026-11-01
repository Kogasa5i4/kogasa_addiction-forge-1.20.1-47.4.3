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
 * <p>由原来的 KogasaTooltipHandler（攻伤/蓄力弹速部分）与 MiningVanillaTooltipHandler
 * （攻速/挖掘速度部分）合并而来。
 */
public final class KogasaTooltipStats {

    private KogasaTooltipStats() {
    }

    public static void apply(ItemStack stack, List<Component> tooltip, boolean isKogasa) {
        Item item = stack.getItem();

        // ========== 1. 删除原版攻速行（仅本模组装备） ==========
        if (isKogasa) {
            String speedAttrKey = Component.translatable(
                    "attribute.name.generic.attack_speed").getString();
            tooltip.removeIf(line -> line.getString().contains(speedAttrKey));
        }

        // ========== 2. 替换原版攻击伤害行为实际伤害（含空手 +1） ==========
        if (isKogasa) {
            String damageAttrKey = Component.translatable(
                    "attribute.name.generic.attack_damage").getString();

            for (int i = 0; i < tooltip.size(); i++) {
                Component line = tooltip.get(i);
                if (line.getString().contains(damageAttrKey)) {
                    double damage = KogasaEquipmentHelper.getDisplayAttackDamage(stack) + 1.0;
                    Component newLine = Component.translatable(
                                    "tooltip.kogasa_addiction.vanilla_attack_damage",
                                    KogasaNumberFormatHelper.formatTrimmed(damage, 1))
                            .withStyle(line.getStyle()) // 继承原样式
                            .withStyle(ChatFormatting.DARK_GREEN);
                    tooltip.set(i, newLine);
                    break;
                }
            }
        }

        // ========== 3. 插入自定义攻速行（仅本模组装备） ==========
        // 有攻速属性的物品：剑、镐/斧/铲/锄、三叉戟
        boolean hasAttackSpeed = item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof HoeItem
                || item instanceof TridentItem;

        int speedInsertIdx = -1;
        if (hasAttackSpeed && isKogasa) {
            double speed = KogasaEquipmentHelper.getDisplayAttackSpeed(stack);
            boolean capped = KogasaEquipmentHelper.isAttackSpeedCapped(stack);

            MutableComponent speedLine = Component.translatable(
                    "tooltip.kogasa_addiction.vanilla_attack_speed",
                    KogasaNumberFormatHelper.formatTrimmed(speed, 1));

            ChatFormatting color = capped ? ChatFormatting.GOLD : ChatFormatting.DARK_GREEN;

            int insertIdx = findAttackDamageLine(tooltip);
            if (insertIdx >= 0) {
                speedInsertIdx = insertIdx + 1;
                tooltip.add(speedInsertIdx, speedLine.withStyle(color));
            } else {
                speedInsertIdx = tooltip.size();
                tooltip.add(speedLine.withStyle(color));
            }
        }

        // ========== 4. 挖掘速度（0/1/2 配置，紧跟在攻速行后面） ==========
        int miningSpeedMode = Config.MINING_SPEED_TOOLTIP_MODE.get();
        boolean showMiningSpeed = item instanceof DiggerItem
                && (miningSpeedMode == 1 || (miningSpeedMode == 2 && isKogasa));
        if (showMiningSpeed) {
            float miningSpeed = getMiningSpeed(stack);

            Component miningLine = Component.translatable(
                            "tooltip.kogasa_addiction.mining_speed",
                            KogasaNumberFormatHelper.formatTrimmed(miningSpeed, 2))
                    .withStyle(ChatFormatting.DARK_GREEN);

            if (speedInsertIdx >= 0) {
                tooltip.add(speedInsertIdx + 1, miningLine);
            } else {
                tooltip.add(miningLine);
            }
        }

        // ========== 5. 蓄力时间 / 弹射物速度（0/1/2 配置） ==========
        int rangedMode = Config.RANGED_STATS_TOOLTIP_MODE.get();
        if (isRangedWeaponOrTrident(stack)
                && (rangedMode == 1 || (rangedMode == 2 && isKogasa))) {
            appendRangedStats(stack, tooltip);
        }
    }

    /**
     * 找到攻击伤害行的索引
     */
    private static int findAttackDamageLine(List<Component> tooltip) {
        String key = Component.translatable("attribute.name.generic.attack_damage").getString();
        for (int i = 0; i < tooltip.size(); i++) {
            if (tooltip.get(i).getString().contains(key)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取物品的挖掘速度（Kogasa 装备含强化/成长倍率；效率附魔由玩家属性另算）
     */
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
