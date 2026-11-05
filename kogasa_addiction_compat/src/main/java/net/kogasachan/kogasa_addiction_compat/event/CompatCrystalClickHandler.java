package net.kogasachan.kogasa_addiction_compat.event;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.kogasachan.kogasa_addiction_compat.network.ClearCompatPacket;
import net.kogasachan.kogasa_addiction_compat.network.CombineCrystalPacket;
import net.kogasachan.kogasa_addiction_compat.network.CompatNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * 彩虹水晶的右键结合：客户端只负责判定并发包，真正的结合在服务端完成。
 *
 * <p>注意不能依赖 {@code ScreenEvent.MouseButtonPressed.Pre} 取消：
 * {@link AbstractContainerScreen#mouseClicked} 会先调用 {@code super.mouseClicked}（事件在这里发出），
 * 然后**无论是否被取消都会继续处理槽位点击**，于是会出现"结合成功但光标物品和槽位物品换位"。
 * 因此改为 mixin 在 {@code AbstractContainerScreen.mouseClicked} 的 HEAD 直接拦截。
 */
public final class CompatCrystalClickHandler {

    private CompatCrystalClickHandler() {
    }

    /**
     * 在 {@code AbstractContainerScreen#slotClicked}（以及创造模式的重写版本）最前面调用。
     *
     * @return true 表示这次右键已被结合逻辑消费，调用方应直接返回，不再走原版槽位点击 / 发包。
     */
    public static boolean tryCombineSlot(AbstractContainerScreen<?> screen, Slot slot,
                                         int button, ClickType type) {
        if (button != 1 || type != ClickType.PICKUP) {
            return false;
        }
        if (slot == null || !slot.hasItem()) {
            return false;
        }

        ItemStack carried = screen.getMenu().getCarried();
        if (carried.is(Items.LAVA_BUCKET)) {
            return tryClearSlot(screen, slot);
        }
        if (carried.isEmpty() || !carried.is(CompatRegistry.RAINBOW_CRYSTAL.get())) {
            return false;
        }

        ItemStack target = slot.getItem();
        if (target.getItem() instanceof IKogasaEquipment || KogasaCompat.hasMarker(target)) {
            return false;
        }
        if (!KogasaCompat.canCombine(target)) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.displayClientMessage(Component.translatable(
                        "message.kogasa_addiction_compat.not_compatible"), true);
            }
            // 吃掉这次点击，避免水晶和不可兼容物品换位
            return true;
        }

        if (screen instanceof CreativeModeInventoryScreen) {
            // 创造模式物品列表里的槽位不属于玩家背包，不处理
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || slot.container != minecraft.player.getInventory()) {
                return false;
            }
            // 创造模式的快捷栏是客户端先行的：先拷贝未标记的物品发给服务端兜底，
            // 再本地打标记让玩家立刻看到效果（顺序不能反，否则服务端会认为"已经兼容"）。
            ItemStack toSend = target.copy();
            KogasaCompat.markCompatible(target);
            CompatNetwork.INSTANCE.sendToServer(new CombineCrystalPacket(
                    slot.getContainerSlot(), true, toSend));
        } else {
            CompatNetwork.INSTANCE.sendToServer(new CombineCrystalPacket(slot.index, false));
        }
        return true;
    }

    /** 熔岩桶右键：清掉非本模组物品上的本模组 NBT，恢复原始状态。 */
    private static boolean tryClearSlot(AbstractContainerScreen<?> screen, Slot slot) {
        ItemStack target = slot.getItem();
        if (target.isEmpty() || target.getItem() instanceof IKogasaEquipment) {
            return false;
        }
        if (!KogasaCompat.hasKogasaData(target)) {
            return false;
        }

        if (screen instanceof CreativeModeInventoryScreen) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || slot.container != minecraft.player.getInventory()) {
                return false;
            }
            ItemStack toSend = target.copy();
            KogasaCompat.clearAddedData(target); // 客户端先恢复显示
            CompatNetwork.INSTANCE.sendToServer(new ClearCompatPacket(
                    slot.getContainerSlot(), true, toSend));
        } else {
            CompatNetwork.INSTANCE.sendToServer(new ClearCompatPacket(slot.index, false));
        }
        return true;
    }
}
