package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
        if (!output.isEmpty() && net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(output)) {
            KogasaAdvancementHelper.checkAndGrant(player, output);
        }
        // Shift 快捷取出：直接扫全背包
        else if (output.isEmpty()) {
            KogasaAdvancementHelper.scanInventoryAndGrant(player);
        }

        // 桶清除：普通/Shift 取出都会触发本事件，且 left/right 仍在（Shift 时 output 为空）
        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();
        if ((right.is(Items.WATER_BUCKET) || right.is(Items.MILK_BUCKET))
                && KogasaEquipmentHelper.isKogasaEquipment(left)) {
            giveBucket(player);
        }
    }

    /** 给玩家一个空铁桶（背包满则掉在脚下）。 */
    public static void giveBucket(ServerPlayer player) {
        ItemStack bucket = new ItemStack(Items.BUCKET);
        if (!player.getInventory().add(bucket)) {
            ItemEntity drop = new ItemEntity(player.level(),
                    player.getX(), player.getY() + 0.5, player.getZ(), bucket);
            drop.setPickUpDelay(0);
            player.level().addFreshEntity(drop);
        }
    }
}
