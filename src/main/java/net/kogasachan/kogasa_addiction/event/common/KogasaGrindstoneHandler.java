package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.GrindstoneEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaGrindstoneHandler {

    @SubscribeEvent
    public static void onGrindstonePlace(GrindstoneEvent.OnPlaceItem event) {
        ItemStack newItem = event.getOutput();
        if (newItem.isEmpty()) return;
        if (!(newItem.getItem() instanceof IKogasaEquipment)) return;

        ItemStack modified = newItem.copy();

        // 砂轮已剥离所有附魔，成长附魔累计数清零，允许重新累积
        if (KogasaEquipmentData.getGrowthEnchantsGranted(newItem) > 0) {
            KogasaEquipmentData.setGrowthEnchantsGranted(modified, 0);
        }

        // 失败提示标记一并重置，下次再次卡住时重新提示
        if (KogasaEquipmentData.hasGrowthEnchantFailNotified(newItem)) {
            KogasaEquipmentData.setGrowthEnchantFailNotified(modified, false);
        }

        event.setOutput(modified);
    }

    @SubscribeEvent
    public static void onGrindstoneTake(GrindstoneEvent.OnTakeItem event) {
        ItemStack newItem = event.getNewTopItem();
        if (newItem.isEmpty()) return;
        if (!(newItem.getItem() instanceof IKogasaEquipment)) return;

        ItemStack modified = newItem.copy();

        // 砂轮已剥离所有附魔，成长附魔累计数清零，允许重新累积
        if (KogasaEquipmentData.getGrowthEnchantsGranted(newItem) > 0) {
            KogasaEquipmentData.setGrowthEnchantsGranted(modified, 0);
        }

        // 失败提示标记一并重置，下次再次卡住时重新提示
        if (KogasaEquipmentData.hasGrowthEnchantFailNotified(newItem)) {
            KogasaEquipmentData.setGrowthEnchantFailNotified(modified, false);
        }

        event.setNewTopItem(modified);
    }
}