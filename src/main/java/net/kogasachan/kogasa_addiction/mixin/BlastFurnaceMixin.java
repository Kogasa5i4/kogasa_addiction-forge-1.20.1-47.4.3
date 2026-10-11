package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.MixinConfigCache;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(AbstractFurnaceBlockEntity.class)
public class BlastFurnaceMixin {

    /**
     * @author Kogasa Chan
     * @reason 高炉燃烧时概率产出特殊锭
     */
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void kogasa$produceIngot(Level level, BlockPos pos, BlockState state,
                                            AbstractFurnaceBlockEntity blockEntity,
                                            CallbackInfo ci) {
        if (!MixinConfigCache.enableBlastFurnaceIngot) return;
        if (level.isClientSide) return;

        // 仅高炉生效，普通熔炉和烟熏炉不产出
        if (!(blockEntity instanceof BlastFurnaceBlockEntity)) return;

        // 必须处于燃烧状态
        if (!blockEntity.isLit()) return;

        // 概率判定
        if (level.random.nextFloat() >= MixinConfigCache.blastFurnaceIngotChance) return;

        // 在方块上方掉落
        ItemStack drop = new ItemStack(ModItems.KOGASA_INGOT.get());
        net.minecraft.world.entity.item.ItemEntity itemEntity =
                new net.minecraft.world.entity.item.ItemEntity(
                        level,
                        pos.getX() + 0.5,
                        pos.getY() + 1.0,
                        pos.getZ() + 0.5,
                        drop
                );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}