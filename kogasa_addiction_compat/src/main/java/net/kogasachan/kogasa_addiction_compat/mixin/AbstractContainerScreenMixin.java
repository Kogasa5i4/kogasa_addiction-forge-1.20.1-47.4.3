package net.kogasachan.kogasa_addiction_compat.mixin;

import net.kogasachan.kogasa_addiction_compat.event.CompatCrystalClickHandler;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 在容器界面最先拦截"手持彩虹水晶右键物品"的操作，
 * 让原版槽位点击完全不发生（避免水晶和物品换位），但结合请求照常发给服务端。
 */
@Mixin(AbstractContainerScreen.class)
public abstract class AbstractContainerScreenMixin {

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void kogasa$combineCrystalRightClick(double mouseX, double mouseY, int button,
                                                 CallbackInfoReturnable<Boolean> cir) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        if (CompatCrystalClickHandler.tryCombine(self, button)) {
            cir.setReturnValue(true);
        }
    }
}
