package net.kogasachan.kogasa_addiction.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public record TenshiBeaconBlockReplacementPacket(BlockPos pos) {

    public TenshiBeaconBlockReplacementPacket(FriendlyByteBuf buf) {
        this(buf.readBlockPos());
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static void handle(TenshiBeaconBlockReplacementPacket msg, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientPacketHandler.cancelBlockPlace(msg.pos()));
        ctx.get().setPacketHandled(true);
    }
}
