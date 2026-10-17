package net.kogasachan.kogasa_addiction;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.push("general");
    }

    public static final ForgeConfigSpec.BooleanValue SHOW_MINING_SPEED = BUILDER
            .comment("Show current mining speed in tooltips for digger tools.\n" +
                    "在挖掘工具的工具提示中显示当前挖掘速度。")
            .define("showMiningSpeed", true);

    public static final ForgeConfigSpec.BooleanValue ENABLE_FIRST_JOIN_REWARD = BUILDER
            .comment("Give players a random Kogasa equipment with a random ability on their first join.\n" +
                    "玩家首次进入世界时获得一件随机 Kogasa 装备，附带随机能力。")
            .define("enableFirstJoinReward", true);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("kogasa_ingot");
    }

    public static final ForgeConfigSpec.BooleanValue ENABLE_KOGASA_INGOT = BUILDER
            .comment("Enable blast furnace to produce kogasa ingots while burning.\n" +
                    "允许伞之锭从燃烧的高炉中掉落。")
            .define("enableKogasaIngot", true);

    public static final ForgeConfigSpec.DoubleValue KOGASA_INGOT_CHANCE = BUILDER
            .comment("Chance per burning tick (1/20 seconds) to produce a kogasa ingot. 0.01 = 1%.\n" +
                    "每游戏刻（1/20秒）伞之锭掉落的概率，0.01 = 1%。")
            .defineInRange("kogasaIngotChance", 0.001, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue KOGASA_INGOT_SEARCH_HEIGHT = BUILDER
            .comment("Maximum upward search height for kogasa ingot drop position.\n" +
                    "伞之锭产出时向上搜索空气的最大高度。")
            .defineInRange("kogasaIngotSearchHeight", 8, 1, 320);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("reinforcement");
    }

    public static final ForgeConfigSpec.BooleanValue ENABLE_KOGASA_EQUIPMENT = BUILDER
            .comment("Enable Kogasa equipment system.\n" +
                    "启用装备强化系统。")
            .define("enableKogasaEquipment", true);

    public static final ForgeConfigSpec.IntValue MAX_REINFORCEMENTS = BUILDER
            .comment("Maximum reinforcement count.\n" +
                    "最大强化数。")
            .defineInRange("maxReinforcements", 3, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue REINFORCEMENT_COST = BUILDER
            .comment("Experience level cost per reinforcement.\n" +
                    "强化消耗的经验等级。")
            .defineInRange("reinforcementCost", 1, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue AMMO_INFUSION_COST = BUILDER
            .comment("Experience level cost to infuse special ammo into an aya bow/crossbow.\n" +
                    "在铁砧中为文之弓/弩装载特殊弹药消耗的经验等级。")
            .defineInRange("ammoInfusionCost", 3, 0, 100);

    public static final ForgeConfigSpec.DoubleValue WEAPON_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for weapons and tools.\n" +
                    "强化时吸收的武器/工具属性比例。")
            .defineInRange("weaponAbsorbedRatio", 0.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ARMOR_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for armor.\n" +
                    "强化时吸收的护甲属性比例。")
            .defineInRange("armorAbsorbedRatio", 0.5, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("growth");
    }

    public static final ForgeConfigSpec.BooleanValue ENABLE_GROWTH = BUILDER
            .comment("Enable growth.\n" +
                    "启用成长。")
            .define("enableGrowth", true);

    public static final ForgeConfigSpec.IntValue GROWTH_UNLOCK_REINFORCEMENTS = BUILDER
            .comment("Reinforcement count required to unlock growth. Can be lower than maxReinforcements.\n" +
                    "解锁成长所需的强化数。可以低于最大强化数。")
            .defineInRange("growthUnlockReinforcements", 3, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_USE = BUILDER
            .comment("Growth gained per correct use. 0.0001 = 0.01%.\n" +
                    "每次成长获得的百分比加成。0.0001 = 0.01%.")
            .defineInRange("growthPerUse", 0.0001, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MAX_GROWTH = BUILDER
            .comment("Maximum growth. 1.0 = 100%.\n" +
                    "最大成长幅度。1.0 = 100%.")
            .defineInRange("maxGrowth", 3, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_EXTRA_SLOTS = BUILDER
            .comment("Unlock extra reinforcement slots through growth.\n" +
                    "通过成长解锁额外强化槽位。")
            .define("enableExtraSlots", true);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_EXTRA_SLOT = BUILDER
            .comment("Growth required to unlock one extra reinforcement slot. 0.01 = 1% growth.")
            .comment("解锁一个额外强化槽所需的成长值。0.01 = 1%。")
            .defineInRange("growthPerExtraSlot", 0.1, 0.0001, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue MAX_EXTRA_SLOTS = BUILDER
            .comment("Maximum extra reinforcement slots unlocked by growth.\n" +
                    "通过成长解锁的最大额外强化槽数。")
            .defineInRange("maxExtraSlots", 4, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_GROWTH_ENCHANT = BUILDER
            .comment("Unlock random enchantments when growth reaches thresholds.\n" +
                    "成长值达到阈值时解锁随机附魔。")
            .define("enableGrowthEnchant", true);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_ENCHANT = BUILDER
            .comment("Growth required to unlock one random enchantment. 0.01 = 1% growth per enchant.\n" +
                    "解锁一个随机附魔所需的成长值。0.01 = 1%。")
            .defineInRange("growthPerEnchant", 0.01, 0.0001, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue MAX_GROWTH_ENCHANTS = BUILDER
            .comment("Maximum number of enchantments granted by growth.\n" +
                    "通过成长获得的最大附魔数量。")
            .defineInRange("maxGrowthEnchants", 256, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ALLOW_MAX_LEVEL_ENCHANTS = BUILDER
            .comment("If true, granted enchantments get a random level from 1 to max. If false, always level 1.\n" +
                    "为 true 时附魔等级随机 1~上限；为 false 时固定 1 级。")
            .define("allowMaxLevelEnchants", true);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("equipment");
    }

    public static final ForgeConfigSpec.BooleanValue ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER = BUILDER
            .comment("If true, absorbed value is multiplied by reinforcement/growth multiplier. " +
                    "If false, absorbed value is added after multiplication.")
            .comment("吸收值是否参与倍率放大")
            .define("absorbedValueIncludedInMultiplier", true);

    public static final ForgeConfigSpec.IntValue DURABILITY_FIXED_PER_REINFORCEMENT = BUILDER
            .comment("Fixed durability added per reinforcement.\n" +
                    "每次强化增加的固定耐久值")
            .defineInRange("durabilityFixedPerReinforcement", 500, 0, 10000);

    public static final ForgeConfigSpec.DoubleValue DURABILITY_RATIO_PER_REINFORCEMENT = BUILDER
            .comment("Durability ratio added per reinforcement. 0.1 = +10% of base.\n" +
                    "每次强化因强化次数而增加的耐久值比例。0.1 = +10%。")
            .defineInRange("durabilityRatioPerReinforcement", 0.2, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.push("tool");
    }

    public static final ForgeConfigSpec.DoubleValue MINING_SPEED_RATIO_PER_REINFORCEMENT = BUILDER
            .comment("Mining speed ratio added per reinforcement. 0.5 = +50% per level.\n" +
                    "每次强化增加的挖掘速度倍率。0.5 = 每级 +50%。")
            .defineInRange("miningSpeedRatioPerReinforcement", 1.5, 0.0, 10.0);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("weapon");
    }

    public static final ForgeConfigSpec.DoubleValue ATTACK_SPEED_CAP = BUILDER
            .comment("Attack speed cap (bare-hand speed). Default 4.0.\n" +
                    "攻速封顶值（空手攻速）。默认 4.0。")
            .defineInRange("attackSpeedCap", 4.0, 1.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ATTACK_SPEED_GROWTH_RATIO = BUILDER
            .comment("""
                    Attack speed growth ratio. progress = min(growth * ratio, 1.0).\s
                    1.0 means max growth reaches bare-hand speed (4.0).\s
                    攻速成长比例。进度为min(成长值 * 此倍率, 1.0).\s
                    1.0时等同于空手攻速（4.0）。""")
            .defineInRange("attackSpeedGrowthRatio", 1.0, 0.01, Double.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("armor");
    }

    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_BASE_RESISTANCE = BUILDER
            .comment("Base knockback resistance. Default 0.05 (half of netherite).\n" +
                    "基础击退抗性。默认 0.05（下界合金的一半）。")
            .defineInRange("knockbackBaseResistance", 0.05, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue KNOCKBACK_MAX_PER_PIECE = BUILDER
            .comment("Asymptotic max knockback resistance per armor piece.\n" +
                    "每件护甲的最大击退抗性（成长值越高越接近这个值）")
            .defineInRange("knockbackMaxPerPiece", 0.25, 0.01, Double.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("ranged");
    }

    public static final ForgeConfigSpec.DoubleValue BOW_PIERCE_GROWTH = BUILDER
            .comment("Growth required for bows/crossbows to gain Pierce I.\n" +
                    "弓弩获得穿透 I 所需的成长值。")
            .defineInRange("bowPierceGrowth", 0.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue BOW_MULTISHOT_2_GROWTH = BUILDER
            .comment("Growth required for 2-arrow multishot.\n" +
                    "解锁双发所需的成长值。")
            .defineInRange("bowMultishot2Growth", 1.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue BOW_MULTISHOT_3_GROWTH = BUILDER
            .comment("Growth required for 3-arrow multishot.\n" +
                    "解锁三发所需的成长值。")
            .defineInRange("bowMultishot3Growth", 2.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue CROSSBOW_AUTO_RELOAD_GROWTH = BUILDER
            .comment("Growth required for crossbow auto-reload. 0 = disabled.\n" +
                    "弩自动装填所需的成长值。0 = 禁用。")
            .defineInRange("crossbowAutoReloadGrowth", 2.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue NO_CONSUME_ARROW_START_GROWTH = BUILDER
            .comment("Growth required to start arrow-saving chance. 1.0 = 100% growth.\n" +
                    "开始概率不消耗箭矢的成长值阈值。1.0 = 100% 成长。")
            .defineInRange("noConsumeArrowStartGrowth", 1.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue NO_CONSUME_ARROW_FULL_GROWTH = BUILDER
            .comment("Growth required for 100% arrow-saving chance. 2.0 = 200% growth.\n" +
                    "达到 100% 不消耗箭矢的成长值。2.0 = 200% 成长。")
            .defineInRange("noConsumeArrowFullGrowth", 2.0, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.pop(2);
    }

    static {
        BUILDER.push("ability");
    }

    // ===== 能力槽位 =====
    public static final ForgeConfigSpec.IntValue ABILITY_INFUSION_COST = BUILDER
            .comment("Experience level cost to infuse an ability ingot into equipment.\n" +
                    "在铁砧中为装备注入能力锭消耗的经验等级。")
            .defineInRange("abilityInfusionCost", 3, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue GROWTH_FOR_SECOND_ABILITY_SLOT = BUILDER
            .comment("Growth required to unlock the 2nd ability slot. 0.5 = 50% growth.\n" +
                    "解锁第 2 个能力槽所需的成长值。0.5 = 50%。")
            .defineInRange("growthForSecondAbilitySlot", 0.5, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.push("aya");
    }

    // ===== Aya 能力 =====
    public static final ForgeConfigSpec.DoubleValue NEWS_VALUE_PER_RECORD = BUILDER
            .comment("News value gained per new record (mob/block/damage type).\n" +
                    "每记录一种新生物/方块/伤害类型获得的新闻值。")
            .defineInRange("newsValuePerRecord", 0.01, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MAX_NEWS_VALUE = BUILDER
            .comment("Maximum news value.\n" +
                    "新闻值上限。")
            .defineInRange("maxNewsValue", 1.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_AYA_INGOT = BUILDER
            .comment("""
                    Enable obtaining Aya Ingot by shooting.\s
                    You can either shoot a screenshot or shoot a bullet.\s
                    是否允许通过进行摄击获取文之锭。\s
                    摄击也就是“拍摄”或“射击”的意思。""")
            .define("enableAyaIngot", true);

    public static final ForgeConfigSpec.DoubleValue SHOOT_INGOT_CHANCE = BUILDER
            .comment("Chance to get an Aya Ingot per shoot. 0.1 = 10%.\n" +
                    "每次进行摄击获得文之锭的概率。0.1 = 10%。")
            .defineInRange("shootIngotChance", 0.1, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue SHOOT_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown in seconds between shoot attempts. 10 = 10s.\n" +
                    "两次摄击之间的冷却（秒）。10 = 10秒。")
            .defineInRange("shootCooldownSeconds", 10, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("cirno");
    }

    // ===== Cirno - 攻击缓慢 =====
    public static final ForgeConfigSpec.DoubleValue CIRNO_SLOW_CHANCE = BUILDER
            .comment("Chance to apply Slowness to the target when attacking.\n" +
                    "攻击时对目标施加缓慢的概率。")
            .defineInRange("cirnoSlowChance", 0.99, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue CIRNO_SELF_SLOW_CHANCE = BUILDER
            .comment("Chance to apply Slowness to yourself when attacking.\n" +
                    "攻击时对自己施加缓慢的概率。")
            .defineInRange("cirnoSelfSlowChance", 0.09, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue CIRNO_SLOW_DURATION = BUILDER
            .comment("Slowness duration applied to the target (in seconds). 10 = 10s.\n" +
                    "对目标施加缓慢的持续时间（秒）。10 = 10秒。")
            .defineInRange("cirnoSlowDuration", 10, 1, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue CIRNO_SLOW_LEVEL = BUILDER
            .comment("Slowness level applied to the target. 1 = Slowness I, 4 = Slowness IV.\n" +
                    "对目标施加的缓慢等级。1 = 缓慢 I，4 = 缓慢 IV。")
            .defineInRange("cirnoSlowLevel", 4, 1, 10);

    public static final ForgeConfigSpec.DoubleValue CIRNO_SELF_SLOW_DURATION = BUILDER
            .comment("Slowness duration applied to yourself (in seconds). 5 = 5s.\n" +
                    "对自己施加缓慢的持续时间（秒）。5 = 5秒。")
            .defineInRange("cirnoSelfSlowDuration", 5, 1, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue CIRNO_SELF_SLOW_LEVEL = BUILDER
            .comment("Slowness level applied to yourself. 1 = Slowness I.\n" +
                    "对自己施加的缓慢等级。1 = 缓慢 I。")
            .defineInRange("cirnoSelfSlowLevel", 1, 1, 10);

    public static final ForgeConfigSpec.DoubleValue CIRNO_INGOT_DROP_CHANCE = BUILDER
            .comment("Chance to get a Cirno Ingot when harvesting ice without Silk Touch. 0.09 = 9%.\n" +
                    "非精准采集采掘冰时掉落琪露诺锭的概率。0.09 = 9%。")
            .defineInRange("cirnoIngotDropChance", 0.09, 0.0, 1.0);

    // ===== Cirno - 复活 =====
    public static final ForgeConfigSpec.BooleanValue CIRNO_REVIVE_ENABLED = BUILDER
            .comment("Enable Cirno ability to revive from durability loss.\n" +
                    "启用琪露诺能力：耐久耗尽时消耗成长值复活。")
            .define("cirnoReviveEnabled", true);

    public static final ForgeConfigSpec.DoubleValue CIRNO_REVIVE_GROWTH_COST = BUILDER
            .comment("Growth consumed to revive the item when durability runs out.\n" +
                    "耐久耗尽时消耗以复活装备的成长值。")
            .defineInRange("cirnoReviveGrowthCost", 0.9, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("koishi");
    }

    // ===== Koishi 能力 =====
    public static final ForgeConfigSpec.DoubleValue KOISHI_CRIT_CHANCE = BUILDER
            .comment("Koishi crit chance. 0.75 = 75%.\n" +
                    "koishi 暴击概率。0.75 = 75%。")
            .defineInRange("koishiCritChance", 0.75, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue KOISHI_CRIT_MULTIPLIER = BUILDER
            .comment("Koishi crit damage multiplier. 1.5 = 150%.\n" +
                    "koishi 暴击伤害倍率。1.5 = 150%。")
            .defineInRange("koishiCritMultiplier", 1.5, 1.0, 10.0);

    public static final ForgeConfigSpec.IntValue KOISHI_GHAST_KILLS_REQUIRED = BUILDER
            .comment("Number of Ghasts killed by reflected fireballs to earn one Koishi Ingot.\n" +
                    "反弹火球击杀多少个恶魂获得一个恋之锭。")
            .defineInRange("koishiGhastKillsRequired", 2, 1, 100);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("kogasa");
    }

    // ===== Kogasa 能力 =====
    public static final ForgeConfigSpec.DoubleValue KOGASA_REINFORCEMENT_MULTIPLIER_PER_LEVEL = BUILDER
            .comment("Kogasa reinforcement overall multiplier bonus per reinforcement. 0.1 = +10% per level.\n" +
                    "强化装备每次强化增加的总体倍率。0.1 = 每级 +10%。")
            .defineInRange("kogasaReinforcementMultiplierPerLevel", 0.1, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.pop(2);
    }

    // ============================================================
    // 远程武器
    // ============================================================
    static {
        BUILDER.push("ranged");
    }

    // ===== 通用 =====
    public static final ForgeConfigSpec.DoubleValue RANGED_MULTISHOT_SPREAD = BUILDER
            .comment("Multishot spread angle in degrees. 8 = ±4° for 3 arrows.\n" +
                    "多发散射角度（度）。8 = 三发时 ±4°。")
            .defineInRange("multishotSpread", 8.0, 0.0, 45.0);

    static {
        BUILDER.push("aya");
    }

    // ===== AYA 远程能力 =====
    public static final ForgeConfigSpec.BooleanValue ENABLE_AYA_RANGED = BUILDER
            .comment("Enable Aya ranged ability (Speed + Haste on projectile hit).\n" +
                    "启用文文远程能力（命中时给射手迅捷 + 急迫）。")
            .define("enableAyaRanged", true);

    public static final ForgeConfigSpec.IntValue AYA_SPEED_DURATION_SECONDS = BUILDER
            .comment("Speed effect duration in seconds. 3 = 3s.\n" +
                    "迅捷持续时间（秒）。3 = 3秒。")
            .defineInRange("ayaSpeedDurationSeconds", 3, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue AYA_SPEED_LEVEL = BUILDER
            .comment("Speed effect level shown to player. 1 = Speed I.\n" +
                    "迅捷效果等级。1 = 迅捷 I。")
            .defineInRange("ayaSpeedLevel", 1, 1, 10);

    public static final ForgeConfigSpec.IntValue AYA_HASTE_DURATION_SECONDS = BUILDER
            .comment("Haste effect duration in seconds. 3 = 3s.\n" +
                    "急迫持续时间（秒）。3 = 3秒。")
            .defineInRange("ayaHasteDurationSeconds", 3, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue AYA_HASTE_LEVEL = BUILDER
            .comment("Haste effect level shown to player. 1 = Haste I.\n" +
                    "急迫效果等级。1 = 急迫 I。")
            .defineInRange("ayaHasteLevel", 1, 1, 10);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("cirno");
    }

    // ===== CIRNO 远程能力 =====
    public static final ForgeConfigSpec.BooleanValue ENABLE_CIRNO_RANGED = BUILDER
            .comment("Enable Cirno ranged ability (enhanced potions + Slowness on plain arrows).\n" +
                    "启用琪露诺远程能力（强化药水 + 无药水箭附加缓慢）。")
            .define("enableCirnoRanged", true);

    public static final ForgeConfigSpec.IntValue CIRNO_POTION_LEVEL_BONUS = BUILDER
            .comment("Bonus potion amplifier levels from Cirno ability. 2 = +2 levels.\n" +
                    "琪露诺对药水等级的加成。2 = +2 级。")
            .defineInRange("cirnoPotionLevelBonus", 2, 0, 10);

    public static final ForgeConfigSpec.DoubleValue CIRNO_POTION_DURATION_MULTIPLIER = BUILDER
            .comment("Potion duration multiplier from Cirno ability. 1.5 = 150%.\n" +
                    "琪露诺对药水持续时间的倍率。1.5 = 150%。")
            .defineInRange("cirnoPotionDurationMultiplier", 1.5, 1.0, 10.0);

    public static final ForgeConfigSpec.IntValue CIRNO_SLOWNESS_DURATION_TICKS = BUILDER
            .comment("Slowness duration on plain arrow hit. 600 = 30s.\n" +
                    "无药水箭命中时对目标施加的缓慢时长（tick）。600 = 30秒。")
            .defineInRange("cirnoSlownessDurationTicks", 600, 1, 72000);

    public static final ForgeConfigSpec.IntValue CIRNO_SLOWNESS_LEVEL = BUILDER
            .comment("Slowness level on plain arrow hit. 5 = Slowness V.\n" +
                    "无药水箭命中时对目标施加的缓慢等级。5 = 缓慢 V。")
            .defineInRange("cirnoSlownessLevel", 5, 1, 256);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("kogasa");
    }

    // ===== KOGASA 远程能力 =====
    public static final ForgeConfigSpec.BooleanValue ENABLE_KOGASA_RANGED = BUILDER
            .comment("Enable Kogasa ranged ability (consecutive hit damage bonus).\n" +
                    "启用小伞远程能力（连续命中伤害加成）。")
            .define("enableKogasaRanged", true);

    public static final ForgeConfigSpec.IntValue KOGASA_STREAK_RESET_SECONDS = BUILDER
            .comment("Time window for consecutive hits in seconds. 3 = 3s.\n" +
                    "连续命中的判定窗口（秒）。3 = 3秒内命中算连续。")
            .defineInRange("kogasaStreakResetSeconds", 3, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue KOGASA_STREAK_BONUS = BUILDER
            .comment("Damage bonus per consecutive hit. 0.1 = +10% per hit.\n" +
                    "每次连续命中的伤害加成。0.1 = 每次 +10%。")
            .defineInRange("kogasaStreakBonus", 0.1, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue KOGASA_STREAK_MAX = BUILDER
            .comment("Maximum streak count. 0 = unlimited.\n" +
                    "连续命中的最大层数。0 = 无上限。")
            .defineInRange("kogasaStreakMax", 10, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("koishi");
    }

    // ===== KOISHI 远程能力 =====
    public static final ForgeConfigSpec.BooleanValue ENABLE_KOISHI_RANGED = BUILDER
            .comment("Enable Koishi ranged ability (armor-piercing crit).\n" +
                    "启用恋恋远程能力（穿甲暴击）。")
            .define("enableKoishiRanged", true);

    public static final ForgeConfigSpec.DoubleValue KOISHI_RANGED_CRIT_CHANCE = BUILDER
            .comment("Chance for armor-piercing crit. 0.75 = 75%.\n" +
                    "穿甲暴击概率。0.75 = 75%。")
            .defineInRange("koishiRangedCritChance", 0.75, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue KOISHI_RANGED_CRIT_MULTIPLIER = BUILDER
            .comment("Damage multiplier on crit. 1.5 = 150%.\n" +
                    "暴击伤害倍率。1.5 = 150%。")
            .defineInRange("koishiRangedCritMultiplier", 1.5, 1.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue KOISHI_PROJECTILE_IMMUNE_MULTIPLIER = BUILDER
            .comment("Damage multiplier against projectile-immune targets. 3.0 = 300%.\n" +
                    "对弹射物免疫目标的伤害倍率。3.0 = 300%。")
            .defineInRange("koishiProjectileImmuneMultiplier", 3.0, 1.0, Double.MAX_VALUE);

    static {
        BUILDER.pop(2);
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
