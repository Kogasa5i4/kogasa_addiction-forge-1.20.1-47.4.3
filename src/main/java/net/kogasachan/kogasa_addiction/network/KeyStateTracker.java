package net.kogasachan.kogasa_addiction.network;

import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class KeyStateTracker {

    private static final Map<UUID, boolean[]> STATES = new ConcurrentHashMap<>();

    private KeyStateTracker() {
    }

    public static void update(UUID uuid, boolean ctrl, boolean alt) {
        STATES.put(uuid, new boolean[]{ctrl, alt});
    }

    public static void clear(UUID uuid) {
        STATES.remove(uuid);
    }

    public static boolean isCtrlDown(Player player) {
        boolean[] s = STATES.get(player.getUUID());
        return s != null && s[0];
    }

    public static boolean isAltDown(Player player) {
        boolean[] s = STATES.get(player.getUUID());
        return s != null && s[1];
    }
}
