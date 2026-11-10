package net.kogasachan.kogasa_addiction_compat.compat;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

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

    /** 原版含钻石宝箱额外掉落彩虹水晶的开关 */
    public static final ForgeConfigSpec.BooleanValue RAINBOW_CHEST_LOOT_ENABLED;

    /** 原版含钻石宝箱额外掉落彩虹水晶的概率（0~1） */
    public static final ForgeConfigSpec.DoubleValue RAINBOW_CHEST_LOOT_CHANCE;

    /** Boss 行为修正总开关（bossTweaks 模块）。 */
    public static final ForgeConfigSpec.BooleanValue ENABLE_BOSS_TWEAKS;

    /** 亚波伦 Doom：阻止没收护甲/Curios。 */
    public static final ForgeConfigSpec.BooleanValue APOLLYON_PREVENT_ITEM_THEFT;

    /** 亚波伦 Doom：阻止 alwaysBase 属性模式。 */
    public static final ForgeConfigSpec.BooleanValue APOLLYON_PREVENT_ATTRIBUTE_RESET;

    /** 亚波伦 Doom：阻止长物品冷却。 */
    public static final ForgeConfigSpec.BooleanValue APOLLYON_PREVENT_COOLDOWN;

    /** YH YoukaiEntity：取消 based-on-CD 的伤害免疫等级。 */
    public static final ForgeConfigSpec.BooleanValue YH_PREVENT_DAMAGE_IMMUNITY;

    /** YH 普通伤害上限（最大生命比例，默认 0.05）。 */
    public static final ForgeConfigSpec.DoubleValue YH_NORMAL_DAMAGE_CAP;

    /** YH 弹幕伤害上限（最大生命比例，默认 0.20）。 */
    public static final ForgeConfigSpec.DoubleValue YH_DANMAKU_DAMAGE_CAP;

    /** YH：setHealth 不再进入 illegalDamage/flag32/负血量状态。 */
    public static final ForgeConfigSpec.BooleanValue YH_PREVENT_SET_HEALTH_STATE;

    /** 亚波伦：取消 moddedInvul/obsidianInvul（Doom 尾杀阶段除外）。 */
    public static final ForgeConfigSpec.BooleanValue APOLLYON_PREVENT_INVULNERABILITY;

    /** UOM：取消 BaseEntity 出生无敌与 m_6673_ 免疫。 */
    public static final ForgeConfigSpec.BooleanValue UOM_PREVENT_INVULNERABILITY;

    /** YH：取消 YoukaiEntity/Cirno/Rumia 的 m_6673_ 伤害免疫。 */
    public static final ForgeConfigSpec.BooleanValue YH_PREVENT_ENTITY_INVULNERABILITY;

    /** 亚波伦/使徒：解除对附近玩家飞行的强制关闭。 */
    public static final ForgeConfigSpec.BooleanValue APOLLYON_PREVENT_FLIGHT_BAN;

    /** YH：把 onDanmakuImmune 里直接 setHealth 改为等额 generic.kill 伤害。 */
    public static final ForgeConfigSpec.BooleanValue YH_REPLACE_DANMAKU_SET_HEALTH;

    /** 没有原版工具/武器基类、但需要按“通用装备”兼容的物品（支持 * 通配符） */
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GENERIC_EQUIPMENT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.push("compat");
        ENABLE_COMPAT = builder
                .comment(
                        "Master switch for the compatibility layer.",
                        "When disabled, Kogasa mechanics never apply to non-Kogasa items.",
                        "兼容总开关。默认开启；关闭时本模组机制不会作用于任何非本模组物品。")
                .define("enableCompatEquipment", true);

        GENERIC_EQUIPMENT = builder
                .comment("Items whose class does not extend SwordItem/DiggerItem/TridentItem/etc. but should still be treated as generic equipment when combined with a rainbow crystal.\n" +
                        "Default: cataclysm:* (Cataclysm weapons extend plain Item).\n" +
                        "Only items with attack attributes, mining speed, or max damage > 0 are eligible; armor is excluded.\n" +
                        "类没有继承原版武器/工具基类、但需要按通用装备兼容的物品。支持 * 通配符。\n" +
                        "默认 cataclysm:*（灾变武器直接继承 Item）。\n" +
                        "仅攻击属性/挖掘速度/有耐久度的物品生效，护甲除外。")
                .defineListAllowEmpty("genericEquipment",
                        List.of("cataclysm:*"),
                        o -> o instanceof String s && !s.isBlank());
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
        RAINBOW_CHEST_LOOT_ENABLED = builder
                .comment(
                        "Add a bonus rainbow crystal drop to vanilla chest loot tables that contain diamonds.",
                        "在含有钻石的原版宝箱战利品中额外加入彩虹水晶掉落。")
                .define("chestLootEnabled", true);
        RAINBOW_CHEST_LOOT_CHANCE = builder
                .comment(
                        "Chance of the bonus rainbow crystal, 0~1. Default 0.5 = 50%.",
                        "额外彩虹水晶的掉落概率，0~1，默认 0.5 = 50%。")
                .defineInRange("chestLootChance", 0.5D, 0.0D, 1.0D);
        builder.pop();

        builder.push("bossTweaks");
        ENABLE_BOSS_TWEAKS = builder
                .comment("Master switch for boss behavior tweaks added by this addon.",
                        "Boss 行为修正总开关。默认开启。")
                .define("enableBossTweaks", true);
        APOLLYON_PREVENT_ITEM_THEFT = builder
                .comment("Apollyon Doom: prevent confiscating armor and Curios.",
                        "亚波伦 Doom 尾杀：阻止夺取护甲和饰品。")
                .define("apollyonPreventItemTheft", true);
        APOLLYON_PREVENT_ATTRIBUTE_RESET = builder
                .comment("Apollyon Doom: prevent forcing attributes to base values.",
                        "亚波伦 Doom 尾杀：阻止强制按初始属性计算。")
                .define("apollyonPreventAttributeReset", true);
        APOLLYON_PREVENT_COOLDOWN = builder
                .comment("Apollyon Doom: prevent adding the long item cooldown.",
                        "亚波伦 Doom 尾杀：阻止添加长物品冷却。")
                .define("apollyonPreventCooldown", true);
        YH_PREVENT_DAMAGE_IMMUNITY = builder
                .comment("YH YoukaiEntity: remove the getCD/hurtCD damage-immunity tiers.",
                        "YH YoukaiEntity：取消 getCD/hurtCD 的伤害免疫等级制。")
                .define("yhPreventDamageImmunity", true);
        YH_NORMAL_DAMAGE_CAP = builder
                .comment("YH normal damage cap as a fraction of max health. Stricter built-in caps are relaxed to this; wider caps are kept.",
                        "YH 普通伤害上限（最大生命比例）；原本更小的限制保持不变。")
                .defineInRange("yhNormalDamageCap", 0.05D, 0.0D, 1.0D);
        YH_DANMAKU_DAMAGE_CAP = builder
                .comment("YH danmaku damage cap as a fraction of max health. Stricter built-in caps are relaxed to this; wider caps are kept.",
                        "YH 弹幕伤害上限（最大生命比例）；原本更小的限制保持不变。")
                .defineInRange("yhDanmakuDamageCap", 0.20D, 0.0D, 1.0D);
        YH_PREVENT_SET_HEALTH_STATE = builder
                .comment("YH: clamp setHealth to [0, maxHealth] without illegalDamage/flag32 state.",
                        "YH：setHealth 夹到 [0,最大生命]，且不进入 illegalDamage/flag32 特殊状态。")
                .define("yhPreventSetHealthState", true);
        APOLLYON_PREVENT_INVULNERABILITY = builder
                .comment("Apollyon: clear modded/obsidian invulnerability (Doom tail phase excluded).",
                        "亚波伦：清除 modded/obsidian 无敌（Doom 尾杀阶段除外）。")
                .define("apollyonPreventInvulnerability", true);
        APOLLYON_PREVENT_FLIGHT_BAN = builder
                .comment("Apollyon/Apostle: stop forcing nearby players' flying ability off.",
                        "亚波伦/使徒：不再强制关闭附近玩家的飞行。")
                .define("apollyonPreventFlightBan", true);
        UOM_PREVENT_INVULNERABILITY = builder
                .comment("UOM: clear BaseEntity spawn invulnerability and m_6673_ immunity.",
                        "UOM：清除 BaseEntity 出生无敌与 m_6673_ 免疫。")
                .define("uomPreventInvulnerability", true);
        YH_PREVENT_ENTITY_INVULNERABILITY = builder
                .comment("YH: clear YoukaiEntity/Cirno/Rumia m_6673_ damage immunity.",
                        "YH：清除 YoukaiEntity/Cirno/Rumia 的 m_6673_ 伤害免疫。")
                .define("yhPreventEntityInvulnerability", true);
        YH_REPLACE_DANMAKU_SET_HEALTH = builder
                .comment("YH: replace Reimu/Yukari onDanmakuImmune setHealth with equal generic.kill damage.",
                        "YH：灵梦/紫的弹幕免疫 onDanmakuImmune 不再直接 setHealth，改为等额 generic.kill 伤害。")
                .define("yhReplaceDanmakuSetHealth", true);
        builder.pop();

        SPEC = builder.build();
    }

    private CompatConfig() {
    }
}
