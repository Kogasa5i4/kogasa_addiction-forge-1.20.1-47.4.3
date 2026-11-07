package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.util.KogasaAmmoService;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 妖归弹幕/符卡装在弩上时，让原版弩把“已装填”当成有箭，实际发射由事件替换。 */
@Mixin(Player.class)
public abstract class PlayerProjectileMixin {

    @Inject(method = "getProjectile", at = @At("RETURN"), cancellable = true)
    private void kogasa$yhAmmoAsProjectile(ItemStack weapon, CallbackInfoReturnable<ItemStack> cir) {
        if (!cir.getReturnValue().isEmpty()) return;
        if (KogasaAmmoService.hasYhAmmo(weapon)) {
            cir.setReturnValue(new ItemStack(Items.ARROW));
        }
    }
}
