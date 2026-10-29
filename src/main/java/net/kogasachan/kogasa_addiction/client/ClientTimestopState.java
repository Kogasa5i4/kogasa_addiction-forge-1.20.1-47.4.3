package net.kogasachan.kogasa_addiction.client;

/**
 * 客户端保存的时停状态，由 {@link net.kogasachan.kogasa_addiction.network.TimestopStatePacket} 更新。
 */
public final class ClientTimestopState {

    private static boolean frozen;

    private ClientTimestopState() {
    }

    public static boolean isFrozen() {
        return frozen;
    }

    public static void setFrozen(boolean value) {
        frozen = value;
    }
}
