package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
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
        if (isKogasa && hasAttackSpeed(stack)) {
            double speed = KogasaEquipmentHelper.getDisplayAttackSpeed(stack);
            ChatFormatting color = ChatFormatting.DARK_GREEN;
            // 原版属性行前面有一个空格（CommonComponents.space()），这里补上，保持缩进一致
            MutableComponent speedLine = CommonComponents.space().append(
                    Component.translatable(
                            "attribute.modifier.equals.0",
                            KogasaNumberFormatHelper.formatTrimmed(speed, 1),
                            Component.translatable("attribute.name.generic.attack_speed"))
                            .withStyle(color));

            int insertAt = insertionPoint(tooltip, damageLineIdx);
            tooltip.add(insertAt, speedLine);
            speedLineIdx = insertAt;
        }

        // ========== 3. 挖掘速度（0/1/2 配置，紧跟攻速行） ==========
        int miningSpeedMode = ClientConfigCache.get(Config.MINING_SPEED_TOOLTIP_MODE);
        boolean showMiningSpeed = hasMiningSpeed(stack)
                && (miningSpeedMode == 1 || (miningSpeedMode == 2 && isKogasa));
        if (showMiningSpeed) {
            Component miningLine = Component.translatable(
                            "tooltip.kogasa_addiction.mining_speed",
                            KogasaNumberFormatHelper.formatTrimmed(getMiningSpeed(stack), 2))
                    .withStyle(ChatFormatting.DARK_GREEN);

            if (speedLineIdx >= 0) {
                tooltip.add(speedLineIdx + 1, miningLine);
            } else {
                // 非本模组装备没有自定义攻速行：插到原版属性区块之后、F3+H 高级提示之前，
                // 不能直接 add 到末尾（否则会跑到耐久度/item id/NBT 下面）。
                int header = findMainHandHeader(tooltip);
                int at = header >= 0
                        ? attributeBlockEnd(tooltip, header)
                        : advancedTooltipStart(tooltip, stack);
                tooltip.add(at, miningLine);
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
                tooltip.set(i, CommonComponents.space().append(
                        Component.translatable(
                                        "attribute.modifier.equals.0",
                                        KogasaNumberFormatHelper.formatTrimmed(damage, 1),
                                        Component.translatable("attribute.name.generic.attack_damage"))
                                .withStyle(tooltip.get(i).getStyle())
                                .withStyle(ChatFormatting.DARK_GREEN)));
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
     * 原版属性行有两种渲染格式，不能只靠“空格前缀”判断：
     * <ul>
     *   <li>基础值（{@code attribute.modifier.equals}）：以 {@code CommonComponents.space()} 开头，深绿色；</li>
     *   <li>非基础修饰符（{@code plus}/{@code take}）：没有前导空格，蓝色/红色
     *       （JEI、创造栏等 player 为 null 的场景也会走这一支）。</li>
     * </ul>
     * 统一改为“文本里包含任意属性名”。属性行一定带属性名（攻击伤害/攻击速度/护甲……），
     * 附魔行与 lore 不会，因此不会误吞其它行。
     */
    private static boolean isAttributeLine(Component line) {
        String text = line.getString();
        if (text.isBlank()) {
            return false;
        }
        if (text.charAt(0) == ' ') {
            return true;
        }
        for (String name : attributeNames()) {
            if (!name.isEmpty() && text.contains(name)) {
                return true;
            }
        }
        return false;
    }

    private static List<String> attributeNames;

    private static List<String> attributeNames() {
        if (attributeNames == null) {
            List<String> names = new ArrayList<>();
            for (Attribute attribute : ForgeRegistries.ATTRIBUTES) {
                names.add(Component.translatable(attribute.getDescriptionId()).getString());
            }
            attributeNames = names;
        }
        return attributeNames;
    }

    private static String translated(String key) {
        return Component.translatable(key).getString();
    }

    private static boolean hasAttackSpeed(ItemStack stack) {
        Item item = stack.getItem();
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof HoeItem
                || item instanceof TridentItem
                // 普通 Item 的通用装备（如灾变武器）从它自己的属性表读基础攻速
                || KogasaEquipmentHelper.getAttributeAttackSpeedBase(stack) != 0.0D;
    }

    /**
     * 是否值得显示“挖掘速度”：原版工具，或任何在常见方块上挖掘速度 >1 的物品。
     * 用于覆盖不继承 DiggerItem 的模组工具。
     */
    private static boolean hasMiningSpeed(ItemStack stack) {
        return stack.getItem() instanceof DiggerItem || getMiningSpeed(stack) > 1.0F;
    }

    private static float getMiningSpeed(ItemStack stack) {
        if (stack.getItem() instanceof DiggerItem digger) {
            float speed = digger.getTier().getSpeed();
            if (KogasaEquipmentHelper.isKogasaEquipment(stack)) {
                speed = KogasaEquipmentHelper.getDestroySpeed(stack, speed);
            }
            return speed;
        }

        // 非 DiggerItem：取常见方块里的最高挖掘速度
        float max = 1.0F;
        for (var state : new net.minecraft.world.level.block.state.BlockState[]{
                net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.DIRT.defaultBlockState(),
                net.minecraft.world.level.block.Blocks.OAK_LOG.defaultBlockState()}) {
            max = Math.max(max, stack.getDestroySpeed(state));
        }
        return max;
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

        List<Component> lines = new ArrayList<>();
        // 远程属性前留一个空行，和上方区块分隔
        lines.add(Component.empty());
        lines.add(Component.translatable("tooltip.kogasa_addiction.ranged.when_in_hand")
                .withStyle(ChatFormatting.GRAY));

        if (ticks > 0) {
            lines.add(Component.translatable(
                            "tooltip.kogasa_addiction.ranged.charge_time",
                            KogasaNumberFormatHelper.formatTrimmed(ticks / 20.0D, 2))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }

        if (velocity > 0.0D) {
            lines.add(Component.translatable(
                            "tooltip.kogasa_addiction.ranged.projectile_velocity",
                            KogasaNumberFormatHelper.formatTrimmed(velocity, 2))
                    .withStyle(ChatFormatting.DARK_GREEN));
        }

        // 和武器/工具保持一致的顺序：放在原版属性区块（“在主手时：…”）之前，
        // 而不是追加到 tooltip 末尾（那样会跑到原版属性/F3+H 信息下面）。
        // 目标顺序：物品名 → 附魔 → 近战属性（如有）→ 远程属性 → 高级提示
        int header = findMainHandHeader(tooltip);
        int insertAt;
        if (header >= 0) {
            // 有近战属性区块：插在区块末尾（近战属性之后、高级提示之前）
            insertAt = attributeBlockEnd(tooltip, header);
        } else {
            // 弓弩没有近战属性：插在高级提示（F3+H 的 item id 行）之前
            insertAt = advancedTooltipStart(tooltip, stack);
        }
        tooltip.addAll(insertAt, lines);
    }

    /** 原版 F3+H 高级提示区块的起点（item id 行）；找不到就返回末尾 */
    private static int advancedTooltipStart(List<Component> tooltip, ItemStack stack) {
        net.minecraft.resources.ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) {
            return tooltip.size();
        }
        String idText = id.toString();
        for (int i = tooltip.size() - 1; i >= 0; i--) {
            if (tooltip.get(i).getString().equals(idText)) {
                return i;
            }
        }
        return tooltip.size();
    }
}
