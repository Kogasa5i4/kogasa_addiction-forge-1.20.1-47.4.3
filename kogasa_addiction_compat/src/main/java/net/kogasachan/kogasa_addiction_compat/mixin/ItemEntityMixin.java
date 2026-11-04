package net.kogasachan.kogasa_addiction_compat.mixin;

import net.kogasachan.kogasa_addiction_compat.compat.RainbowLavaHandler;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 彩虹水晶与熔岩的交互：把检测挂在 {@link ItemEntity#tick()} 的 HEAD。
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void kogasa$checkLavaConversion(CallbackInfo ci) {
        RainbowLavaHandler.tick((ItemEntity) (Object) this);
    }
}
