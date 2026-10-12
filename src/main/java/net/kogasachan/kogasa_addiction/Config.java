package net.kogasachan.kogasa_addiction;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue SHOW_MINING_SPEED = BUILDER
            .comment("Show current mining speed in tooltips for digger tools.")
            .comment("在挖掘工具的 Tooltip 中显示当前挖掘速度。")
            .define("showMiningSpeed", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_BLAST_FURNACE_INGOT = BUILDER
            .comment("Enable blast furnace to produce special ingots while burning.")
            .comment("允许伞之锭从燃烧的高炉中掉落。")
            .define("enableBlastFurnaceIngot", true);

    public static final ForgeConfigSpec.DoubleValue BLAST_FURNACE_INGOT_CHANCE = BUILDER
            .comment("Chance per burning tick to produce an ingot. 0.01 = 1%.")
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
            .defineInRange("maxGrowth", 1.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_EXTRA_SLOTS = BUILDER
            .comment("Unlock extra reinforcement slots through growth.")
            .comment("通过成长解锁额外强化槽位。")
            .define("enableExtraSlots", true);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_EXTRA_SLOT = BUILDER
            .comment("Growth required to unlock one extra reinforcement slot. 0.01 = 1% growth.")
            .comment("解锁一个额外强化槽所需的成长值。0.01 = 1%。")
            .defineInRange("growthPerExtraSlot", 0.01, 0.0001, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue MAX_EXTRA_SLOTS = BUILDER
            .comment("Maximum extra reinforcement slots unlocked by growth.")
            .comment("通过成长解锁的最大额外强化槽数。")
            .defineInRange("maxExtraSlots", 3, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue WEAPON_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for weapons and tools.")
            .comment("工具吸收强化倍率。")
            .defineInRange("weaponAbsorbedRatio", 1.0, 0.0, 10.0);

    public static final ForgeConfigSpec.DoubleValue ARMOR_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for armor.")
            .comment("护甲吸收强化倍率。")
            .defineInRange("armorAbsorbedRatio", 1.0, 0.0, 10.0);

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
            .defineInRange("maxGrowthEnchants", 5, 0, Integer.MAX_VALUE);

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

    public static final ForgeConfigSpec.DoubleValue REINFORCEMENT_MULTIPLIER_PER_LEVEL = BUILDER
            .comment("Multiplier bonus per reinforcement. 0.1 = +10% per level.")
            .comment("每次强化增加的倍率。0.1 = 每级 +10%。")
            .defineInRange("reinforcementMultiplierPerLevel", 0.1, 0.0, Double.MAX_VALUE);

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

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
