package net.kogasachan.kogasa_addiction.util;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * 通用 registry id 列表匹配（支持 {@code *} / {@code ?} 通配符），用于配置里的
 * 动态数值材料列表、附魔列表等。
 *
 * <p>每次调用直接读配置列表，不做缓存，避免配置重载后缓存不失效。
 */
public final class KogasaIdListMatcher {

    private KogasaIdListMatcher() {
    }

    public static boolean matches(ResourceLocation id, List<? extends String> patterns) {
        return id != null && matchesString(id.toString(), patterns);
    }

    /**
     * 同一套通配符规则，用于类名/包名等任意字符串。
     */
    public static boolean matchesString(String full, List<? extends String> patterns) {
        if (full == null || patterns == null || patterns.isEmpty()) {
            return false;
        }
        for (String raw : patterns) {
            if (raw == null) continue;
            String pattern = raw.trim();
            if (pattern.isEmpty()) continue;

            if (pattern.indexOf('*') >= 0 || pattern.indexOf('?') >= 0) {
                if (full.matches(globToRegex(pattern))) {
                    return true;
                }
            } else if (full.equals(pattern)) {
                return true;
            }
        }
        return false;
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' -> sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }
}
