package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import dev.xkmc.youkaishomecoming.content.entity.youkai.CombatProgress;
import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.content.entity.youkai.YoukaiEntity")
public abstract class YhYoukaiSetHealthMixin extends PathfinderMob {

    @Shadow(remap = false)
    public CombatProgress combatProgress;

    protected YhYoukaiSetHealthMixin(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = {"setHealth", "m_21153_"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$safeSetHealth(float amount, CallbackInfo ci) {
        if (!BossTweaks.preventYhSetHealthState() || !Float.isFinite(amount)) return;
        float clamped = Math.max(0.0F, Math.min(getMaxHealth(), amount));
        super.setHealth(clamped);
        ci.cancel();
    }

    @Inject(method = "getCombatProgress", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$syncCombatProgress(CallbackInfoReturnable<Float> cir) {
        if (!BossTweaks.preventYhSetHealthState()) return;
        float health = super.getHealth();
        if (combatProgress != null) combatProgress.progress = health;
        cir.setReturnValue(health);
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = {"die", "m_6667_"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$safeDie(DamageSource source, CallbackInfo ci) {
        if (!BossTweaks.preventYhSetHealthState()) return;
        super.die(source);
        ci.cancel();
    }

    @SuppressWarnings("UnresolvedMixinReference")
    @Inject(method = {"tickDeath", "m_6153_"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$safeTickDeath(CallbackInfo ci) {
        if (!BossTweaks.preventYhSetHealthState()) return;
        super.tickDeath();
        ci.cancel();
    }
}
