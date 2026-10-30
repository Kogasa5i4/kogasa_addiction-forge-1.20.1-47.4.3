package net.kogasachan.kogasa_addiction.network;

import net.kogasachan.kogasa_addiction.event.common.TimestopHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class TimestopPacket {

    public TimestopPacket() {
    }

    public TimestopPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                // 交给服务端 Handler 处理逻辑
                TimestopHandler.tryActivateTimestop(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}