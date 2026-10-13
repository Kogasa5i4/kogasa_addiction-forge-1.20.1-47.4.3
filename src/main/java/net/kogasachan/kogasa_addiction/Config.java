package net.kogasachan.kogasa_addiction;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_MINING_SPEED = BUILDER
            .comment("Show current mining speed in tooltips for digger tools.")
            .comment("在挖掘工具的工具提示中显示当前挖掘速度。")
            .define("showMiningSpeed", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_KOGASA_INGOT = BUILDER
            .comment("Enable blast furnace to produce kogasa ingots while burning.")
            .comment("允许伞之锭从燃烧的高炉中掉落。")
            .define("enableBlastFurnaceIngot", true);

    public static final ForgeConfigSpec.DoubleValue KOGASA_INGOT_CHANCE = BUILDER
            .comment("Chance per burning tick to produce a kogasa ingot. 0.01 = 1%.")
            .comment("伞之锭掉落的概率，0.01 = 1%。")
            .defineInRange("blastFurnaceIngotChance", 0.000005, 0.0, 1.0);

    public static final ForgeConfigSpec.BooleanValue ENABLE_KOGASA_EQUIPMENT = BUILDER
            .comment("Enable Kogasa equipment system.")
            .comment("启用装备强化系统。")
            .define("enableKogasaEquipment", true);

    public static final ForgeConfigSpec.IntValue MAX_REINFORCEMENTS = BUILDER
            .comment("Maximum reinforcement count.")
            .comment("最大强化数。")
            .defineInRange("maxReinforcements", 3, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue REINFORCEMENT_COST = BUILDER
            .comment("Experience level cost per reinforcement.")
            .comment("强化消耗的经验等级。")
            .defineInRange("reinforcementCost", 1, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_GROWTH = BUILDER
            .comment("Enable growth.")
            .comment("启用成长。")
            .define("enableGrowth", true);

    public static final ForgeConfigSpec.IntValue GROWTH_UNLOCK_REINFORCEMENTS = BUILDER
            .comment("Reinforcement count required to unlock growth. Can be lower than maxReinforcements.")
            .comment("解锁成长所需的强化数。可以低于最大强化数。")
            .defineInRange("growthUnlockReinforcements", 3, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_USE = BUILDER
            .comment("Growth gained per correct use. 0.0001 = 0.01%.")
            .comment("每次成长获得的百分比加成。0.0001 = 0.01%.")
            .defineInRange("growthPerUse", 0.0001, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MAX_GROWTH = BUILDER
            .comment("Maximum growth. 1.0 = 100%.")
            .comment("最大成长幅度。1.0 = 100%.")
            .defineInRange("maxGrowth", 3, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_EXTRA_SLOTS = BUILDER
            .comment("Unlock extra reinforcement slots through growth.")
            .comment("通过成长解锁额外强化槽位。")
            .define("enableExtraSlots", true);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_EXTRA_SLOT = BUILDER
            .comment("Growth required to unlock one extra reinforcement slot. 0.01 = 1% growth.")
            .comment("解锁一个额外强化槽所需的成长值。0.01 = 1%。")
            .defineInRange("growthPerExtraSlot", 0.1, 0.0001, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue MAX_EXTRA_SLOTS = BUILDER
            .comment("Maximum extra reinforcement slots unlocked by growth.")
            .comment("通过成长解锁的最大额外强化槽数。")
            .defineInRange("maxExtraSlots", 4, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue WEAPON_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for weapons and tools.")
            .comment("工具吸收强化倍率。")
            .defineInRange("weaponAbsorbedRatio", 0.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ARMOR_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for armor.")
            .comment("护甲吸收强化倍率。")
            .defineInRange("armorAbsorbedRatio", 0.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_GROWTH_ENCHANT = BUILDER
            .comment("Unlock random enchantments when growth reaches thresholds.")
            .comment("成长值达到阈值时解锁随机附魔。")
            .define("enableGrowthEnchant", true);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_ENCHANT = BUILDER
            .comment("Growth required to unlock one random enchantment. 0.01 = 1% growth per enchant.")
            .comment("解锁一个随机附魔所需的成长值。0.01 = 1%。")
            .defineInRange("growthPerEnchant", 0.01, 0.0001, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue MAX_GROWTH_ENCHANTS = BUILDER
            .comment("Maximum number of enchantments granted by growth.")
            .comment("通过成长获得的最大附魔数量。")
            .defineInRange("maxGrowthEnchants", 100, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ALLOW_MAX_LEVEL_ENCHANTS = BUILDER
            .comment("If true, granted enchantments get a random level from 1 to max. If false, always level 1.")
            .comment("为 true 时附魔等级随机 1~上限；为 false 时固定 1 级。")
            .define("allowMaxLevelEnchants", true);

    public static final ForgeConfigSpec.IntValue DURABILITY_FIXED_PER_REINFORCEMENT = BUILDER
            .comment("Fixed durability added per reinforcement.")
            .comment("每次强化增加的固定耐久值")
            .defineInRange("durabilityFixedPerReinforcement", 500, 0, 10000);

    public static final ForgeConfigSpec.DoubleValue DURABILITY_RATIO_PER_REINFORCEMENT = BUILDER
            .comment("Durability ratio added per reinforcement. 0.1 = +10% of base.")
            .comment("每次强化因强化次数而增加的耐久值比例。0.1 = +10%。")
            .defineInRange("durabilityRatioPerReinforcement", 0.2, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MINING_SPEED_RATIO_PER_REINFORCEMENT = BUILDER
            .comment("Mining speed ratio added per reinforcement. 0.5 = +50% per level.")
            .comment("每次强化增加的挖掘速度倍率。0.5 = 每级 +50%。")
            .defineInRange("miningSpeedRatioPerReinforcement", 1.5, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_BASE_RESISTANCE = BUILDER
            .comment("Knockback resistance at growth = 0. Default 0.05 (half of netherite).")
            .comment("成长值为 0 时的击退抗性。默认 0.05（下界合金的一半）。")
            .defineInRange("knockbackBaseResistance", 0.05, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_MAX_PER_PIECE = BUILDER
            .comment("Asymptotic max knockback resistance per armor piece. " +
                    "0.25 means 4 pieces sum approaches 1.0.")
            .comment("每件护甲的击退抗性渐近上限。0.25 时 4 件总和渐近 1.0。")
            .defineInRange("knockbackMaxPerPiece", 0.25, 0.01, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ATTACK_SPEED_CAP = BUILDER
            .comment("Attack speed cap (bare-hand speed). Default 4.0.")
            .comment("攻速封顶值（空手攻速）。默认 4.0。")
            .defineInRange("attackSpeedCap", 4.0, 1.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER = BUILDER
            .comment("If true, absorbed value is multiplied by reinforcement/growth multiplier. " +
                    "If false, absorbed value is added after multiplication.")
            .comment("为 true 时吸收值参与倍率放大；为 false 时吸收值固定加在倍率之后。")
            .define("absorbedValueIncludedInMultiplier", true);

    public static final ForgeConfigSpec.DoubleValue ATTACK_SPEED_GROWTH_RATIO = BUILDER
            .comment("Attack speed growth ratio. progress = min(growth * ratio, 1.0). " +
                    "1.0 means max growth reaches bare-hand speed (4.0).")
            .comment("攻速成长比例。progress = min(growth * ratio, 1.0). " +
                    "1.0时等同于空手攻速（4.0）。")
            .defineInRange("attackSpeedGrowthRatio", 1.0, 0.01, Double.MAX_VALUE);

    // ===== 能力槽位 =====
    public static final ForgeConfigSpec.IntValue ABILITY_INFUSION_COST = BUILDER
            .comment("Experience level cost to infuse an ability ingot into equipment.")
            .comment("在铁砧中为装备注入能力锭消耗的经验等级。")
            .defineInRange("abilityInfusionCost", 3, 0, 40);

    public static final ForgeConfigSpec.DoubleValue GROWTH_FOR_SECOND_ABILITY_SLOT = BUILDER
            .comment("Growth required to unlock the 2nd ability slot. 0.5 = 50% growth.")
            .comment("解锁第 2 个能力槽所需的成长值。0.5 = 50%。")
            .defineInRange("growthForSecondAbilitySlot", 0.5, 0.0, 1.0);

    // ===== Aya 能力 =====
    public static final ForgeConfigSpec.DoubleValue NEWS_VALUE_PER_RECORD = BUILDER
            .comment("News value gained per new record (mob/block/damage type).")
            .comment("每记录一种新生物/方块/伤害类型获得的新闻值。")
            .defineInRange("newsValuePerRecord", 0.01, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue MAX_NEWS_VALUE = BUILDER
            .comment("Maximum news value.")
            .comment("新闻值上限。")
            .defineInRange("maxNewsValue", 1.0, 0.0, 100.0);

    // ===== Cirno - 攻击缓慢 =====
    public static final ForgeConfigSpec.DoubleValue CIRNO_SLOW_CHANCE = BUILDER
            .comment("Chance to apply Slowness IV to the target when attacking.")
            .comment("攻击时对目标施加缓慢 IV 的概率。")
            .defineInRange("cirnoSlowChance", 0.99, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue CIRNO_SELF_SLOW_CHANCE = BUILDER
            .comment("Chance to apply Slowness to yourself when attacking.")
            .comment("攻击时对自己施加缓慢的概率。")
            .defineInRange("cirnoSelfSlowChance", 0.09, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue CIRNO_SLOW_DURATION = BUILDER
            .comment("Slowness duration applied to the target (in ticks). 200 = 10s.")
            .comment("对目标施加缓慢的持续时间（tick）。200 = 10秒。")
            .defineInRange("cirnoSlowDuration", 200, 1, 72000);

    public static final ForgeConfigSpec.IntValue CIRNO_SLOW_AMPLIFIER = BUILDER
            .comment("Slowness amplifier on target. 3 = Slowness IV.")
            .comment("对目标施加缓慢的等级。3 = 缓慢 IV。")
            .defineInRange("cirnoSlowAmplifier", 3, 0, 10);

    public static final ForgeConfigSpec.IntValue CIRNO_SELF_SLOW_DURATION = BUILDER
            .comment("Slowness duration applied to yourself (in ticks). 100 = 5s.")
            .comment("对自己施加缓慢的持续时间（tick）。100 = 5秒。")
            .defineInRange("cirnoSelfSlowDuration", 100, 1, 72000);

    public static final ForgeConfigSpec.IntValue CIRNO_SELF_SLOW_AMPLIFIER = BUILDER
            .comment("Slowness amplifier on yourself. 0 = Slowness I.")
            .comment("对自己施加缓慢的等级。0 = 缓慢 I。")
            .defineInRange("cirnoSelfSlowAmplifier", 0, 0, 10);

    // ===== Cirno - 复活 =====
    public static final ForgeConfigSpec.BooleanValue CIRNO_REVIVE_ENABLED = BUILDER
            .comment("Enable Cirno ability to revive from durability loss.")
            .comment("启用琪露诺能力：耐久耗尽时消耗成长值复活。")
            .define("cirnoReviveEnabled", true);

    public static final ForgeConfigSpec.DoubleValue CIRNO_REVIVE_GROWTH_COST = BUILDER
            .comment("Growth consumed to revive the item when durability runs out.")
            .comment("耐久耗尽时消耗的成长值以复活装备。")
            .defineInRange("cirnoReviveGrowthCost", 0.9, 0.0, 1.0);

    // ===== Koishi 能力 =====
    public static final ForgeConfigSpec.DoubleValue KOISHI_CRIT_CHANCE = BUILDER
            .comment("Koishi crit chance. 0.75 = 75%.")
            .comment("koishi 暴击概率。0.75 = 75%。")
            .defineInRange("koishiCritChance", 0.75, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue KOISHI_CRIT_MULTIPLIER = BUILDER
            .comment("Koishi crit damage multiplier. 1.5 = 150%.")
            .comment("koishi 暴击伤害倍率。1.5 = 150%。")
            .defineInRange("koishiCritMultiplier", 1.5, 1.0, 10.0);

    // ===== Kogasa 能力 =====
    public static final ForgeConfigSpec.DoubleValue KOGASA_REINFORCEMENT_MULTIPLIER_PER_LEVEL = BUILDER
            .comment("Kogasa reinforcement overall multiplier bonus per reinforcement. 0.1 = +10% per level.")
            .comment("强化装备每次强化增加的总体倍率。0.1 = 每级 +10%。")
            .defineInRange("kogasaReinforcementMultiplierPerLevel", 0.1, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
