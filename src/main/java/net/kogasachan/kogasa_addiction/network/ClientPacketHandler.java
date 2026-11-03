package net.kogasachan.kogasa_addiction.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

public class ClientPacketHandler {
    public static void cancelBlockPlace(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        // flag 3 = Block.UPDATE_NEIGHBORS | Block.UPDATE_CLIENTS，立刻重渲染，消除预测方块
        mc.level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
    }

    /**
     * 用服务端的真实物品覆盖客户端预测后的手持物品。
     * 主要修复"服务端取消方块放置、客户端已预测扣掉 1 个方块"导致的显示数量不一致。
     * 按快捷栏槽位写回，避免延迟期间玩家切换选中槽位时覆盖错物品。
     */
    public static void syncHeldItems(int mainSlot, ItemStack mainStack, ItemStack offStack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        mc.player.getInventory().setItem(mainSlot, mainStack);
        mc.player.getInventory().offhand.set(0, offStack);
    }
}
