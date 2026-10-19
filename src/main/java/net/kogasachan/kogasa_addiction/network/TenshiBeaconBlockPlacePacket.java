package net.kogasachan.kogasa_addiction.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TenshiBeaconBlockPlacePacket(BlockPos pos) {

    public TenshiBeaconBlockPlacePacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void handle(TenshiBeaconBlockPlacePacket msg, Supplier<NetworkEvent.Context> ctx) {
        // consumerMainThread 已保证在主线程；这里只做分发，避免专用服务器加载客户端类
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.cancelBlockPlace(msg.pos()));
        ctx.get().setPacketHandled(true);
    }
}
