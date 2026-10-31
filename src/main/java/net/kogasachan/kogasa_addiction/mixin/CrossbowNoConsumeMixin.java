package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 弩的"概率不消耗箭矢"。
 *
 * <p>旧实现用 {@code @Redirect} 拦截 {@code loadProjectile} 里的
 * {@code ItemStack.split(I)}，那是启动期冲突源：别的弓弩类模组只要对同一指令再 {@code @Redirect}
 * 一次就会崩。现在改成**不拦截调用本身**：
 *
 * <p>原版 {@code loadProjectile} 只有在 {@code !creative && !hasAmmo}（也就是第 1 支箭）时才会
 * {@code pAmmoStack.split(1)}。所以只要在方法开头先把弹药栈 <b>grow(1)</b> 再让原版去 split，
 * 净消耗就正好是 0：
 * <pre>
 *   原版：n → split(1) → n-1   （消耗 1 支）
 *   我方：n → grow(1) → n+1 → split(1) → n   （净消耗 0）
 * </pre>
 * 而且因为 grow 过之后栈一定非空，原版那条
 * {@code if (pAmmoStack.isEmpty()) getInventory().removeItem(...)} 也不会执行，
 * 不存在"栈被移出背包后补不回来"的问题。
 */
@Mixin(CrossbowItem.class)
public class CrossbowNoConsumeMixin {

    @Unique
    private static final String TAG_NO_CONSUME = "KogasaNoConsume";

    /**
     * 每次装填前清除上次的不消耗标记
     */
    @Inject(method = "tryLoadProjectiles", at = @At("HEAD"))
    private static void kogasa$clearNoConsumeMark(LivingEntity shooter, ItemStack crossbow,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (crossbow.getTag() != null) {
            crossbow.getTag().remove(TAG_NO_CONSUME);
        }
    }

    /**
     * 在真正发生 split 的那次调用前，把弹药栈先补一支回去。
     */
    @Inject(method = "loadProjectile", at = @At("HEAD"))
    private static void kogasa$preGrantArrow(LivingEntity shooter, ItemStack crossbow, ItemStack ammo,
                                            boolean hasAmmo, boolean creative,
                                            CallbackInfoReturnable<Boolean> cir) {
        // 只在"会被 split 的那一条路径"上动手：非创造 + 第 1 支 + 弹药非空
        if (creative || hasAmmo) return;
        if (ammo.isEmpty()) return;
        if (!crossbow.is(ModItems.AYA_CROSSBOW.get())) return;
        if (!(shooter instanceof Player player)) return;
        if (player.getAbilities().instabuild) return;

        double chance = KogasaEquipmentHelper.getNoConsumeArrowChance(crossbow);
        if (chance <= 0.0D) return;
        if (player.getRandom().nextDouble() >= chance) return;

        // 先补一支，抵消随后原版的 split(1)
        ammo.grow(1);
        crossbow.getOrCreateTag().putBoolean(TAG_NO_CONSUME, true);
    }
}
