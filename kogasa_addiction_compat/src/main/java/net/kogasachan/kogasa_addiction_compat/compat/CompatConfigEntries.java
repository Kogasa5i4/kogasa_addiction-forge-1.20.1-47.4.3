package net.kogasachan.kogasa_addiction_compat.compat;

import net.minecraftforge.common.ForgeConfigSpec;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * 反射扫描 {@link CompatConfig} 里的 {@link ForgeConfigSpec.ConfigValue} 字段，
 * 建立“路径 → 句柄”表，供 {@code /kogasa config} 的兼容配置页使用。
 */
public final class CompatConfigEntries {

    private static Map<String, ForgeConfigSpec.ConfigValue<?>> values;

    private CompatConfigEntries() {
    }

    public static Map<String, ForgeConfigSpec.ConfigValue<?>> values() {
        if (values != null) {
            return values;
        }

        Map<String, ForgeConfigSpec.ConfigValue<?>> map = new TreeMap<>();
        for (Field field : CompatConfig.class.getDeclaredFields()) {
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
                    map.put(String.join(".", value.getPath()), value);
                }
            } catch (ReflectiveOperationException ignored) {
                // 取不到就跳过
            }
        }
        values = new LinkedHashMap<>(map);
        return values;
    }
}
