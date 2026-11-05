package net.kogasachan.kogasa_addiction_compat.mixin;

import net.kogasachan.kogasa_addiction_compat.event.CompatCrystalClickHandler;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 创造模式物品栏重写了 {@code slotClicked}（创造模式物品列表的槽位不走普通菜单点击），
 * 单独拦截它，保证创造模式里结合水晶也不和背包物品换位。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kogasa$combineCrystalSlotClick(Slot slot, int slotId, int button, ClickType type,
                                                CallbackInfo ci) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (CompatCrystalClickHandler.tryCombineSlot(self, slot, button, type)) {
            ci.cancel();
        }
    }
}
