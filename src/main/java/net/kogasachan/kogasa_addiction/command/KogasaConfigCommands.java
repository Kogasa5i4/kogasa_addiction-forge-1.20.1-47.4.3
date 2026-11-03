package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.event.common.ConfigSyncHandler;
import net.kogasachan.kogasa_addiction.util.ConfigEntries;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@code /kogasa config}：调试用配置读写。
 *
 * <ul>
 *   <li>{@code list [过滤词]} —— 列出配置项与当前值；</li>
 *   <li>{@code get <路径>} —— 查看当前值与默认值；</li>
 *   <li>{@code set <路径> <值>} —— 修改并保存（会自动 {@link ModConfigCache#refresh()}）；</li>
 *   <li>{@code reset <路径>} —— 恢复默认值。</li>
 * </ul>
 *
 * <p>路径就是配置文件里的层级路径，例如 {@code growth.growthPerUse}、{@code general.miningSpeedTooltipMode}。
 * 通过反射扫描 {@link Config} 里的 {@link ForgeConfigSpec.ConfigValue} 字段建立"路径 → 值"表，
 * 以后新增配置无需再改本类。
 *
 * <p>为什么不用 gamerule：gamerule 只支持 boolean/int、属于世界存档级数据，
 * 而本模组配置是实例级、含 double/字符串/列表，语义与生命周期都对不上。
 */
public final class KogasaConfigCommands {

    /** 路径 → 配置值句柄（首次使用时构建） */
    private static Map<String, ForgeConfigSpec.ConfigValue<?>> values;

    /** 路径补全 */
    private static final SuggestionProvider<CommandSourceStack> PATH_SUGGESTIONS =
            (ctx, builder) -> {
                for (String path : valueMap().keySet()) {
                    builder.suggest(path);
                }
                return builder.buildFuture();
            };

    /** 布尔项的值补全 */
    private static final SuggestionProvider<CommandSourceStack> BOOLEAN_VALUE_SUGGESTIONS =
            (ctx, builder) -> {
                String path = StringArgumentType.getString(ctx, "path");
                ForgeConfigSpec.ConfigValue<?> value = valueMap().get(path);
                if (value != null && value.getDefault() instanceof Boolean) {
                    builder.suggest("true");
                    builder.suggest("false");
                }
                return builder.buildFuture();
            };

    private KogasaConfigCommands() {
    }

    public static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("config")
                .requires(KogasaCommandSupport.ADMIN)
                .then(Commands.literal("list")
                        .executes(ctx -> list(ctx, ""))
                        .then(Commands.argument("filter", StringArgumentType.greedyString())
                                .executes(ctx -> list(ctx, StringArgumentType.getString(ctx, "filter")))))
                .then(Commands.literal("get")
                        .then(Commands.argument("path", StringArgumentType.string())
                                .suggests(PATH_SUGGESTIONS)
                                .executes(KogasaConfigCommands::get)))
                .then(Commands.literal("set")
                        .then(Commands.argument("path", StringArgumentType.string())
                                .suggests(PATH_SUGGESTIONS)
                                .then(Commands.argument("value", StringArgumentType.greedyString())
                                        .suggests(BOOLEAN_VALUE_SUGGESTIONS)
                                        .executes(KogasaConfigCommands::set))))
                .then(Commands.literal("reset")
                        .then(Commands.argument("path", StringArgumentType.string())
                                .suggests(PATH_SUGGESTIONS)
                                .executes(KogasaConfigCommands::reset))));
    }

    // ===== 子命令 =====

    private static int get(CommandContext<CommandSourceStack> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        ForgeConfigSpec.ConfigValue<?> value = valueMap().get(path);
        if (value == null) {
            return unknownPath(ctx, path);
        }
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.config.get",
                Component.literal(path).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(value.get())).withStyle(ChatFormatting.YELLOW),
                Component.literal(String.valueOf(value.getDefault())).withStyle(ChatFormatting.GRAY)
        ), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        String raw = StringArgumentType.getString(ctx, "value");
        ForgeConfigSpec.ConfigValue<?> value = valueMap().get(path);
        if (value == null) {
            return unknownPath(ctx, path);
        }

        Object oldValue = value.get();
        Object parsed;
        try {
            parsed = parse(value.getDefault(), raw);
        } catch (RuntimeException ex) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.config.error.bad_value",
                    Component.literal(raw), Component.literal(String.valueOf(value.getDefault()))));
            return 0;
        }

        try {
            setRaw(value, parsed);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.config.error.rejected", Component.literal(String.valueOf(ex.getMessage()))));
            return 0;
        }

        Config.SPEC.save();
        // 缓存是按热路径手写的，改完配置只需要刷新一次
        ModConfigCache.refresh();
        // 把新配置同步给所有客户端（tooltip / 帕秋莉手册要用）
        ConfigSyncHandler.broadcastAll(ctx.getSource().getServer());

        Object newValue = value.get();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.config.set",
                Component.literal(path).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(oldValue)).withStyle(ChatFormatting.GRAY),
                Component.literal(String.valueOf(newValue)).withStyle(ChatFormatting.YELLOW)
        ), true);
        return 1;
    }

    private static int reset(CommandContext<CommandSourceStack> ctx) {
        String path = StringArgumentType.getString(ctx, "path");
        ForgeConfigSpec.ConfigValue<?> value = valueMap().get(path);
        if (value == null) {
            return unknownPath(ctx, path);
        }

        Object oldValue = value.get();
        setRaw(value, value.getDefault());
        Config.SPEC.save();
        ModConfigCache.refresh();
        ConfigSyncHandler.broadcastAll(ctx.getSource().getServer());

        Object newValue = value.get();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.config.reset",
                Component.literal(path).withStyle(ChatFormatting.AQUA),
                Component.literal(String.valueOf(oldValue)).withStyle(ChatFormatting.GRAY),
                Component.literal(String.valueOf(newValue)).withStyle(ChatFormatting.YELLOW)
        ), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx, String filter) {
        String needle = filter.toLowerCase();
        List<String> matched = new ArrayList<>();
        for (Map.Entry<String, ForgeConfigSpec.ConfigValue<?>> entry : valueMap().entrySet()) {
            if (needle.isEmpty() || entry.getKey().toLowerCase().contains(needle)) {
                matched.add(entry.getKey() + " = " + entry.getValue().get());
            }
        }

        int limit = 20;
        int shown = Math.min(limit, matched.size());
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.config.list.header",
                Component.literal(String.valueOf(matched.size())),
                Component.literal(needle.isEmpty() ? "*" : needle)), false);

        for (int i = 0; i < shown; i++) {
            String line = matched.get(i);
            ctx.getSource().sendSuccess(() -> Component.literal("  " + line).withStyle(ChatFormatting.GRAY), false);
        }
        if (matched.size() > shown) {
            int rest = matched.size() - shown;
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.config.list.more", Component.literal(String.valueOf(rest))), false);
        }
        return matched.size();
    }

    // ===== 工具 =====

    private static int unknownPath(CommandContext<CommandSourceStack> ctx, String path) {
        ctx.getSource().sendFailure(Component.translatable(
                "commands.kogasa_addiction.config.error.unknown_path", Component.literal(path)));
        return 0;
    }

    /** 按默认值的类型解析输入 */
    private static Object parse(Object defaultValue, String raw) {
        String text = raw.trim();
        if (defaultValue instanceof Boolean) {
            if (!text.equalsIgnoreCase("true") && !text.equalsIgnoreCase("false")) {
                throw new IllegalArgumentException("boolean");
            }
            return Boolean.parseBoolean(text);
        }
        if (defaultValue instanceof Integer) {
            return Integer.parseInt(text);
        }
        if (defaultValue instanceof Long) {
            return Long.parseLong(text);
        }
        if (defaultValue instanceof Double) {
            return Double.parseDouble(text);
        }
        if (defaultValue instanceof String) {
            return text;
        }
        if (defaultValue instanceof List<?>) {
            List<String> parts = new ArrayList<>();
            for (String part : text.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    parts.add(trimmed);
                }
            }
            return parts;
        }
        throw new IllegalArgumentException("unsupported");
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void setRaw(ForgeConfigSpec.ConfigValue<?> value, Object newValue) {
        ((ForgeConfigSpec.ConfigValue) value).set(newValue);
    }

    /** "路径 → 句柄"表（{@link ConfigEntries} 里统一维护） */
    private static Map<String, ForgeConfigSpec.ConfigValue<?>> valueMap() {
        return ConfigEntries.values();
    }
}
