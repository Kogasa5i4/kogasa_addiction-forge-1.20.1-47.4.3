package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import dev.xkmc.youkaishomecoming.content.entity.youkai.YoukaiEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.xkmc.youkaishomecoming.content.entity.boss.BossYoukaiEntity")
public abstract class YhBossTweakMixin extends YoukaiEntity {

    @Unique
    private static final TagKey<DamageType> YH_DANMAKU =
            TagKey.create(Registries.DAMAGE_TYPE,
                    ResourceLocation.fromNamespaceAndPath("youkaishomecoming", "danmaku"));
    @Unique
    private static final ThreadLocal<Boolean> SAFE_SET_HEALTH = ThreadLocal.withInitial(() -> false);

    @Shadow(remap = false)
    protected abstract int damageLimit();

    @Shadow(remap = false)
    protected abstract int nonDanmakuReduction();

    protected YhBossTweakMixin(EntityType<? extends YoukaiEntity> type, Level level) {
        super(type, level);
    }

    @Inject(method = "getCD", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$removeYhDamageImmunity(DamageSource source, CallbackInfoReturnable<Integer> cir) {
        if (BossTweaks.preventYhDamageImmunity()) {
            cir.setReturnValue(0);
        }
    }

    @Inject(method = "clampDamage", at = @At("RETURN"), cancellable = true, require = 0, remap = false)
    private void kogasa$relaxYhDamageCap(DamageSource source, float amount,
                                         CallbackInfoReturnable<Float> cir) {
        float result = cir.getReturnValueF();
        if (!Float.isFinite(result) || result <= 0.0F || !Float.isFinite(amount)) return;

        float maxHealth = this.getMaxHealth();
        int limit = damageLimit();
        int reduction = nonDanmakuReduction();
        if (limit <= 0 || reduction <= 0) return;

        boolean danmaku = source.is(YH_DANMAKU);
        float originalCap = maxHealth / limit;
        if (!danmaku) originalCap /= reduction;

        double percent = danmaku
                ? BossTweaks.yhDanmakuDamageCap()
                : BossTweaks.yhNormalDamageCap();
        float targetCap = (float) (maxHealth * Math.max(0.0D, Math.min(1.0D, percent)));

        if (originalCap > 0.0F && originalCap < targetCap
                && Math.abs(result - originalCap) <= Math.max(0.001F, originalCap * 0.001F)) {
            cir.setReturnValue(Math.min(amount, targetCap));
        }
    }

    @Inject(method = {"setHealth", "m_21153_"}, at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$safeSetHealth(float val, CallbackInfo ci) {
        if (!BossTweaks.preventYhSetHealthState() || !Float.isFinite(val)) return;
        SAFE_SET_HEALTH.set(true);
        try {
            float clamped = Math.max(0.0F, Math.min(this.getMaxHealth(), val));
            super.setHealth(clamped);
        } finally {
            SAFE_SET_HEALTH.set(false);
        }
        ci.cancel();
    }

    @Inject(method = "setCombatProgress", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$safeCombatProgress(float amount, CallbackInfo ci) {
        if (!BossTweaks.preventYhSetHealthState()) return;
        super.setCombatProgress(amount);
        ci.cancel();
    }

    @Inject(method = "notifyIllegalDamage", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$suppressIllegalState(float amount, Entity causer, CallbackInfo ci) {
        if (SAFE_SET_HEALTH.get() || BossTweaks.preventYhSetHealthState()) {
            ci.cancel();
        }
    }
}
