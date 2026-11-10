package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.mega.uom.common.entity.boss.uom.BaseEntity")
public abstract class UomBaseInvulMixin {

    @Shadow
    public abstract void setInvulnerableTicks(int p_31511_);

    @Inject(method = "hurt", at = @At("HEAD"), require = 0)
    private void kogasa$clearUomSpawnInvul(DamageSource damageSource, float damage, CallbackInfoReturnable<Boolean> cir) {
        if (BossTweaks.preventUomInvulnerability()) {
            setInvulnerableTicks(0);
        }
    }
}
