package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CrossbowItem.class)
public class CrossbowAutoReloadMixin {
    @Invoker("isCharged")
    public static boolean invokeIsCharged(ItemStack stack) {
        throw new AssertionError();
    }

    @Invoker("setCharged")
    public static void invokeSetCharged(ItemStack stack, boolean charged) {
        throw new AssertionError();
    }

    @Invoker("tryLoadProjectiles")
    public static boolean invokeTryLoad(LivingEntity shooter, ItemStack stack) {
        throw new AssertionError();
    }

    /**
     * 发射完成后自动重新装填（消耗背包里的箭）
     * 注入点在 setCharged(itemstack, false) 之后，此时弩已变回未装填状态
     */
    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/CrossbowItem;setCharged(Lnet/minecraft/world/item/ItemStack;Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void kogasa$autoReload(Level level, Player player, InteractionHand hand,
                                   CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide) return;

        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof IKogasaEquipment)) return;

        // 成长值是否解锁自动装填
        if (!KogasaEquipmentHelper.isCrossbowAutoReload(stack)) return;

        // 此时弩已发射完、变为未装填
        if (invokeIsCharged(stack)) return;

        // 背包里有箭才能装填
        if (player.getProjectile(stack).isEmpty()) return;

        if (invokeTryLoad(player, stack)) {
            invokeSetCharged(stack, true);
            level.playSound(null, player.getX(), player.getY(), player.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS,
                    1.0F, 1.0F / (level.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
        }
    }
}
