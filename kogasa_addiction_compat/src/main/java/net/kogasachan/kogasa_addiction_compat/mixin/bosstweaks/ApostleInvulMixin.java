package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.entity.player.Abilities;
import org.objectweb.asm.Opcodes;

@Pseudo
@Mixin(targets = "com.Polarice3.Goety.common.entities.boss.Apostle", remap = false)
public abstract class ApostleInvulMixin {

    @Shadow
    public int moddedInvul;

    @Shadow
    public int obsidianInvul;

    @Redirect(method = "aiStep",
            at = @At(value = "FIELD",
                    target = "Lnet/minecraft/world/entity/player/Abilities;flying:Z",
                    opcode = Opcodes.PUTFIELD),
            require = 0, remap = false)
    private void kogasa$keepPlayerFlying(Abilities abilities, boolean value) {
        if (BossTweaks.preventApollyonFlightBan() && abilities.flying) {
            return;
        }
        abilities.flying = value;
    }

    @Inject(method = "isSettingUpSecond", at = @At("HEAD"), cancellable = true, require = 0, remap = false)
    private void kogasa$skipApostleSetupInvul(CallbackInfoReturnable<Boolean> cir) {
        if (BossTweaks.preventApollyonInvulnerability() && !BossTweaks.isApollyonDoom(this)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("HEAD"), require = 0, remap = false)
    private void kogasa$clearApostleInvul(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (!BossTweaks.preventApollyonInvulnerability()) return;
        if (BossTweaks.isApollyonDoom(this)) return; // 尾杀阶段保留无敌
        moddedInvul = 0;
        obsidianInvul = 0;
    }
}
