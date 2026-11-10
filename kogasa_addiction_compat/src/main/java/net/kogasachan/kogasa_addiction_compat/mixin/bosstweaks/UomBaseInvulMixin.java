package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.mega.uom.common.entity.boss.uom.BaseEntity", remap = false)
public abstract class UomBaseInvulMixin {

    @Shadow
    public abstract void setInvulnerableTicks(int ticks);

    @Inject(method = "m_6469_", at = @At("HEAD"), require = 0, remap = false)
    private void kogasa$clearUomSpawnInvul(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (BossTweaks.preventUomInvulnerability()) {
            setInvulnerableTicks(0);
        }
    }

    @Inject(method = "m_6673_", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$clearUomIsInvulnerable(DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        if (BossTweaks.preventUomInvulnerability()) {
            cir.setReturnValue(false);
        }
    }
}
