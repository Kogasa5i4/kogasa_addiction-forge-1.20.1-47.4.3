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
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
     * 拦截第 1 支箭的消耗。
     * 触发不消耗 → 返回复制，不动原栈；否则走原版 split。
     * 多重射击的第 2、3 支箭走 copy() 不消耗，无需处理。
     */
    @Redirect(
            method = "loadProjectile",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;split(I)Lnet/minecraft/world/item/ItemStack;"
            )
    )
    private static ItemStack kogasa$noConsumeSplit(
            ItemStack ammoStack, int amount,
            LivingEntity pShooter, ItemStack pCrossbowStack,
            ItemStack pAmmoStack, boolean pHasAmmo, boolean pIsCreative) {

        // 非 Kogasa 弩
        if (!pCrossbowStack.is(ModItems.AYA_CROSSBOW.get())) {
            return ammoStack.split(amount);
        }
        // 非玩家
        if (!(pShooter instanceof Player player)) {
            return ammoStack.split(amount);
        }
        // 创造模式
        if (player.getAbilities().instabuild) {
            return ammoStack.split(amount);
        }

        double chance = KogasaEquipmentHelper.getNoConsumeArrowChance(pCrossbowStack);
        if (chance > 0.0 && player.getRandom().nextDouble() < chance) {
            // 触发不消耗：设置标记 + 返回复制，不动原栈
            pCrossbowStack.getOrCreateTag().putBoolean(TAG_NO_CONSUME, true);
            return ammoStack.copyWithCount(amount);
        }

        return ammoStack.split(amount);
    }
}
