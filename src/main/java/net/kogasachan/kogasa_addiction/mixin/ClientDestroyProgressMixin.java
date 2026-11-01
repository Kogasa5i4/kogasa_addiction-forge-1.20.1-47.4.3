package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 修复"成长写入导致挖掘进度频繁清零"。
 *
 * <p>{@code MultiPlayerGameMode#sameDestroyTarget} 用
 * {@code isSameItemSameTags(当前主手, 开始破坏时缓存)} 与
 * {@code shouldCauseBlockBreakReset}（Forge 默认实现同样逐键比较 tag）判断挖掘是否继续。
 * 成长写入会让物品 NBT 变化 → 判定为"换了物品" → 进度归零。
 *
 * <p>这里整体重算这一行：两个比较都换成"忽略本模组高频数据根"的版本，
 * 其余语义（位置相同、物品类型相同、耐久不计）与原版一致。
 */
@Mixin(MultiPlayerGameMode.class)
public class ClientDestroyProgressMixin {

    @Shadow
    private BlockPos destroyBlockPos;

    @Shadow
    private ItemStack destroyingItem;

    @Inject(method = "sameDestroyTarget", at = @At("HEAD"), cancellable = true)
    private void kogasa$ignoreGrowthChanges(BlockPos pPos, CallbackInfoReturnable<Boolean> cir) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return; // 异常状态，保留原版逻辑
        }

        ItemStack current = player.getMainHandItem();
        cir.setReturnValue(
                pPos.equals(this.destroyBlockPos)
                        && KogasaEquipmentHelper.areStacksEffectivelySame(current, this.destroyingItem)
                        && !KogasaEquipmentHelper.shouldCauseBlockBreakResetIgnoringGrowth(this.destroyingItem, current));
    }
}
