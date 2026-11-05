package net.kogasachan.kogasa_addiction_compat.network;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 兼容附属的网络通道。
 */
public final class CompatNetwork {

    private static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(KogasaCompatAddon.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private CompatNetwork() {
    }

    public static void register() {
        int id = 0;
        INSTANCE.messageBuilder(CombineCrystalPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(CombineCrystalPacket::encode)
                .decoder(CombineCrystalPacket::new)
                .consumerMainThread(CombineCrystalPacket::handle)
                .add();
        INSTANCE.messageBuilder(ClearCompatPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(ClearCompatPacket::encode)
                .decoder(ClearCompatPacket::new)
                .consumerMainThread(ClearCompatPacket::handle)
                .add();
    }
}
