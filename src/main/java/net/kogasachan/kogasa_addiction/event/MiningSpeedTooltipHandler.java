package net.kogasachan.kogasa_addiction.event;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class MiningSpeedTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        if (!Config.SHOW_MINING_SPEED.get()) return;

        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        if (!(item instanceof DiggerItem digger)) return;

        List<Component> tooltip = event.getToolTip();

        float speed = digger.getTier().getSpeed();
        if (item instanceof IKogasaEquipment) {
            speed *= (float) KogasaEquipmentData.getMultiplier(stack);
        }
        int efficiency = EnchantmentHelper.getItemEnchantmentLevel(
                Enchantments.BLOCK_EFFICIENCY, stack);
        if (efficiency > 0) {
            speed += efficiency * efficiency + 1;
        }

        int insertIndex = getInsertIndex(tooltip);

        Component miningLine = Component.translatable(
                        "tooltip.kogasa_addiction.mining_speed",
                        String.format("%.2f", speed))
                .withStyle(ChatFormatting.DARK_GREEN);

        if (insertIndex >= 0) {
            tooltip.add(insertIndex + 1, miningLine);
        } else {
            tooltip.add(miningLine);
        }
    }

    private static int getInsertIndex(List<Component> tooltip) {
        int damageIndex = -1, speedIndex = -1;
        String damageKey = Component.translatable("attribute.name.generic.attack_damage").getString();
        String speedKey = Component.translatable("attribute.name.generic.attack_speed").getString();

        for (int i = 0; i < tooltip.size(); i++) {
            String text = tooltip.get(i).getString();
            if (text.contains(damageKey)) damageIndex = i;
            if (text.contains(speedKey)) speedIndex = i;
        }
        return Math.max(damageIndex, speedIndex);
    }
}