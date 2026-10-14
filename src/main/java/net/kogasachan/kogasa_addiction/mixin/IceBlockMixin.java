package net.kogasachan.kogasa_addiction.mixin;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(IceBlock.class)
public class IceBlockMixin {

    /**
     * @author Kogasa Chan
     * @reason 霜冰自然融化时概率掉落琪露诺锭
     */
    @Inject(method = "melt", at = @At("HEAD"))
    private void kogasa$onMelt(BlockState state, Level level, BlockPos pos, CallbackInfo ci) {
        // 只处理霜冰，普通冰融化（在 ultraWarm 维度）不掉
        if (!(state.getBlock() instanceof FrostedIceBlock)) return;
        if (!(level instanceof ServerLevel serverLevel)) return;

        if (serverLevel.random.nextFloat() < ModConfigCache.cirnoIngotDropChance) {
            Block.popResource(serverLevel, pos, new ItemStack(ModItems.CIRNO_INGOT.get()));
        }
    }
}