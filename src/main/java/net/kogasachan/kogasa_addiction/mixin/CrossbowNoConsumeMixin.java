package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 弩的"概率不消耗箭矢"。
 *
 * <p>思路是**掷骰命中时根本不消耗**：在 {@code loadProjectile} 开头接手，
 * 用弹药栈的复制品（count=1）调用原版私有方法 {@code addChargedProjectile} 完成装填，
 * 然后取消原方法——原版那条 {@code pAmmoStack.split(1)} 根本不会执行。
 *
 * <p>这样就不需要任何"消耗后再返还"的补丁：弹药栈数量与所在槽位自始至终不变，
 * 不存在满组溢出（64→66）、也不存在返还位置不对或背包满时掉落的问题。
 *
 * <p>掷骰失败/不满足条件时原方法照常执行，行为与原版一致。
 */
@Mixin(CrossbowItem.class)
public class CrossbowNoConsumeMixin {

    @Unique
    private static final String TAG_NO_CONSUME = "KogasaNoConsume";

    /** 原版私有装填方法：把一支弹药物品追加到弩的 ChargedProjectiles 列表 */
    @Invoker("addChargedProjectile")
    static void kogasa$addChargedProjectile(ItemStack pCrossbowStack, ItemStack pAmmoStack) {
        throw new AssertionError();
    }

    /**
     * 每次装填前清除上次的不消耗标记
     */
    @Inject(method = "tryLoadProjectiles", at = @At("HEAD"))
    private static void kogasa$clearNoConsumeMark(LivingEntity pShooter, ItemStack pCrossbowStack,
                                                  CallbackInfoReturnable<Boolean> cir) {
        if (pCrossbowStack.getTag() != null) {
            pCrossbowStack.getTag().remove(TAG_NO_CONSUME);
        }
    }

    /**
     * 只在"原版会 split 的那条路径"上（非创造 + 第 1 支 + 弹药非空）掷骰；
     * 命中则不 split、直接以复制品装填并取消原方法。
     */
    @Inject(method = "loadProjectile", at = @At("HEAD"), cancellable = true)
    private static void kogasa$loadWithoutConsuming(LivingEntity pShooter, ItemStack pCrossbowStack,
                                                    ItemStack pAmmoStack, boolean pHasAmmo,
                                                    boolean pIsCreative,
                                                    CallbackInfoReturnable<Boolean> cir) {
        if (pIsCreative || pHasAmmo) return;
        if (pAmmoStack.isEmpty()) return;
        if (!pCrossbowStack.is(ModItems.AYA_CROSSBOW.get())) return;
        if (!(pShooter instanceof ServerPlayer player)) return;
        if (player.getAbilities().instabuild) return;

        double chance = KogasaEquipmentHelper.getNoConsumeArrowChance(pCrossbowStack);
        if (chance <= 0.0D) return;
        if (player.getRandom().nextDouble() >= chance) return;

        // 掷骰命中：复制品装填，弹药栈完全不动（等价于"这一发不消耗"）
        ItemStack loaded = pAmmoStack.copy();
        loaded.setCount(1);
        kogasa$addChargedProjectile(pCrossbowStack, loaded);

        // 发射时 CrossbowProjectileHandler 会读这个标记，把这支箭设为不可拾取
        pCrossbowStack.getOrCreateTag().putBoolean(TAG_NO_CONSUME, true);

        cir.setReturnValue(true);
    }
}
