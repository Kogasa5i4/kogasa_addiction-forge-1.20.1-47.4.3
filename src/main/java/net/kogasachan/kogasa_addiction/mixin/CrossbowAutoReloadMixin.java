package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.server.level.ServerPlayer;
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
     * 注入点在 setCharged(stack, false) 之后，此时弩已变回未装填状态
     */
    @Inject(
            method = "use",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/CrossbowItem;setCharged(Lnet/minecraft/world/item/ItemStack;Z)V",
                    shift = At.Shift.AFTER
            )
    )
    private void kogasa$autoReload(Level pLevel, Player pPlayer, InteractionHand pHand,
                                   CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = pPlayer.getItemInHand(pHand);
        if (!net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(stack)) return;

        // 成长值是否解锁自动装填
        if (!KogasaEquipmentHelper.isCrossbowAutoReload(stack)) return;

        // 此时弩已发射完、变为未装填
        if (invokeIsCharged(stack)) return;

        // ===== 客户端：只同步"已装填"状态 =====
        // 自动装填的动作由服务端执行，但 Charged 标记要等物品同步才会到客户端。
        // 不补这一步的话，客户端在这段时间里会以为弩是空的：玩家按住右键"想装填"，
        // 每次点击都被服务端当成开火 —— 表现就是一次装填能连射好几发（伪连发），
        // 多余的箭还是"不消耗"掷骰产生的不可拾取箭。
        if (pLevel.isClientSide) {
            if (!pPlayer.getProjectile(stack).isEmpty()) {
                invokeSetCharged(stack, true);
            }
            return;
        }

        // 背包里有箭才能装填
        if (pPlayer.getProjectile(stack).isEmpty()) return;

        if (invokeTryLoad(pPlayer, stack)) {
            invokeSetCharged(stack, true);
            pLevel.playSound(null, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(),
                    SoundEvents.CROSSBOW_LOADING_END, SoundSource.PLAYERS,
                    1.0F, 1.0F / (pLevel.getRandom().nextFloat() * 0.5F + 1.0F) + 0.2F);
            // 立即把装填结果同步给客户端（Charged 标记与装填列表都在物品 NBT 里）
            if (pPlayer instanceof ServerPlayer serverPlayer) {
                serverPlayer.containerMenu.broadcastChanges();
            }
        }
    }
}
