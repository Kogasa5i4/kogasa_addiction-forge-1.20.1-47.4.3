package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 反射扫描 {@link Config} 里的所有 {@link ForgeConfigSpec.ConfigValue} 字段，
 * 建立"配置路径 → 句柄"表。
 *
 * <p>用途：
 * <ul>
 *   <li>{@code /kogasa config} 调试指令；</li>
 *   <li>登录/重载时把配置打包发给客户端（{@link #snapshot()}）。</li>
 * </ul>
 * 新增配置项无需改这里。
 */
public final class ConfigEntries {

    /** 列表值在同步包里的分隔符（配置文件里的列表项不会包含它） */
    public static final char LIST_SEPARATOR = '\u001f';

    private static Map<String, ForgeConfigSpec.ConfigValue<?>> values;

    private ConfigEntries() {
    }

    public static Map<String, ForgeConfigSpec.ConfigValue<?>> values() {
        if (values != null) {
            return values;
        }

        Map<String, ForgeConfigSpec.ConfigValue<?>> map = new TreeMap<>();
        for (Field field : Config.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!ForgeConfigSpec.ConfigValue.class.isAssignableFrom(field.getType())) {
                continue;
            }
            try {
                field.setAccessible(true);
                Object raw = field.get(null);
                if (raw instanceof ForgeConfigSpec.ConfigValue<?> value) {
                    map.put(pathOf(value), value);
                }
            } catch (ReflectiveOperationException ignored) {
                // 取不到就跳过，不影响其它配置项
            }
        }
        values = new LinkedHashMap<>(map);
        return values;
    }

    /** 配置文件的点分路径，例如 {@code growth.growthPerUse} */
    public static String pathOf(ForgeConfigSpec.ConfigValue<?> value) {
        return String.join(".", value.getPath());
    }

    /** 全部配置的"路径 → 值字符串"快照（列表用 {@link #LIST_SEPARATOR} 连接） */
    public static Map<String, String> snapshot() {
        Map<String, String> snapshot = new LinkedHashMap<>();
        for (Map.Entry<String, ForgeConfigSpec.ConfigValue<?>> entry : values().entrySet()) {
            snapshot.put(entry.getKey(), stringify(entry.getValue().get()));
        }
        return snapshot;
    }

    /** 把配置值转成同步用的字符串 */
    public static String stringify(Object value) {
        if (value instanceof List<?> list) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(LIST_SEPARATOR);
                }
                builder.append(list.get(i));
            }
            return builder.toString();
        }
        return String.valueOf(value);
    }
}
