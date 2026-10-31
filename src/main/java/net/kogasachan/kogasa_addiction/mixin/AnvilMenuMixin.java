package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ItemCombinerMenu;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 咲夜飞刀一次最多修复 3 把；输入超过 3 把时，把剩余损坏飞刀放回铁砧左侧输入槽，
 * 避免原版 onTake 清空整个输入堆叠导致余量丢失。
 */
@Mixin(AnvilMenu.class)
public abstract class AnvilMenuMixin {

    @Unique
    private ItemStack kogasa$inputBeforeTake;

    @Unique
    private ItemStack kogasa$materialBeforeTake;

    @Unique
    private Container kogasa$inputSlots() {
        // inputSlots 与 slotsChanged 都是 ItemCombinerMenu 的成员；inputSlots 由 AT 暴露为 public
        return ((ItemCombinerMenu) (Object) this).inputSlots;
    }

    @Inject(method = "onTake", at = @At("HEAD"))
    private void kogasa$captureInput(Player player, ItemStack output, CallbackInfo ci) {
        Container inputSlots = this.kogasa$inputSlots();
        this.kogasa$inputBeforeTake = inputSlots.getItem(0).copy();
        this.kogasa$materialBeforeTake = inputSlots.getItem(1).copy();
    }

    @Inject(method = "onTake", at = @At("TAIL"))
    private void kogasa$returnRemainder(Player player, ItemStack output, CallbackInfo ci) {
        ItemStack input = this.kogasa$inputBeforeTake;
        ItemStack material = this.kogasa$materialBeforeTake;
        this.kogasa$inputBeforeTake = null;
        this.kogasa$materialBeforeTake = null;

        if (input == null || material == null) return;
        if (!input.is(ModItems.BROKEN_SAKUYA_KNIFE.get())) return;
        if (!material.is(ModItems.SAKUYA_INGOT.get())) return;
        if (input.getCount() <= 3) return;

        ItemStack remainder = input.copy();
        remainder.setCount(input.getCount() - 3);

        Container inputSlots = this.kogasa$inputSlots();
        inputSlots.setItem(0, remainder);
        inputSlots.setChanged();

        // 直接让铁砧重算结果。
        // 这里刻意不调 slotsChanged：它在 ItemCombinerMenu 上是回调链的入口，
        // 其它铁砧类模组很可能也覆写了它，走那条路会互相递归/打架。
        // createResult() 是原版重算逻辑本身（AnvilMenu 的 public 方法），语义更确定。
        ((AnvilMenu) (Object) this).createResult();
    }
}
