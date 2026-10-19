package net.kogasachan.kogasa_addiction.network;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class TenshiAbilityPacket {
    private final double charge;
    private final boolean isReady;

    public TenshiAbilityPacket(double charge, boolean isReady) {
        this.charge = charge;
        this.isReady = isReady;
    }

    public TenshiAbilityPacket(FriendlyByteBuf buf) {
        this.charge = buf.readDouble();
        this.isReady = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeDouble(charge);
        buf.writeBoolean(isReady);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            var player = Minecraft.getInstance().player;
            if (player != null) {
                player.getPersistentData().putDouble("KogasaTenshiCharge", charge);
                player.getPersistentData().putBoolean("KogasaTenshiBurstReady", isReady);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
