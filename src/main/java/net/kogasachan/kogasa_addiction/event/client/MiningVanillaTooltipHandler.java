package net.kogasachan.kogasa_addiction.event.client;

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
                || item instanceof HoeItem
                || item instanceof TridentItem;

        // 挖掘工具：镐/斧/铲/锄
        boolean isDigger = item instanceof DiggerItem;

        // ========== 3. 插入自定义攻速行 ==========
        int speedInsertIdx = -1;
        if (hasAttackSpeed) {
            double speed;
            boolean capped = false;
            if (isKogasa) {
                speed = KogasaEquipmentHelper.getDisplayAttackSpeed(stack);
                capped = KogasaEquipmentHelper.isAttackSpeedCapped(stack);
            } else {
                double baseSpeed = KogasaEquipmentHelper.getBaseAttackSpeed(stack);
                speed = 4.0 + baseSpeed;
            }

            MutableComponent speedLine = Component.translatable(
                    "tooltip.kogasa_addiction.vanilla_attack_speed",
                    String.format("%.1f", speed));

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

        // ========== 4. 插入挖掘速度（紧跟在攻速行后面） ==========
        if (isDigger && Config.SHOW_MINING_SPEED.get()) {
            float miningSpeed = getMiningSpeed(stack);

            Component miningLine = Component.translatable(
                            "tooltip.kogasa_addiction.mining_speed",
                            String.format("%.2f", miningSpeed))
                    .withStyle(ChatFormatting.DARK_GREEN);

            if (speedInsertIdx >= 0) {
                tooltip.add(speedInsertIdx + 1, miningLine);
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
     * 获取物品的挖掘速度（含 Kogasa 倍率和效率附魔）
     */
    private static float getMiningSpeed(ItemStack stack) {
        if (!(stack.getItem() instanceof DiggerItem digger)) return 1.0F;

        float speed = digger.getTier().getSpeed();

        if (stack.getItem() instanceof IKogasaEquipment) {
            speed = KogasaEquipmentHelper.getDestroySpeed(stack, speed);
        }

        return speed;
    }
}