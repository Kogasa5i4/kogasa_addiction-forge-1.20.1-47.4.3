package net.kogasachan.kogasa_addiction.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：把指定的快捷栏槽位与副手槽位强制同步成服务端真实物品。
 *
 * <p>用于"服务端取消了方块放置，但客户端已经预测扣掉了 1 个方块"的场景
 * （天子锭触发时取消放置）：不发这个包，客户端手持数量会一直比服务端少 1，
 * 直到下一次别的同步才纠正。
 *
 * <p>用槽位号而不是"手"：延迟期间玩家可能切换快捷栏，按槽位写回不会覆盖错物品。
 */
public class HeldItemSyncPacket {

    private final int mainSlot;
    private final ItemStack mainStack;
    private final ItemStack offStack;

    public HeldItemSyncPacket(int mainSlot, ItemStack mainStack, ItemStack offStack) {
        this.mainSlot = mainSlot;
        this.mainStack = mainStack.copy();
        this.offStack = offStack.copy();
    }

    public HeldItemSyncPacket(FriendlyByteBuf buf) {
        this.mainSlot = buf.readVarInt();
        this.mainStack = buf.readItem();
        this.offStack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(mainSlot);
        buf.writeItem(mainStack);
        buf.writeItem(offStack);
    }

    public static void handle(HeldItemSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.syncHeldItems(msg.mainSlot, msg.mainStack, msg.offStack)));
        ctx.get().setPacketHandled(true);
    }
}
