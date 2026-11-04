package net.kogasachan.kogasa_addiction_compat.compat;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 兼容附属配置。所有开关默认关闭/保守，避免影响未测试的整合包。
 */
public final class CompatConfig {

    /** 配置文件版本号：拼进文件名（kogasa_addiction_compat-common-<版本>.toml），目前 0 */
    public static final String CONFIG_VERSION = "0";

    public static final ForgeConfigSpec SPEC;

    /** 兼容总开关：关闭时本模组机制不会作用于任何非本模组物品 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_COMPAT;

    /** 彩虹水晶丢进熔岩/熔岩炼药锅后转化能力锭的开关 */
    public static final ForgeConfigSpec.BooleanValue RAINBOW_LAVA_ENABLED;

    /** 转化概率（0~1），默认 1.0 */
    public static final ForgeConfigSpec.DoubleValue RAINBOW_LAVA_CHANCE;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("compat");
        ENABLE_COMPAT = builder
                .comment(
                        "Master switch for the compatibility layer.",
                        "When disabled, Kogasa mechanics never apply to non-Kogasa items.",
                        "兼容总开关。默认开启；关闭时本模组机制不会作用于任何非本模组物品。")
                .define("enableCompatEquipment", true);
        builder.pop();

        builder.push("rainbow");
        RAINBOW_LAVA_ENABLED = builder
                .comment(
                        "Convert a rainbow crystal dropped into lava (or a lava cauldron) into a random ability ingot.",
                        "彩虹水晶丢进熔岩（或熔岩炼药锅）时，是否转化为随机一种能力锭。")
                .define("lavaConversionEnabled", true);
        RAINBOW_LAVA_CHANCE = builder
                .comment(
                        "Chance of the lava conversion, 0~1. 1.0 = always.",
                        "熔岩转化概率，0~1。1.0 表示必定转化。")
                .defineInRange("lavaConversionChance", 1.0D, 0.0D, 1.0D);
        builder.pop();

        SPEC = builder.build();
    }

    private CompatConfig() {
    }
}
