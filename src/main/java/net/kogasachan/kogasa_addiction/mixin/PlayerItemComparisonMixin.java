package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 修复"成长写入导致攻击冷却频繁重置"。
 *
 * <p>{@code Player#tick} 每一 tick 都用 {@code ItemStack.matches(lastItemInMainHand, 主手)}
 * 判断主手是否变化，变化就 {@code resetAttackStrengthTicker()}。成长每次攻击/挖掘都会写
 * {@code KogasaEquipment.Growth}（+0.001）到物品 NBT，于是每次攻击后下一 tick 都被判定为
 * "物品变了"，攻击冷却直接清零。
 *
 * <p>这里把该调用替换为"忽略本模组高频数据根"的比较
 * （{@link KogasaEquipmentHelper#matchesIgnoringGrowth}），数量语义与原版一致。
 */
@Mixin(Player.class)
public class PlayerItemComparisonMixin {

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/ItemStack;matches(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z"
            )
    )
    private boolean kogasa$matchesIgnoringGrowth(ItemStack pStack, ItemStack pOther) {
        return KogasaEquipmentHelper.matchesIgnoringGrowth(pStack, pOther);
    }
}
