package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 关闭亚波伦 Doom 的三段“尾杀惩罚”。
 *
 * <p>目标是 RevelationFix 的 PlayerTickrateExecutor；不在编译期依赖该类，
 * 由 BossTweaksMixinPlugin 在类存在时才应用。
 */
@Pseudo
@Mixin(targets = "com.mega.revelationfix.common.apollyon.common.PlayerTickrateExecutor", remap = false)
public abstract class ApollyonDoomMixin {

    @Inject(method = "processInventory", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void kogasa$preventApollyonItemTheft(CallbackInfo ci) {
        if (BossTweaks.preventApollyonItemTheft()) {
            ci.cancel();
        }
    }

    @Inject(method = "updateBaseAttributeMode", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void kogasa$preventApollyonAttributeReset(CallbackInfo ci) {
        if (BossTweaks.preventApollyonAttributeReset()) {
            ci.cancel();
        }
    }

    @Inject(method = "refreshCooldown", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private static void kogasa$preventApollyonCooldown(CallbackInfo ci) {
        if (BossTweaks.preventApollyonCooldown()) {
            ci.cancel();
        }
    }
}
