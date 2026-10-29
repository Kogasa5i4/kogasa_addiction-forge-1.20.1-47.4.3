package net.kogasachan.kogasa_addiction.network;

import net.kogasachan.kogasa_addiction.client.ClientTimestopState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步时停是否生效，供 Tooltip 显示可用状态。
 */
public class TimestopStatePacket {

    private final boolean frozen;

    public TimestopStatePacket(boolean frozen) {
        this.frozen = frozen;
    }

    public TimestopStatePacket(FriendlyByteBuf buf) {
        this.frozen = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.frozen);
    }

    public static void handle(TimestopStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientTimestopState.setFrozen(msg.frozen)));
        ctx.get().setPacketHandled(true);
    }
}
