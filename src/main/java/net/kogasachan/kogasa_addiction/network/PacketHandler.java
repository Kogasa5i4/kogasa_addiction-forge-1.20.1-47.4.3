package net.kogasachan.kogasa_addiction.network;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public static void register() {
        INSTANCE.messageBuilder(ScreenshotTakenPacket.class, packetId++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ScreenshotTakenPacket::encode)
                .decoder(ScreenshotTakenPacket::new)
                .consumerMainThread(ScreenshotTakenPacket::handle)
                .add();

        INSTANCE.messageBuilder(TenshiSyncPacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TenshiSyncPacket::encode)
                .decoder(TenshiSyncPacket::new)
                .consumerMainThread(TenshiSyncPacket::handle)
                .add();

        INSTANCE.messageBuilder(TenshiBeaconBlockPlacePacket.class, packetId++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(TenshiBeaconBlockPlacePacket::encode)
                .decoder(TenshiBeaconBlockPlacePacket::new)
                .consumerMainThread(TenshiBeaconBlockPlacePacket::handle)
                .add();
    }
}