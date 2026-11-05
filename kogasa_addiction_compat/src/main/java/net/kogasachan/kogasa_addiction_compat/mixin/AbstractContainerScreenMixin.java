package net.kogasachan.kogasa_addiction_compat.mixin;

import net.kogasachan.kogasa_addiction_compat.event.CompatCrystalClickHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在槽位点击真正落地之前拦截（比 mouseClicked 更可靠）：
 * 原版 {@code mouseClicked} 会先调用若干逻辑再走 {@code slotClicked}，
 * 而 {@code slotClicked} 才是会修改菜单并发点击包的地方。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "slotClicked", at = @At("HEAD"), cancellable = true)
    private void kogasa$combineCrystalSlotClick(Slot slot, int slotId, int button, ClickType type,
                                                CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (CompatCrystalClickHandler.tryCombineSlot(self, slot, button, type)) {
            ci.cancel();
        }
    }
}
