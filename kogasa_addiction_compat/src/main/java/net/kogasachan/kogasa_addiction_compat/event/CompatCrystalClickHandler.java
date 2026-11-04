package net.kogasachan.kogasa_addiction_compat.event;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.kogasachan.kogasa_addiction_compat.network.CombineCrystalPacket;
import net.kogasachan.kogasa_addiction_compat.network.CompatNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

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
     * @return true 表示这次右键已被结合逻辑消费，调用方应直接返回，不再走原版槽位点击。
     */
    public static boolean tryCombine(AbstractContainerScreen<?> screen, int button) {
        if (button != 1) {
            return false;
        }

        ItemStack carried = screen.getMenu().getCarried();
        if (carried.isEmpty() || !carried.is(CompatRegistry.RAINBOW_CRYSTAL.get())) {
            return false;
        }

        Slot slot = screen.hoveredSlot;
        if (slot == null || !slot.hasItem()) {
            return false;
        }

        ItemStack target = slot.getItem();
        if (target.getItem() instanceof IKogasaEquipment || KogasaCompat.hasMarker(target)) {
            return false;
        }

        if (screen instanceof CreativeModeInventoryScreen) {
            // 创造模式物品列表里的槽位不属于玩家背包，不处理
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player == null || slot.container != minecraft.player.getInventory()) {
                return false;
            }
            CompatNetwork.INSTANCE.sendToServer(new CombineCrystalPacket(slot.getContainerSlot(), true));
        } else {
            CompatNetwork.INSTANCE.sendToServer(new CombineCrystalPacket(slot.index, false));
        }
        return true;
    }
}
