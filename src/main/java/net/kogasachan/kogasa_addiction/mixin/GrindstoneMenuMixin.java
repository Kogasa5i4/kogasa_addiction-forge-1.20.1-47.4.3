package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 砂轮生成成品时重置成长附魔计数。
 * 原 GrindstoneEvent 的 OnPlaceItem#getOutput 在事件触发时默认为空，
 * 不能用来修改原版计算后的成品；这里直接改 removeNonCurses 的返回值。
 */
@Mixin(GrindstoneMenu.class)
public abstract class GrindstoneMenuMixin {

    @Inject(method = "removeNonCurses", at = @At("RETURN"), cancellable = true)
    private void kogasa$resetGrowthEnchantState(ItemStack stack, int damage, int count,
                                                CallbackInfoReturnable<ItemStack> cir) {
        ItemStack result = cir.getReturnValue();
        if (result.isEmpty() || !(result.getItem() instanceof IKogasaEquipment)) return;
        if (KogasaEquipmentData.getGrowthEnchantsGranted(result) <= 0
                && !KogasaEquipmentData.hasGrowthEnchantFailNotified(result)) {
            return;
        }

        ItemStack modified = result.copy();
        KogasaEquipmentData.setGrowthEnchantsGranted(modified, 0);
        KogasaEquipmentData.setGrowthEnchantFailNotified(modified, false);
        cir.setReturnValue(modified);
    }
}
