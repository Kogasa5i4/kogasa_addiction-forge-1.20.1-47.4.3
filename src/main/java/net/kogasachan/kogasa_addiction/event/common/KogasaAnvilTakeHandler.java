package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.AnvilRepairEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaAnvilTakeHandler {

    @SubscribeEvent
    public static void onAnvilRepair(AnvilRepairEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack output = event.getOutput();

        // 普通点击取出：直接检查输出槽的物品
        if (!output.isEmpty() && output.getItem() instanceof IKogasaEquipment) {
            KogasaAdvancementHelper.checkAndGrant(player, output);
        }
        // Shift 快捷取出：直接扫全背包
        else if (output.isEmpty()) {
            KogasaAdvancementHelper.scanInventoryAndGrant(player);
        }
    }
}