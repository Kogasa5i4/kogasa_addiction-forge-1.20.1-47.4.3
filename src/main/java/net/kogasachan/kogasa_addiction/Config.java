package net.kogasachan.kogasa_addiction;

import net.minecraftforge.common.ForgeConfigSpec;

import java.util.List;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    static {
        BUILDER.push("general");
    }

    public static final ForgeConfigSpec.IntValue MINING_SPEED_TOOLTIP_MODE = BUILDER
            .comment("Mining speed tooltip mode. 0 = disabled, 1 = all digger tools, 2 = Kogasa equipment only.\n" +
                    "挖掘速度 Tooltip 模式。0 = 不显示，1 = 所有挖掘工具，2 = 仅本模组装备。")
            .defineInRange("miningSpeedTooltipMode", 1, 0, 2);

    public static final ForgeConfigSpec.IntValue RANGED_STATS_TOOLTIP_MODE = BUILDER
            .comment("Draw time / projectile velocity tooltip mode. 0 = off, 1 = all bows/crossbows/tridents, 2 = mod equipment only.\n" +
                    "蓄力时间/弹射物速度 Tooltip 模式。0 = 不显示，1 = 所有弓弩三叉戟，2 = 仅本模组装备。")
            .defineInRange("rangedStatsTooltipMode", 1, 0, 2);

    public static final ForgeConfigSpec.BooleanValue ENABLE_FIRST_JOIN_REWARD = BUILDER
            .comment("Give players a random piece of mod equipment with a random ability on their first join.\n" +
                    "玩家首次进入世界时获得一件随机模组装备，附带随机能力。")
            .define("enableFirstJoinReward", true);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("loot");
    }

    /**
     * 战利品开出的"封印装备"：封印期间属性 -50% 且能力不生效，
     * 成长达到该值时自动解封（恢复全力 + 结算成长附魔）。
     */
    public static final ForgeConfigSpec.DoubleValue LOCKED_EQUIPMENT_UNLOCK_GROWTH = BUILDER
            .comment("Growth value at which loot \"sealed\" equipment automatically unlocks.\n" +
                    "Sealed equipment has -50% attributes and inactive abilities until then.\n" +
                    "战利品开出的封印装备在封印期间属性 -50% 且能力不生效，成长达到该值时自动解封。")
            .defineInRange("lockedEquipmentUnlockGrowth", 0.5, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("mobEquipment");
    }

    public static final ForgeConfigSpec.BooleanValue MOB_EQUIPMENT_ENABLED = BUILDER
            .comment("Whether monsters can spawn holding/wearing mod equipment.\n" +
                    "怪物生成时是否可能持有本模组装备。")
            .define("enabled", true);

    public static final ForgeConfigSpec.DoubleValue MOB_EQUIPMENT_CHANCE = BUILDER
            .comment("Chance for an eligible monster to get mod equipment. 0.05 = 5%.\n" +
                    "符合条件的怪物获得本模组装备的概率。0.05 = 5%。")
            .defineInRange("chance", 0.05, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue MOB_EQUIPMENT_SEALED_CHANCE = BUILDER
            .comment("Chance that the granted equipment is a sealed piece (random ability, locked until growth).\n" +
                    "获得的装备为封印装备（随机能力，成长解封）的概率。")
            .defineInRange("sealedChance", 0.5, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue MOB_EQUIPMENT_DROP_CHANCE = BUILDER
            .comment("Drop chance for mob equipment on death. Vanilla default is 0.085.\n" +
                    "怪物死亡时装备的掉落率。原版默认 0.085。")
            .defineInRange("dropChance", 0.085, 0.0, 1.0);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("villagerTrades");
    }

    public static final ForgeConfigSpec.BooleanValue VILLAGER_TRADES_ENABLED = BUILDER
            .comment("Whether villagers and the wandering trader offer mod equipment trades.\n" +
                    "村民与流浪商人是否出售本模组装备。")
            .define("enabled", true);

    public static final ForgeConfigSpec.DoubleValue VILLAGER_TRADE_PRICE_MULTIPLIER = BUILDER
            .comment("Multiplier applied to the emerald cost of all mod villager trades.\n" +
                    "所有本模组村民交易的绿宝石价格倍率。")
            .defineInRange("priceMultiplier", 1.0, 0.1, 100.0);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("reinforcement");
    }

    public static final ForgeConfigSpec.BooleanValue ENABLE_KOGASA_EQUIPMENT = BUILDER
            .comment("Enable custom anvil reinforcement and ability infusion for mod equipment.\n" +
                    "启用本模组装备的自定义铁砧强化与能力注入配方。")
            .define("enableKogasaEquipment", true);

    public static final ForgeConfigSpec.IntValue MAX_REINFORCEMENTS = BUILDER
            .comment("Maximum reinforcement count.\n" +
                    "最大强化数。")
            .defineInRange("maxReinforcements", 3, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue REINFORCEMENT_COST = BUILDER
            .comment("Experience level cost per reinforcement.\n" +
                    "强化消耗的经验等级。")
            .defineInRange("reinforcementCost", 1, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue CLEAR_COST = BUILDER
            .comment("Experience level cost clearing all reinforcements and abilities.\n" +
                    "清除所有强化和能力消耗的经验等级。")
            .defineInRange("clearCost", 1, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue AMMO_INFUSION_COST = BUILDER
            .comment("Experience level cost to infuse special ammo into an aya bow/crossbow.\n" +
                    "在铁砧中为文文弓/弩装载特殊弹药消耗的经验等级。")
            .defineInRange("ammoInfusionCost", 3, 0, 100);

    public static final ForgeConfigSpec.IntValue SEAL_COST = BUILDER
            .comment("Experience level cost to seal equipment with a stick in the anvil (loot-style seal: -50% attributes, abilities inactive until growth unlocks it).\n" +
                    "在铁砧中用木棍封印装备消耗的经验等级（战利品式封印：属性 -50%、能力暂失效，成长达标后解封）。")
            .defineInRange("sealCost", 1, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue WEAPON_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for weapons and tools.\n" +
                    "强化时吸收的武器/工具属性比例。")
            .defineInRange("weaponAbsorbedRatio", 1.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ARMOR_ABSORBED_RATIO = BUILDER
            .comment("Absorbed value ratio for armor.\n" +
                    "强化时吸收的护甲属性比例。")
            .defineInRange("armorAbsorbedRatio", 1.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ATTRIBUTE_ABSORB_MELEE_BLACKLIST = BUILDER
            .comment("Attributes never absorbed when the output is melee. Supports * and ? wildcards.\n" +
                    "近战装备吸收时排除的属性，支持 * 和 ? 通配符。")
            .defineListAllowEmpty("attributeAbsorbMeleeBlacklist",
                    List.of("minecraft:generic.attack_speed"),
                    o -> o instanceof String s && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ATTRIBUTE_ABSORB_RANGED_BLACKLIST = BUILDER
            .comment("Attributes never absorbed when the output is ranged. Supports * and ? wildcards.\n" +
                    "远程装备吸收时排除的属性，支持 * 和 ? 通配符。")
            .defineListAllowEmpty("attributeAbsorbRangedBlacklist",
                    List.of("minecraft:generic.attack_speed"),
                    o -> o instanceof String s && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ATTRIBUTE_ABSORB_ARMOR_BLACKLIST = BUILDER
            .comment("Attributes never absorbed when the output is armor. Supports * and ? wildcards.\n" +
                    "护甲吸收时排除的属性，支持 * 和 ? 通配符。")
            .defineListAllowEmpty("attributeAbsorbArmorBlacklist",
                    List.of(
                            "minecraft:generic.armor",
                            "minecraft:generic.armor_toughness",
                            "minecraft:generic.knockback_resistance"),
                    o -> o instanceof String s && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ATTRIBUTE_ABSORB_SCALE = BUILDER
            .comment("Per-attribute absorption multiplier, format: attribute_id=value, e.g. irons_spellbooks:*=0.1.\n" +
                    "Empty = all allowed attributes use 1.0; the first matching pattern wins; supports * and ? wildcards.\n" +
                    "按属性配置吸收倍率，格式：属性ID=数值，例如 irons_spellbooks:*=0.1。\n" +
                    "留空 = 所有允许属性倍率为 1.0；按列表顺序取第一个匹配项；支持 * 和 ? 通配符。")
            .defineListAllowEmpty("attributeAbsorbScale", List.of(),
                    o -> o instanceof String s && s.contains("=") && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> DYNAMIC_STAT_MATERIALS = BUILDER
            .comment("Items listed here use their current main-hand ATTACK_DAMAGE attribute as the base absorption value when used as reinforcement material. Supports * wildcards.\n" +
                    "Use *:* to apply this logic to every item. Use modid:* for a whole mod (default: cataclysm:*).\n" +
                    "WARNING: do NOT add items whose stats change with NBT/growth (this mod's own equipment, Star Meow Craft's Uniforged, etc.). Their current value would be absorbed directly and become extremely overpowered.\n" +
                    "此处列出的物品作为强化材料时，会用当前主手攻击伤害属性作为吸收基准。支持 * 通配符。\n" +
                    "填 *:* 可对所有物品生效；按模组用 modid:*（默认 cataclysm:*）。\n" +
                    "警告：不要加入数值会随 NBT/成长变化的物品（本模组装备、星喵工艺的千刃锻锋等），否则吸收值会非常超模。\n" +
                    "Format: modid:item_id, e.g. cataclysm:*")
            .defineListAllowEmpty("dynamicStatMaterials",
                    List.of("cataclysm:*"),
                    o -> o instanceof String s && !s.isBlank());

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

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ATTRIBUTE_GROWTH_BLACKLIST = BUILDER
            .comment("Attributes that do not receive the growth multiplier. Supports * and ? wildcards.\n" +
                    "不参与成长倍率的属性，支持 * 和 ? 通配符。")
            .defineListAllowEmpty("attributeGrowthBlacklist", List.of(),
                    o -> o instanceof String s && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> ATTRIBUTE_GROWTH_CAPS = BUILDER
            .comment("Per-attribute cap for the final grown additive value, format: attribute_id=cap.\n" +
                    "Empty = no cap; first matching pattern wins; supports * and ? wildcards.\n" +
                    "按属性限制成长后的最终加值，格式：属性ID=上限。\n" +
                    "留空 = 不限制；按列表顺序取第一个匹配项；支持 * 和 ? 通配符。")
            .defineListAllowEmpty("attributeGrowthCaps", List.of(),
                    o -> o instanceof String s && s.contains("=") && !s.isBlank());

    public static final ForgeConfigSpec.IntValue GROWTH_UNLOCK_REINFORCEMENTS = BUILDER
            .comment("Reinforcement count required to unlock growth. Can be lower than maxReinforcements.\n" +
                    "解锁成长所需的强化数。可以低于最大强化数。")
            .defineInRange("growthUnlockReinforcements", 3, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_USE = BUILDER
            .comment("Growth gained per correct use. 0.001 = 0.1%.\n" +
                    "每次成长获得的百分比加成。0.001 = 0.1%。")
            .defineInRange("growthPerUse", 0.001, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MAX_GROWTH = BUILDER
            .comment("Maximum stored growth value. 1.0 means +100% growth bonus (growth multiplier x2.0); default 3.0 means +300% bonus (growth multiplier x4.0).\n" +
                    "最大成长值。1.0 表示 +100% 成长加成（成长倍率 ×2.0）；默认 3.0 表示 +300% 加成（成长倍率 ×4.0）。")
            .defineInRange("maxGrowth", 3, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ENABLE_EXTRA_SLOTS = BUILDER
            .comment("Unlock extra reinforcement slots through growth.\n" +
                    "通过成长解锁额外强化槽位。")
            .define("enableExtraSlots", true);

    public static final ForgeConfigSpec.DoubleValue GROWTH_PER_EXTRA_SLOT = BUILDER
            .comment("Growth required to unlock one extra reinforcement slot. Default 0.1 = 10% growth.\n" +
                    "解锁一个额外强化槽所需的成长值。默认 0.1 = 10%。")
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
            .comment("Growth required to unlock one random enchantment. Default 0.02 = 2% growth per enchant.\n" +
                    "解锁一个随机附魔所需的成长值。默认 0.02 = 2%。")
            .defineInRange("growthPerEnchant", 0.02, 0.0001, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue MAX_GROWTH_ENCHANTS = BUILDER
            .comment("Maximum number of enchantments granted by growth.\n" +
                    "通过成长获得的最大附魔数量。")
            .defineInRange("maxGrowthEnchants", 150, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.BooleanValue ALLOW_MAX_LEVEL_ENCHANTS = BUILDER
            .comment("If true, granted enchantments get a random level from 1 to max. If false, always level 1.\n" +
                    "为 true 时附魔等级随机 1~上限；为 false 时固定 1 级。")
            .define("allowMaxLevelEnchants", true);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GROWTH_ENCHANT_BLACKLIST = BUILDER
            .comment("Mode 1 (absolute blacklist): never granted by growth; existing copies are removed when the player logs in; the anvil will not produce items carrying them. Supports * wildcards.\n" +
                    "Existing config files are not migrated automatically when this default list changes.\n" +
                    "模式 1（绝对黑名单）：成长系统绝对不会授予；玩家进存档时清除已有附魔；铁砧也不会产出带这些附魔的物品。支持 * 通配符。\n" +
                    "默认列表更新不会自动写入已有配置文件，升级后请手动补充。\n" +
                    "Format: modid:enchantment_id, e.g. minecraft:riptide")
            .defineListAllowEmpty("growthEnchantBlacklist", List.of(),
                    o -> o instanceof String s && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GROWTH_ENCHANT_UPGRADE_ONLY = BUILDER
            .comment("Mode 2: if the item already has this enchantment it may still be upgraded; if it does not have it, it will never be granted. Supports * wildcards.\n" +
                    "模式 2：本来没有就不给；本来就有的会正常升级。支持 * 通配符。\n" +
                    "Format: modid:enchantment_id")
            .defineListAllowEmpty("growthEnchantUpgradeOnly", List.of(
                            "minecraft:riptide",
                            "minecraft:silk_touch",
                            "minecraft:frost_walker",
                            "apotheosis:knowledge",
                            "deeperdarker:catalysis",
                            "kaleidoscope_end:void_walker"),
                    o -> o instanceof String s && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> GROWTH_ENCHANT_FROZEN = BUILDER
            .comment("Mode 3: if present, keep it unchanged and never upgrade it; if absent, never grant it. Supports * wildcards.\n" +
                    "模式 3：本来有也不动、不升级；本来没有也不会给。支持 * 通配符。\n" +
                    "Format: modid:enchantment_id")
            .defineListAllowEmpty("growthEnchantFrozen", List.of(
                            "*:*curse",
                            "*:curse*",
                            "l2complements:*"),
                    o -> o instanceof String s && !s.isBlank());

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("equipment");
    }

    public static final ForgeConfigSpec.BooleanValue ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER = BUILDER
            .comment("If true, absorbed value is scaled by reinforcement/growth multipliers; if false, it is added after those multipliers.\n" +
                    "为 true 时，吸收值会受到强化/成长倍率放大；为 false 时，吸收值在这些倍率计算后再直接加算。")
            .define("absorbedValueIncludedInMultiplier", true);

    public static final ForgeConfigSpec.IntValue DURABILITY_FIXED_PER_REINFORCEMENT = BUILDER
            .comment("Fixed durability added per reinforcement.\n" +
                    "每次强化增加的固定耐久值")
            .defineInRange("durabilityFixedPerReinforcement", 500, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue DURABILITY_RATIO_PER_REINFORCEMENT = BUILDER
            .comment("Durability ratio added per reinforcement. Default 0.25 = +25% of base.\n" +
                    "每次强化因强化次数而增加的耐久值比例。默认 0.25 = +25%。")
            .defineInRange("durabilityRatioPerReinforcement", 0.25, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.push("tool");
    }

    public static final ForgeConfigSpec.DoubleValue MINING_SPEED_RATIO_PER_REINFORCEMENT = BUILDER
            .comment("Mining speed bonus ratio per reinforcement. 0.5 = +50% per level; the default 1.5 = +150% per level.\n" +
                    "每级强化增加的挖掘速度加成比例。0.5 = 每级 +50%；默认值 1.5 = 每级 +150%。")
            .defineInRange("miningSpeedRatioPerReinforcement", 1.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ATTACK_SPEED_CAP = BUILDER
            .comment("Attack speed cap. Default 4.0 (bare-hand speed).\n" +
                    "攻速封顶值。默认 4.0（空手攻速）。")
            .defineInRange("attackSpeedCap", 4.0, 2.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ATTACK_SPEED_GROWTH_RATIO = BUILDER
            .comment("Attack speed growth ratio. progress = min((growth + Aya news value) * ratio, 1.0). A progress of 1.0 reaches the configured bare-hand attack speed cap.\n" +
                    "攻速成长比例。进度 = min((成长值 + 文文新闻值) × 此倍率, 1.0)；进度达到 1.0 时，攻速达到配置的空手攻速上限。")
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
            .comment("Asymptotic knockback-resistance cap per armor piece. Growth and Aya news value approach this cap; each reinforcement raises the cap by 50%, up to 1.0.\n" +
                    "每件护甲的渐近击退抗性上限。成长值与文文新闻值越高，实际抗性越接近上限；每次强化会将该上限提高 50%，但最高不超过 1.0。")
            .defineInRange("knockbackMaxPerPiece", 0.5, 0.01, Double.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("ranged");
    }

    public static final ForgeConfigSpec.DoubleValue BOW_PIERCE_GROWTH = BUILDER
            .comment("Growth required for bows/crossbows to gain an extra Pierce I.\n" +
                    "弓弩获得额外穿透 I 所需的成长值。")
            .defineInRange("bowPierceGrowth", 0.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue RANGED_DRAW_SPEED_ABSORB_RATIO = BUILDER
            .comment("Fraction of the vanilla full-draw time absorbed as \"draw speed\" per bow/crossbow reinforcement.\n" +
                    "0.1 = every reinforcement absorbs 10% of the 20 (bow) / 25 (crossbow) ticks needed to draw fully.\n" +
                    "弓弩每次强化吸收的拉弓速度比例（相对原版拉满时间）。\n" +
                    "0.1 = 每次强化吸收 20（弓）/ 25（弩）tick 的 10%，即拉满时间缩短 1 tick。")
            .defineInRange("rangedDrawSpeedAbsorbRatio", 0.1, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue BOW_MULTISHOT_2_GROWTH = BUILDER
            .comment("Growth required for 2-arrow multishot.\n" +
                    "解锁双发所需的成长值。")
            .defineInRange("bowMultishot2Growth", 1.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue BOW_MULTISHOT_3_GROWTH = BUILDER
            .comment("Growth required for 3-arrow multishot.\n" +
                    "解锁三发所需的成长值。")
            .defineInRange("bowMultishot3Growth", 2.5, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue RANGED_MULTISHOT_SPREAD = BUILDER
            .comment("Multishot spread in degrees between adjacent arrows. 8 = ±4° for 2 arrows, ±8° for 3 arrows.\n" +
                    "多发时相邻箭矢的偏航角间隔（度）。8 = 双发 ±4°，三发 ±8°。")
            .defineInRange("multishotSpread", 8.0, 0.0, 45.0);

    public static final ForgeConfigSpec.DoubleValue CROSSBOW_AUTO_RELOAD_GROWTH = BUILDER
            .comment("Growth required for crossbow auto-reload. 0 = disabled.\n" +
                    "弩自动装填所需的成长值。0 = 禁用。")
            .defineInRange("crossbowAutoReloadGrowth", 2.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue NO_CONSUME_ARROW_START_GROWTH = BUILDER
            .comment("Growth required to start arrow-saving chance. 1.0 = 100% growth.\n" +
                    "开始概率不消耗箭矢的成长值阈值。1.0 = 100% 成长。")
            .defineInRange("noConsumeArrowStartGrowth", 2.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue NO_CONSUME_ARROW_FULL_GROWTH = BUILDER
            .comment("Growth required for 100% arrow-saving chance. 1.0 = 100% growth.\n" +
                    "达到 100% 不消耗箭矢的成长值。1.0 = 100% 成长。")
            .defineInRange("noConsumeArrowFullGrowth", 3.0, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("trident");
    }

    public static final ForgeConfigSpec.DoubleValue SAKUYA_KNIFE_BASE_BREAK_CHANCE = BUILDER
            .comment("Base chance for the knife to break into a broken state upon pickup. 0.3 = 30%.\n" +
                    "咲夜飞刀拾取时变为损坏状态的基础概率。0.3 = 30%。")
            .defineInRange("sakuyaKnifeBaseBreakChance", 0.3, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue SAKUYA_KNIFE_BREAK_REDUCTION = BUILDER
            .comment("Break chance reduction per reinforcement/growth point. 0.01 = 1%.\n" +
                    "每点强化/成长值降低的损坏概率。0.01 = 1%。")
            .defineInRange("sakuyaKnifeBreakReduction", 0.01, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue SAKUYA_KNIFE_REPAIR_COST = BUILDER
            .comment("Experience level cost to repair a batch of broken sakuya knives (up to 3 per operation).\n" +
                    "在铁砧中修复一批损坏的咲夜飞刀消耗的经验等级（每次最多 3 把）。")
            .defineInRange("sakuyaKnifeRepairCost", 3, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue SAKUYA_KNIFE_THROW_VELOCITY = BUILDER
            .comment("Initial throw velocity for Sakuya Knife. Vanilla trident is ~2.5.\n" +
                    "咲夜飞刀的初始投掷速度。原版三叉戟约为2.5。")
            .defineInRange("sakuyaKnifeThrowVelocity", 3.0, 0.5, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue SAKUYA_KNIFE_MENDING_REPAIR_CHANCE = BUILDER
            .comment("Chance per XP absorption that a broken knife with Mending consumes 1 XP and is restored to a normal knife. 0.2 = 20%; 0 disables.\n" +
                    "带经验修补的损坏飞刀：每次吸收经验有该概率消耗 1 点经验还原为正常飞刀。0.2 = 20%；0 = 禁用。")
            .defineInRange("mendingRepairChance", 0.2, 0.0, 1.0);

    static {
        BUILDER.pop(2);
    }

    static {
        BUILDER.push("ability");
    }

    public static final ForgeConfigSpec.IntValue ABILITY_INFUSION_COST = BUILDER
            .comment("Experience level cost to infuse an ability ingot into equipment.\n" +
                    "在铁砧中为装备注入能力锭消耗的经验等级。")
            .defineInRange("abilityInfusionCost", 3, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ABILITY_SLOT_INITIAL_GROWTH = BUILDER
            .comment("Growth required to unlock the FIRST ability slot. 0 = the first slot is always available.\n" +
                    "解锁第一个能力槽所需的成长值。默认 0 = 第一槽始终可用。")
            .defineInRange("abilitySlotInitialGrowth", 0.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue ABILITY_SLOT_GROWTH_INCREMENT = BUILDER
            .comment("Extra growth required for each additional ability slot (arithmetic progression).\n" +
                    "Slot N unlocks at: abilitySlotInitialGrowth + (N-1) * abilitySlotGrowthIncrement.\n" +
                    "Defaults 0 / 0.5: slots unlock at 0, 0.5, 1.0, 1.5 ... (capped by abilitySlotMax).\n" +
                    "每个额外能力槽所需的成长增量（等差数列解锁）。\n" +
                    "第 N 个槽在 初始值 + (N-1) × 增量 处解锁。默认 0 / 0.5：0、0.5、1.0、1.5 ...（受 abilitySlotMax 封顶）。")
            .defineInRange("abilitySlotGrowthIncrement", 0.5, 0.0001, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue ABILITY_SLOT_MAX = BUILDER
            .comment("Hard cap on the number of ability slots. Default 4.\n" +
                    "能力槽数量上限。默认 4。")
            .defineInRange("abilitySlotMax", 4, 1, 64);

    static {
        BUILDER.push("aya");
    }

    public static final ForgeConfigSpec.DoubleValue NEWS_VALUE_PER_RECORD = BUILDER
            .comment("News value gained per new record (mob/block/damage type).\n" +
                    "每记录一种新生物/方块/伤害类型获得的新闻值。")
            .defineInRange("newsValuePerRecord", 0.01, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue MAX_NEWS_VALUE = BUILDER
            .comment("Maximum news value.\n" +
                    "新闻值上限。")
            .defineInRange("maxNewsValue", 1.0, 0.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue SHOOT_INGOT_CHANCE = BUILDER
            .comment("Chance to receive an Aya Ingot per eligible screenshot or bow/crossbow-shot attempt. 0.1 = 10%; attempts during the shared cooldown are ignored.\n" +
                    "每次符合条件的截图或弓/弩射击尝试获得文文锭的概率。0.1 = 10%；共用冷却期间的尝试会被忽略。")
            .defineInRange("shootIngotChance", 0.1, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue SHOOT_COOLDOWN_SECONDS = BUILDER
            .comment("Shared cooldown in seconds for screenshot and bow/crossbow-shot reward attempts. 10 = 10 seconds; 0 disables the cooldown.\n" +
                    "截图与弓/弩射击奖励共用的冷却时间（秒）。10 = 10 秒；0 表示不启用冷却。")
            .defineInRange("shootCooldownSeconds", 10, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("cirno");
    }

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
            .defineInRange("cirnoSlowLevel", 4, 1, 256);

    public static final ForgeConfigSpec.DoubleValue CIRNO_SELF_SLOW_DURATION = BUILDER
            .comment("Slowness duration applied to yourself (in seconds). 5 = 5s.\n" +
                    "对自己施加缓慢的持续时间（秒）。5 = 5秒。")
            .defineInRange("cirnoSelfSlowDuration", 5, 1, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue CIRNO_SELF_SLOW_LEVEL = BUILDER
            .comment("Slowness level applied to yourself. 1 = Slowness I.\n" +
                    "对自己施加的缓慢等级。1 = 缓慢 I。")
            .defineInRange("cirnoSelfSlowLevel", 1, 1, 256);

    public static final ForgeConfigSpec.DoubleValue CIRNO_INGOT_DROP_CHANCE = BUILDER
            .comment("Chance to get a Cirno Ingot from qualifying ice events: mining without Silk Touch, Frosted Ice melting, or explosion-caused ice destruction. 0.09 = 9%.\n" +
                    "符合条件的冰类事件掉落琪露诺锭的概率：未使用精准采集挖掘、霜冰融化或爆炸破坏冰类方块。0.09 = 9%。")
            .defineInRange("cirnoIngotDropChance", 0.09, 0.0, 1.0);

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

    public static final ForgeConfigSpec.DoubleValue KOISHI_CRIT_CHANCE = BUILDER
            .comment("Koishi crit chance. 0.75 = 75%.\n" +
                    "恋恋暴击概率。0.75 = 75%。")
            .defineInRange("koishiCritChance", 0.75, 0.0, 1.0);

    public static final ForgeConfigSpec.DoubleValue KOISHI_CRIT_MULTIPLIER = BUILDER
            .comment("Koishi crit damage multiplier. 1.5 = 150%.\n" +
                    "恋恋暴击伤害倍率。1.5 = 150%。")
            .defineInRange("koishiCritMultiplier", 1.5, 1.0, 10.0);

    public static final ForgeConfigSpec.IntValue KOISHI_GHAST_KILLS_REQUIRED = BUILDER
            .comment("Number of Ghasts killed by reflected fireballs to earn one Koishi Ingot.\n" +
                    "反弹火球击杀多少个恶魂获得一个恋恋锭。")
            .defineInRange("koishiGhastKillsRequired", 2, 1, 10);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("kogasa");
    }

    public static final ForgeConfigSpec.DoubleValue KOGASA_INGOT_CHANCE = BUILDER
            .comment("Chance per server tick (20 ticks per second) to produce a Kogasa Ingot while the blast furnace is lit. Default 0.001 = 0.1%; 0.01 = 1%.\n" +
                    "高炉燃烧时每游戏刻（每秒 20 刻）产出小伞锭的概率。默认值 0.001 = 0.1%；0.01 = 1%。")
            .defineInRange("kogasaIngotChance", 0.001, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue KOGASA_INGOT_SEARCH_HEIGHT = BUILDER
            .comment("Maximum upward search height for kogasa ingot drop position.\n" +
                    "小伞锭产出时向上搜索空气的最大高度。")
            .defineInRange("kogasaIngotSearchHeight", 8, 1, 320);

    public static final ForgeConfigSpec.DoubleValue KOGASA_REINFORCEMENT_MULTIPLIER_PER_LEVEL = BUILDER
            .comment("Kogasa reinforcement overall multiplier bonus per reinforcement. 0.1 = +10% per level.\n" +
                    "强化装备每次强化增加的总体倍率。0.1 = 每级 +10%。")
            .defineInRange("kogasaReinforcementMultiplierPerLevel", 0.1, 0.0, Double.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("tenshi");
    }

    public static final ForgeConfigSpec.IntValue TENSHI_INGOT_MIN_Y = BUILDER
            .comment("The minimum Y level required to obtain a Tenshi Ingot by placing beacon base blocks.\n" +
                    "放置信标基座方块获取天子锭的最低 Y 轴高度。")
            .defineInRange("tenshiIngotMinY", 128, 0, 320);

    public static final ForgeConfigSpec.DoubleValue TENSHI_INGOT_CHANCE = BUILDER
            .comment("Chance to get a Tenshi Ingot. 0.105 = 10.5%.\n" +
                    "放置信标基座方块获取天子锭的概率。0.105 = 10.5%。")
            .defineInRange("tenshiIngotChance", 0.105, 0.0, 1.0);

    public static final ForgeConfigSpec.BooleanValue TENSHI_INGOT_CONSUME_BLOCK = BUILDER
            .comment("If true, the placed beacon-base block item is consumed when the ingot triggers.\n" +
                    "Default false: the block is not consumed (pure bonus loot), which is the historic behavior.\n" +
                    "NOTE: keeping this false means the player can repeatedly place the same block to keep rolling;\n" +
                    "set it to true if you want each roll to cost one block.\n" +
                    "天子锭触发时是否消耗被放置的方块物品。\n" +
                    "默认 false：不消耗方块（纯额外奖励，与历史行为一致）。\n" +
                    "注意：保持 false 意味着玩家可以反复放置同一方块持续触发；\n" +
                    "如果希望每次触发消耗一个方块，请设为 true。")
            .define("tenshiIngotConsumeBlock", false);

    public static final ForgeConfigSpec.DoubleValue TENSHI_DOMAIN_THRESHOLD = BUILDER
            .comment("Damage required to charge the 'State of No-Thought, No-Mind' (20 in default).\n" +
                    "无念无想的境界所需的蓄力伤害值（默认20）。")
            .defineInRange("tenshiDomainThreshold", 20.0, 1.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue TENSHI_BURST_DAMAGE_RATIO = BUILDER
            .comment("Percentage of target's max health dealt as extra damage when burst triggers. 0.1 = 10%.\n" +
                    "境界蓄满后，下一次命中造成目标最大生命值的额外伤害比例。0.1 = 10%。")
            .defineInRange("tenshiBurstDamageRatio", 0.1, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue TENSHI_DEBUFF_DURATION_SECONDS = BUILDER
            .comment("Duration of Slowness and Mining Fatigue applied on hit (in seconds).\n" +
                    "攻击命中时施加的缓慢与挖掘疲劳持续时间（秒）。")
            .defineInRange("tenshiDebuffDurationSeconds", 5, 1, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.DoubleValue TENSHI_FLYING_MOB_MULTIPLIER = BUILDER
            .comment("Damage multiplier against non-boss flying mobs. 2.0 = 200%.\n" +
                    "对非 Boss 飞行生物的伤害倍率。2.0 = 200%。")
            .defineInRange("tenshiFlyingMobMultiplier", 2.0, 1.0, Double.MAX_VALUE);

    public static final ForgeConfigSpec.IntValue TENSHI_DEBUFF_LEVEL = BUILDER
            .comment("Slowness / Mining Fatigue level applied by Tenshi hits. 7 = level VII (amplifier 6).\n" +
                    "天子命中时施加的缓慢与挖掘疲劳等级。7 = VII 级（amplifier 6）。")
            .defineInRange("tenshiDebuffLevel", 7, 1, 255);

    public static final ForgeConfigSpec.IntValue TENSHI_RESISTANCE_MAX_LEVEL = BUILDER
            .comment("Maximum Resistance level granted by the Tenshi charge. 4 = Resistance IV.\n" +
                    "天子蓄力提供的最高抗性等级。4 = 抗性 IV。")
            .defineInRange("tenshiResistanceMaxLevel", 4, 1, 255);

    public static final ForgeConfigSpec.IntValue TENSHI_RESISTANCE_REFRESH_TICKS = BUILDER
            .comment("Duration (ticks) of each Resistance refresh. 120 = 6 seconds.\n" +
                    "每次刷新抗性效果的持续时间（tick）。120 = 6 秒。")
            .defineInRange("tenshiResistanceRefreshTicks", 120, 20, 1200);

    public static final ForgeConfigSpec.IntValue TENSHI_RESISTANCE_THRESHOLD_DIVISOR = BUILDER
            .comment("The charge threshold is split into this many steps for Resistance levels. 4 = one level per quarter.\n" +
                    "蓄力阈值分成的段数，用于决定抗性等级。4 = 每四分之一阈值提升一级。")
            .defineInRange("tenshiResistanceThresholdDivisor", 4, 1, 20);

    public static final ForgeConfigSpec.BooleanValue TENSHI_BUFF_ALLOW_MODDED = BUILDER
            .comment("Allow beneficial effects from non-minecraft namespaces in the Tenshi random buff pool.\n" +
                    "Default false for safety; enable to let the whitelist/blacklist include modded effects.\n" +
                    "是否允许非 minecraft 命名空间的正面效果进入天子随机增益池。\n" +
                    "默认 false 以保证安全；启用后可用白名单/黑名单筛选模组效果。")
            .define("tenshiBuffAllowModded", false);

    public static final ForgeConfigSpec.IntValue TENSHI_BUFF_DURATION_SECONDS = BUILDER
            .comment("Seconds added each time a Tenshi random buff is applied.\n" +
                    "天子随机增益每次叠加增加的秒数。")
            .defineInRange("tenshiBuffDurationSeconds", 10, 1, 3600);

    public static final ForgeConfigSpec.IntValue TENSHI_BUFF_DURATION_CAP_SECONDS = BUILDER
            .comment("Maximum duration of a Tenshi random buff, in seconds.\n" +
                    "天子随机增益的最大持续时间（秒）。")
            .defineInRange("tenshiBuffDurationCapSeconds", 60, 1, 7200);

    public static final ForgeConfigSpec.IntValue TENSHI_BUFF_LEVEL_CAP = BUILDER
            .comment("Maximum level of a Tenshi random buff. 4 = level IV (amplifier 3).\n" +
                    "天子随机增益的最高等级。4 = IV 级（amplifier 3）。")
            .defineInRange("tenshiBuffLevelCap", 4, 1, 255);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TENSHI_BUFF_WHITELIST = BUILDER
            .comment("Tenshi random buff whitelist. Empty = all beneficial vanilla effects are allowed.\n" +
                    "If non-empty, only matching effects can be chosen. Supports * and ? wildcards.\n" +
                    "天子随机增益白名单。留空 = 允许所有原版正面效果；非空时只有匹配的效果会被选中。支持 * 和 ? 通配符。")
            .defineListAllowEmpty("tenshiBuffWhitelist", List.of(),
                    o -> o instanceof String s && !s.isBlank());

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> TENSHI_BUFF_BLACKLIST = BUILDER
            .comment("Tenshi random buff blacklist. Applied after the whitelist; matching effects are always excluded.\n" +
                    "Defaults exclude Instant Health and Hero of the Village. Supports * and ? wildcards.\n" +
                    "天子随机增益黑名单。在白名单之后生效；匹配的效果一律排除。支持 * 和 ? 通配符。\n" +
                    "默认排除瞬间治疗与村庄英雄。")
            .defineListAllowEmpty("tenshiBuffBlacklist",
                    List.of("minecraft:heal", "minecraft:hero_of_the_villager"),
                    o -> o instanceof String s && !s.isBlank());

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("sakuya");
    }

    public static final ForgeConfigSpec.DoubleValue SAKUYA_INGOT_CHANCE = BUILDER
            .comment("Chance to get a Sakuya Ingot at the start of each day if the coordinate condition is met. 0.1 = 10%.\n" +
                    "每天开始时，满足坐标条件获取咲夜锭的概率。0.1 = 10%。")
            .defineInRange("sakuyaIngotChance", 1 / 3D, 0.0, 1.0);

    public static final ForgeConfigSpec.IntValue SAKUYA_TIMESTOP_SECONDS_PER_EQUIPMENT = BUILDER
            .comment("Timestop duration per piece of Sakuya equipment (in seconds).\n" +
                    "每件咲夜能力装备提供的时停时间（秒）。")
            .defineInRange("sakuyaTimestopSecondsPerEquipment", 5, 1, 60);

    public static final ForgeConfigSpec.IntValue SAKUYA_TIMESTOP_RADIUS = BUILDER
            .comment("Timestop radius in blocks. 20 = 20 blocks.\n" +
                    "时停作用半径（格）。20 = 20 格。")
            .defineInRange("sakuyaTimestopRadius", 20, 1, 128);

    public static final ForgeConfigSpec.IntValue SAKUYA_TIMESTOP_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown in seconds after a timestop ends before it can be triggered again. 0 disables the cooldown.\n" +
                    "一次时停结束后，再次发动时停所需的冷却时间（秒）。0 表示不启用冷却。")
            .defineInRange("sakuyaTimestopCooldownSeconds", 30, 0, Integer.MAX_VALUE);

    static {
        BUILDER.pop();
    }

    static {
        BUILDER.push("ranged.aya");
    }

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

    public static final ForgeConfigSpec.BooleanValue ENABLE_KOISHI_RANGED = BUILDER
            .comment("Enable Koishi ranged effects: armor-piercing critical hits and special-target damage multipliers.\n" +
                    "启用恋恋远程效果：穿甲暴击与特殊目标伤害倍率。")
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
            .comment("Extra damage multiplier for Koishi ranged attacks: applies once to Endermen and Withers below 50% health, and twice to Ender Dragons (3.0 means x9 against Ender Dragons).\n" +
                    "恋恋远程攻击的特殊目标额外倍率：对末影人及生命值低于 50% 的凋灵应用一次；对末影龙应用两次（3.0 对末影龙相当于 ×9）。")
            .defineInRange("koishiProjectileImmuneMultiplier", 3.0, 1.0, Double.MAX_VALUE);

    static {
        BUILDER.pop(3);
    }

    static {
        BUILDER.push("youkai_homecoming");
    }

    public static final ForgeConfigSpec.BooleanValue YH_LOOT_ENABLED = BUILDER
            .comment("Add Youkai's Homecoming items to our Mystia Izakaya / Cirno ice house loot tables.\n" +
                    "是否把妖怪们的归家物品加入夜雀食堂/琪露诺冰屋宝箱。")
            .define("lootEnabled", true);

    public static final ForgeConfigSpec.DoubleValue YH_LOOT_CHANCE = BUILDER
            .comment("Chance for the bonus pool, 0~1. Default 0.5 = 50%.\n" +
                    "额外战利品池的触发概率，0~1，默认 0.5。")
            .defineInRange("lootChance", 0.5D, 0.0D, 1.0D);

    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> YH_LOOT_BLACKLIST = BUILDER
            .comment("Youkai's Homecoming item IDs excluded from the loot pools. Supports * and ? wildcards.\n" +
                    "从战利品池排除的妖归物品 ID，支持 * 和 ? 通配符。")
            .defineListAllowEmpty("lootBlacklist", List.of(),
                    o -> o instanceof String s && !s.isBlank());

    static {
        BUILDER.pop();
    }

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
