package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.network.ConfigSyncPacket;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.kogasachan.kogasa_addiction.util.ConfigEntries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * 把服务端配置同步给客户端（仅用于客户端显示，如 tooltip / 帕秋莉手册数值）。
 *
 * <p>发送时机：
 * <ul>
 *   <li>玩家登录；</li>
 *   <li>配置重载（`/reload` 或文件被修改后 Forge 的自动重载）；</li>
 *   <li>{@code /kogasa config set} 保存后由 {@link #broadcastAll(MinecraftServer)} 主动广播。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ConfigSyncHandler {

    private ConfigSyncHandler() {
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            sendTo(player);
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // 服务端启动后如果有玩家（极少见）也补发一次
        broadcastAll(event.getServer());
    }

    @SubscribeEvent
    public static void onConfigReloading(ModConfigEvent.Reloading event) {
        if (event.getConfig().getType() != net.minecraftforge.fml.config.ModConfig.Type.COMMON) {
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        // 重载可能来自文件监听线程，切回服务端线程再发包
        server.execute(() -> broadcastAll(server));
    }

    public static void sendTo(ServerPlayer player) {
        if (player == null || player.connection == null) {
            return;
        }
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new ConfigSyncPacket(ConfigEntries.snapshot()));
    }

    public static void broadcastAll(MinecraftServer server) {
        if (server == null || server.getPlayerList().getPlayerCount() == 0) {
            return;
        }
        PacketHandler.INSTANCE.send(
                PacketDistributor.ALL.noArg(),
                new ConfigSyncPacket(ConfigEntries.snapshot()));
    }
}
