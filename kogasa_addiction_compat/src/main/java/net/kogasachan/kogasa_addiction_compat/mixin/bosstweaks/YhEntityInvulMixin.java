package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = {
        "dev.xkmc.youkaishomecoming.content.entity.youkai.YoukaiEntity",
        "dev.xkmc.youkaishomecoming.content.entity.fairy.CirnoEntity",
        "dev.xkmc.youkaishomecoming.content.entity.rumia.RumiaEntity"
})
public abstract class YhEntityInvulMixin {

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = {"isInvulnerableTo", "m_6673_"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$clearYhEntityInvul(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (BossTweaks.preventYhEntityInvulnerability()) {
            cir.setReturnValue(false);
        }
    }
}
