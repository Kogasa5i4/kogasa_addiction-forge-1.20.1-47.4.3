package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlastFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(AbstractFurnaceBlockEntity.class)
public class BlastFurnaceMixin {

    /**
     * @author Kogasa Chan
     * @reason 高炉燃烧时概率产出伞之锭，优先输出到相邻容器
     */
    @Inject(method = "serverTick", at = @At("HEAD"))
    private static void kogasa$produceIngot(Level level, BlockPos pos, BlockState state,
                                            AbstractFurnaceBlockEntity blockEntity,
                                            CallbackInfo ci) {
        if (!ModConfigCache.enableKogasaIngot) return;
        if (level.isClientSide) return;
        if (!(blockEntity instanceof BlastFurnaceBlockEntity)) return;
        if (!blockEntity.isLit()) return;
        if (level.random.nextFloat() >= ModConfigCache.kogasaIngotChance) return;

        ItemStack drop = new ItemStack(ModItems.KOGASA_INGOT.get());

        Direction facing = state.getValue(AbstractFurnaceBlock.FACING);
        Direction[] faces = {
                facing,                       // 前
                facing.getClockWise(),        // 左
                facing.getCounterClockWise(), // 右
                facing.getOpposite(),         // 后
                Direction.UP,                 // 上
                Direction.DOWN                // 下
        };

        // 1. 插入六个面的容器
        for (Direction dir : faces) {
            BlockPos targetPos = pos.relative(dir);
            BlockEntity be = level.getBlockEntity(targetPos);
            if (be == null) continue;

            Optional<IItemHandler> resolved = be.getCapability(
                    ForgeCapabilities.ITEM_HANDLER,
                    dir.getOpposite()
            ).resolve();

            if (resolved.isEmpty()) continue;
            if (kogasa_addiction_forge_1_20_1_47_4_3$insertFully(resolved.get(), drop)) return;
        }

        // 2. 六个面有空气/流体 → 掉落
        for (Direction dir : faces) {
            BlockPos targetPos = pos.relative(dir);
            if (kogasa_addiction_forge_1_20_1_47_4_3$canPlaceItemAt(level, targetPos)) {
                kogasa_addiction_forge_1_20_1_47_4_3$spawnItem(level, targetPos, drop);
                return;
            }
        }

        // 3. 上方搜索空气
        BlockPos searchPos = pos.above();
        for (int i = 0; i < ModConfigCache.kogasaIngotSearchHeight; i++) {
            if (kogasa_addiction_forge_1_20_1_47_4_3$canPlaceItemAt(level, searchPos)) {
                kogasa_addiction_forge_1_20_1_47_4_3$spawnItem(level, searchPos, drop);
                return;
            }
            searchPos = searchPos.above();
        }

        // 4. 上表面中心
        kogasa_addiction_forge_1_20_1_47_4_3$spawnItem(level, pos.above(), drop);
    }

    /**
     * 尝试完全插入容器，返回是否成功
     */
    @Unique
    private static boolean kogasa_addiction_forge_1_20_1_47_4_3$insertFully(IItemHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int i = 0; i < handler.getSlots() && !remaining.isEmpty(); i++) {
            remaining = handler.insertItem(i, remaining, false);
        }
        return remaining.isEmpty();
    }

    /**
     * 该位置能否放置掉落物
     */
    @Unique
    private static boolean kogasa_addiction_forge_1_20_1_47_4_3$canPlaceItemAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        if (!state.getFluidState().isEmpty()) return true;
        // 非实心方块（草、火把等）
        return state.getCollisionShape(level, pos).isEmpty();
    }

    /**
     * 在指定位置生成掉落物
     */
    @Unique
    private static void kogasa_addiction_forge_1_20_1_47_4_3$spawnItem(Level level, BlockPos pos, ItemStack stack) {
        ItemEntity itemEntity = new ItemEntity(
                level,
                pos.getX() + 0.5,
                pos.getY() + 0.5,
                pos.getZ() + 0.5,
                stack
        );
        itemEntity.setDefaultPickUpDelay();
        level.addFreshEntity(itemEntity);
    }
}