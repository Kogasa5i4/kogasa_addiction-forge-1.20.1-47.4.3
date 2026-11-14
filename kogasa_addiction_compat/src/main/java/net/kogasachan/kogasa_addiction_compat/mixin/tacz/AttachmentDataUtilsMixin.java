package net.kogasachan.kogasa_addiction_compat.mixin.tacz;

import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import net.kogasachan.kogasa_addiction_compat.compat.tacz.TaczCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 把 Kogasa 吸收的弹匣容量叠加到 TaCZ 的换弹容量计算上。
 */
@Mixin(AttachmentDataUtils.class)
public abstract class AttachmentDataUtilsMixin {

    @Inject(method = "getAmmoCountWithAttachment", at = @At("RETURN"), cancellable = true, remap = false)
    private static void kogasa$ammoCount(ItemStack stack, GunData data,
                                         CallbackInfoReturnable<Integer> cir) {
        int level = AttachmentDataUtils.getMagExtendLevel(stack, data);
        double bonus = TaczCompat.ammoBonus(stack, cir.getReturnValue(), level);
        if (bonus > 0.0D) {
            cir.setReturnValue(cir.getReturnValue() + (int) Math.round(bonus));
        }
    }
}
