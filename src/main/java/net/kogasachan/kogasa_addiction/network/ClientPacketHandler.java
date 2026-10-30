package net.kogasachan.kogasa_addiction.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;

public class ClientPacketHandler {
    public static void cancelBlockPlace(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // flag 3 = Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS，立刻重渲染，消除预测方块
        mc.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }
}
