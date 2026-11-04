package net.kogasachan.kogasa_addiction_compat.mixin;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 兼容物品的耐久上限：强化/成长带来的最大耐久。
 *
 * <p>{@code ItemStack#getMaxDamage()} 是最终值来源，Forge 没有对应事件，只能 mixin。
 * 只对"兼容物品"生效（默认总开关关闭 → 完全不介入）。
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin {

    @Inject(method = "getMaxDamage", at = @At("RETURN"), cancellable = true)
    private void kogasa$compatMaxDamage(CallbackInfoReturnable<Integer> cir) {
        try {
            ItemStack self = (ItemStack) (Object) this;
            if (!KogasaCompat.isCompatOnly(self)) {
                return;
            }
            int base = cir.getReturnValue();
            int modified = KogasaEquipmentHelper.getMaxDamage(self, base);
            if (modified != base) {
                cir.setReturnValue(modified);
            }
        } catch (RuntimeException ignored) {
            // 配置未加载 / 标签未绑定等早期情况：保持原版值
        }
    }
}
