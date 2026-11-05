package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.event.common.CrossbowProjectileHandler;
import net.kogasachan.kogasa_addiction.event.common.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 弩的成长与装填速度。
 *
 * <p><b>本 mixin 不再使用任何 {@code @Redirect}。</b>原先拦截
 * {@code Level.addFreshEntity}（改穿透/弹速/弹药/烟花）的逻辑，已整体迁移到
 * {@link CrossbowProjectileHandler}，用 Forge 的 {@code EntityJoinLevelEvent} 实现 ——
 * 事件订阅不会和别的模组抢同一处字节码注入点，从根上消除了启动期冲突。
 */
@Mixin(CrossbowItem.class)
public class CrossbowMixin {

    // ========== 1. 发射时触发成长，并重置本次射击的烟花标记 ==========
    @Inject(method = "performShooting", at = @At("HEAD"))
    private static void kogasa$onShoot(Level pLevel, LivingEntity pShooter,
                                       InteractionHand pUsedHand, ItemStack pCrossbowStack,
                                       float pVelocity, float pInaccuracy,
                                       CallbackInfo ci) {
        if (pLevel.isClientSide) return;

        // 每次射击清一次"烟花已生成"标记，供实体加入世界时去重
        if (pCrossbowStack.getTag() != null) {
            pCrossbowStack.getTag().remove(CrossbowProjectileHandler.TAG_FIREWORK_SPAWNED);
        }

        if (!net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(pCrossbowStack)) return;
        if (!(pShooter instanceof ServerPlayer player)) return;

        KogasaGrowthHandler.addGrowth(pCrossbowStack, player);
    }

    // ========== 2. 装填速度：总倍率 + 强化吸收的拉弓速度 ==========
    @Inject(method = "getChargeDuration", at = @At("RETURN"), cancellable = true)
    private static void kogasa$modifyChargeDuration(ItemStack pCrossbowStack,
                                                    CallbackInfoReturnable<Integer> cir) {
        if (!net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(pCrossbowStack)) return;

        int original = cir.getReturnValue();
        if (original <= 0) return;

        // 链 1：总倍率（沿用原行为）
        double multiplier = KogasaEquipmentData.getMultiplier(pCrossbowStack);
        int shortened = (int) Math.round(original / Math.sqrt(multiplier));

        // 链 2：强化吸收来的拉弓速度
        shortened = KogasaEquipmentHelper.getEffectiveDrawTicks(pCrossbowStack, Math.max(1, shortened));

        cir.setReturnValue(shortened);
    }
}
