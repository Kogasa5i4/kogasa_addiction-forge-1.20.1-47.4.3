package net.kogasachan.kogasa_addiction.network;

import net.kogasachan.kogasa_addiction.event.client.ClientConfigCache;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 服务端 → 客户端：同步全部配置值（路径 → 字符串）。
 *
 * <p>客户端只把同步值用于**显示**（tooltip、帕秋莉手册数值），逻辑仍以服务端为准。
 * 登录时单发一次，配置重载 / {@code /kogasa config set} 后广播给所有在线玩家。
 */
public class ConfigSyncPacket {

    private final Map<String, String> values;

    public ConfigSyncPacket(Map<String, String> values) {
        this.values = new HashMap<>(values);
    }

    public ConfigSyncPacket(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        Map<String, String> read = new HashMap<>(Math.max(4, size));
        for (int i = 0; i < size; i++) {
            read.put(buf.readUtf(), buf.readUtf());
        }
        this.values = read;
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(values.size());
        for (Map.Entry<String, String> entry : values.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeUtf(entry.getValue());
        }
    }

    public static void handle(ConfigSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT,
                () -> () -> ClientConfigCache.accept(msg.values)));
        ctx.get().setPacketHandled(true);
    }
}
