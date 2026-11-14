package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.util.ConfigEntries;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 客户端侧配置缓存：保存服务端通过 {@code ConfigSyncPacket} 同步来的配置值。
 *
 * <p><b>只用于显示</b>（tooltip、帕秋莉手册数值），任何逻辑都必须走服务端配置。
 * 没有收到同步数据时（单机、离线）自动回退到本地 {@link ForgeConfigSpec.ConfigValue#get()}，
 * 所以调用方可以无脑用 {@link #get(ForgeConfigSpec.ConfigValue)} 替代 {@code value.get()}。
 */
public final class ClientConfigCache {

    private static Map<String, String> synced = Map.of();

    private ClientConfigCache() {
    }

    /**
     * 由 {@code ConfigSyncPacket} 调用，替换整份缓存
     */
    public static void accept(Map<String, String> values) {
        synced = new HashMap<>(values);
    }

    /**
     * 有同步值时优先用同步值，否则回退到本地配置
     */
    @SuppressWarnings("unchecked")
    public static <T> T get(ForgeConfigSpec.ConfigValue<T> value) {
        String raw = synced.get(ConfigEntries.pathOf(value));
        if (raw == null) {
            return value.get();
        }
        try {
            return (T) parse(raw, value.get());
        } catch (RuntimeException ex) {
            return value.get();
        }
    }

    private static Object parse(String raw, Object fallback) {
        if (fallback instanceof Boolean) {
            return Boolean.parseBoolean(raw);
        }
        if (fallback instanceof Integer) {
            return Integer.parseInt(raw);
        }
        if (fallback instanceof Long) {
            return Long.parseLong(raw);
        }
        if (fallback instanceof Double) {
            return Double.parseDouble(raw);
        }
        if (fallback instanceof String) {
            return raw;
        }
        if (fallback instanceof List<?>) {
            List<String> parts = new ArrayList<>();
            if (!raw.isEmpty()) {
                for (String part : raw.split(String.valueOf(ConfigEntries.LIST_SEPARATOR))) {
                    parts.add(part);
                }
            }
            return parts;
        }
        throw new IllegalArgumentException("unsupported config type");
    }
}
