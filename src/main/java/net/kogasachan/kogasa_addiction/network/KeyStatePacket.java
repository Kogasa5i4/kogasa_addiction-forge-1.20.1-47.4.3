package net.kogasachan.kogasa_addiction.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class KeyStatePacket {

    private final boolean ctrl;
    private final boolean alt;

    public KeyStatePacket(boolean ctrl, boolean alt) {
        this.ctrl = ctrl;
        this.alt = alt;
    }

    public KeyStatePacket(FriendlyByteBuf buf) {
        this.ctrl = buf.readBoolean();
        this.alt = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(ctrl);
        buf.writeBoolean(alt);
    }

    public static void handle(KeyStatePacket msg, Supplier<NetworkEvent.Context> ctxSupplier) {
        NetworkEvent.Context ctx = ctxSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player != null) {
                KeyStateTracker.update(player.getUUID(), msg.ctrl, msg.alt);
            }
        });
        ctx.setPacketHandled(true);
    }
}