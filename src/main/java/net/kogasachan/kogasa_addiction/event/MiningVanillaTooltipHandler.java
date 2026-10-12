package net.kogasachan.kogasa_addiction.event;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MiningVanillaTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        List<Component> tooltip = event.getToolTip();

        // ========== 1. 删除原版攻速行（对所有物品） ==========
        String speedAttrKey = Component.translatable(
                "attribute.name.generic.attack_speed").getString();
        tooltip.removeIf(line -> line.getString().contains(speedAttrKey));

        // ========== 2. 判断物品类型 ==========
        Item item = stack.getItem();
        boolean isKogasa = item instanceof IKogasaEquipment;

        // 有攻速属性的物品：剑、镐/斧/铲（DiggerItem）、锄
        boolean hasAttackSpeed = item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof HoeItem;

        // 挖掘工具：镐/斧/铲/锄
        boolean isDigger = item instanceof DiggerItem;

        // ========== 3. 插入自定义攻速行 ==========
        if (hasAttackSpeed) {
            double speed;
            if (isKogasa) {
                speed = KogasaEquipmentHelper.getDisplayAttackSpeed(stack);
            } else {
                // 非 Kogasa 物品：原版基础攻速 + 玩家基础 4.0
                double baseSpeed = getVanillaBaseSpeed(stack);
                speed = 4.0 + baseSpeed;
            }

            MutableComponent speedLine = Component.translatable(
                    "tooltip.kogasa_addiction.vanilla_attack_speed",
                    String.format("%.1f", speed));

            // 插在攻击伤害行下方；找不到就追加到末尾
            int insertIdx = findAttackDamageLine(tooltip);
            if (insertIdx >= 0) {
                tooltip.add(insertIdx + 1, speedLine.withStyle(ChatFormatting.DARK_GREEN));
            } else {
                tooltip.add(speedLine.withStyle(ChatFormatting.DARK_GREEN));
            }
        }

        // ========== 4. 插入挖掘速度（仅挖掘工具） ==========
        if (isDigger && Config.SHOW_MINING_SPEED.get()) {
            float miningSpeed = getMiningSpeed(stack);

            Component miningLine = Component.translatable(
                            "tooltip.kogasa_addiction.mining_speed",
                            String.format("%.2f", miningSpeed))
                    .withStyle(ChatFormatting.DARK_GREEN);

            // 紧跟在攻速行后面
            int speedIdx = findLineByKey(tooltip, "tooltip.kogasa_addiction.attack_speed");
            if (speedIdx < 0) {
                // 没找到攻速行（理论上不会，因为上面刚插过）
                speedIdx = findAttackDamageLine(tooltip);
            }
            if (speedIdx >= 0) {
                tooltip.add(speedIdx + 1, miningLine);
            } else {
                tooltip.add(miningLine);
            }
        }
    }

// ========== 辅助方法 ==========

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
     * 找到包含指定翻译键内容的行的索引（用于定位刚插入的攻速行）
     */
    private static int findLineByKey(List<Component> tooltip, String translationKey) {
        // 简化：直接用当前语言下的文本匹配
        String prefix = Component.translatable(translationKey, "", "").getString()
                .replace("%s", "").replace("  ", " ").trim();
        for (int i = 0; i < tooltip.size(); i++) {
            String text = tooltip.get(i).getString();
            // 匹配关键部分（去掉占位符后剩的文本）
            String keyword = prefix.split("：")[0].split(":")[0].trim();
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 获取非 Kogasa 物品的原版基础攻速修饰符
     */
    private static double getVanillaBaseSpeed(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem) return -2.4;
        if (item instanceof HoeItem) return -3.0;
        // DiggerItem：用注册名后缀判断
        var id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return -2.8;
        String path = id.getPath();
        if (path.endsWith("_axe")) return -3.2;
        if (path.endsWith("_shovel")) return -3.0;
        return -2.8; // 镐
    }

    /**
     * 获取物品的挖掘速度（含 Kogasa 倍率和效率附魔）
     */
    private static float getMiningSpeed(ItemStack stack) {
        if (!(stack.getItem() instanceof DiggerItem digger)) return 1.0F;

        float speed = digger.getTier().getSpeed();

        if (stack.getItem() instanceof IKogasaEquipment) {
            speed = KogasaEquipmentHelper.getDestroySpeed(stack, speed);
        }

//        int efficiency = EnchantmentHelper.getItemEnchantmentLevel(
//                Enchantments.BLOCK_EFFICIENCY, stack);
//        if (efficiency > 0) {
//            speed += efficiency * efficiency + 1;
//        }
// 不要效率附魔

        return speed;
    }
}