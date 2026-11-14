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
 * 创造模式背包自己重写了 slotClicked，必须在子类单独拦截。
 */
@Mixin(CreativeModeInventoryScreen.class)
public abstract class CreativeModeInventoryScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kogasa$combineCrystalCreativeSlotClick(Slot slot, int slotId, int button,
                                                        ClickType type, CallbackInfo ci) {
        CreativeModeInventoryScreen self = (CreativeModeInventoryScreen) (Object) this;
        if (CompatCrystalClickHandler.tryCombineSlot(self, slot, button, type)) {
            ci.cancel();
        }
    }
}
