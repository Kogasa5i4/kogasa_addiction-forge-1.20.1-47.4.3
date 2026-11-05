package net.kogasachan.kogasa_addiction.datagen;

import com.google.gson.*;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 语言文件 datagen：以 {@code lang_src/{en_us,zh_cn}.json} 为唯一源，生成
 * {@code src/generated/resources/assets/kogasa_addiction/lang/*.json}，并做一致性校验。
 *
 * <p>校验内容：
 * <ul>
 *   <li>两种语言的键集合必须完全一致（缺键/多键直接让 runData 失败并列出差异）；</li>
 *   <li>允许空值（例如告示牌的空白行）；</li>
 *   <li>{@code %s}/{@code %d} 占位符数量必须一致，避免翻译参数错位。</li>
 * </ul>
 *
 * <p>源文件路径按 datagen 工作目录自动探测（{@code run-data} 或工程根目录）。
 */
public class LangSrcProvider implements DataProvider {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?[sd]");
    private static final List<String> LOCALES = List.of("en_us", "zh_cn");

    private final PackOutput output;

    @Nullable
    private final Path sourceDir;

    public LangSrcProvider(PackOutput output) {
        this.output = output;
        this.sourceDir = findSourceDir();
    }

    /**
     * 只在“主工程自己的 datagen”里生效：
     * 附属的 runData 也会广播到本模组，但那时工作目录在附属工程下、找不到 lang_src，
     * 直接跳过（否则会把主模组的 lang 写进附属的 generated 目录）。
     */
    @Nullable
    private static Path findSourceDir() {
        List<Path> candidates = List.of(
                Path.of("lang_src"),
                Path.of("src", "main", "resources", "lang_src"),
                Path.of("..", "src", "main", "resources", "lang_src")
        );
        for (Path candidate : candidates) {
            if (Files.isDirectory(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        if (this.sourceDir == null) {
            // 不是主工程的 datagen（例如附属的 runData），跳过
            return CompletableFuture.completedFuture(null);
        }

        Map<String, Map<String, String>> byLocale = new LinkedHashMap<>();
        for (String locale : LOCALES) {
            byLocale.put(locale, read(locale));
        }

        validate(byLocale);

        Path langDir = output.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(KogasaAddiction.MODID + "/lang");
        List<CompletableFuture<?>> futures = new ArrayList<>();
        for (Map.Entry<String, Map<String, String>> entry : byLocale.entrySet()) {
            JsonObject json = new JsonObject();
            new TreeMap<>(entry.getValue()).forEach(json::addProperty);
            futures.add(DataProvider.saveStable(cache, json, langDir.resolve(entry.getKey() + ".json")));
        }
        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new));
    }

    private Map<String, String> read(String locale) {
        Path file = sourceDir.resolve(locale + ".json");
        if (!Files.isRegularFile(file)) {
            throw new IllegalStateException("缺少语言源文件：" + file);
        }
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            JsonElement element = JsonParser.parseReader(reader);
            JsonObject object = element.getAsJsonObject();
            Map<String, String> map = new LinkedHashMap<>();
            for (String key : object.keySet()) {
                map.put(key, object.get(key).getAsString());
            }
            return map;
        } catch (IOException | RuntimeException ex) {
            throw new IllegalStateException("读取语言源文件失败：" + file + " -> " + ex.getMessage(), ex);
        }
    }

    private void validate(Map<String, Map<String, String>> byLocale) {
        Map<String, String> en = byLocale.get("en_us");
        Map<String, String> zh = byLocale.get("zh_cn");
        if (en == null || zh == null) {
            throw new IllegalStateException("语言源必须同时提供 en_us 与 zh_cn");
        }

        TreeSet<String> missingInZh = new TreeSet<>(en.keySet());
        missingInZh.removeAll(zh.keySet());
        TreeSet<String> extraInZh = new TreeSet<>(zh.keySet());
        extraInZh.removeAll(en.keySet());

        List<String> problems = new ArrayList<>();
        if (!missingInZh.isEmpty()) {
            problems.add("zh_cn 缺少 " + missingInZh.size() + " 个键：" + missingInZh);
        }
        if (!extraInZh.isEmpty()) {
            problems.add("zh_cn 多出 " + extraInZh.size() + " 个键：" + extraInZh);
        }

        for (String key : en.keySet()) {
            String enValue = en.get(key);
            String zhValue = zh.get(key);
            if (zhValue == null) {
                continue;
            }
            int enCount = countPlaceholders(enValue);
            int zhCount = countPlaceholders(zhValue);
            if (enCount != zhCount) {
                problems.add("占位符数量不一致：" + key + "（en=" + enCount + ", zh=" + zhCount + "）");
            }
        }

        if (!problems.isEmpty()) {
            throw new IllegalStateException("lang_src 校验失败（" + problems.size() + " 项）：\n  "
                    + String.join("\n  ", problems));
        }
    }

    private static int countPlaceholders(String value) {
        Matcher matcher = PLACEHOLDER.matcher(value);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    @Override
    public String getName() {
        return "Kogasa lang_src (language files)";
    }
}
