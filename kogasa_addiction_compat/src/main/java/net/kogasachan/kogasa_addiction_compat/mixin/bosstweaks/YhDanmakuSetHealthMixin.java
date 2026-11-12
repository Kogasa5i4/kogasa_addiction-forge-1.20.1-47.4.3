package net.kogasachan.kogasa_addiction_compat.mixin.bosstweaks;

import net.kogasachan.kogasa_addiction_compat.bosstweaks.BossTweaks;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = {
        "dev.xkmc.youkaishomecoming.content.entity.reimu.ReimuEntity",
        "dev.xkmc.youkaishomecoming.content.entity.boss.YukariEntity"
})
public abstract class YhDanmakuSetHealthMixin {

    @Redirect(method = "onDanmakuImmune",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V",
                      remap = true),
            require = 0, remap = false)
    private void kogasa$replaceDanmakuSetHealth(LivingEntity target, float newHealth) {
        if (!BossTweaks.replaceYhDanmakuSetHealth()) {
            target.setHealth(newHealth);
            return;
        }
        if (!(target instanceof Player)) {
            target.setHealth(newHealth);
            return;
        }
        float damage = target.getHealth() - newHealth;
        if (damage <= 0.0F) {
            target.setHealth(newHealth);
            return;
        }
        target.hurt(target.level().damageSources().genericKill(), damage);
    }
}
