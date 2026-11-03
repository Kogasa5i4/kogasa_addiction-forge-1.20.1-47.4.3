package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraftforge.fml.ModList;
import vazkii.patchouli.api.PatchouliAPI;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 给帕秋莉手册注册 $(kogasa_cfg:参数名) 文本函数：渲染页面时实时读取当前配置，
 * 让《数值与计算公式》等章节的数字随配置文件动态变化，无需重启。
 *
 * <p>Patchouli 为 compileOnly 依赖：仅当加载了 Patchouli 时才注册，
 * 类方法体内的 PatchouliAPI 引用在运行时才解析，未安装 Patchouli 时不会触发加载。
 */
public final class KogasaManualConfigFunctions {

    private static final Map<String, Supplier<String>> VALUES = new HashMap<>();

    static {
        // ---- 成长 / 强化 / 吸收 ----
        f("growthPerUse", () -> pct(ClientConfigCache.get(Config.GROWTH_PER_USE)));
        f("maxGrowth", () -> num(ClientConfigCache.get(Config.MAX_GROWTH)));
        f("maxGrowthPlusOne", () -> num(ClientConfigCache.get(Config.MAX_GROWTH) + 1.0));
        f("growthUnlockReinforcements", () -> Integer.toString(ClientConfigCache.get(Config.GROWTH_UNLOCK_REINFORCEMENTS)));
        f("growthPerEnchant", () -> pct(ClientConfigCache.get(Config.GROWTH_PER_ENCHANT)));
        f("maxGrowthEnchants", () -> Integer.toString(ClientConfigCache.get(Config.MAX_GROWTH_ENCHANTS)));
        f("growthPerExtraSlot", () -> pct(ClientConfigCache.get(Config.GROWTH_PER_EXTRA_SLOT)));
        f("maxExtraSlots", () -> Integer.toString(ClientConfigCache.get(Config.MAX_EXTRA_SLOTS)));
        f("maxReinforcements", () -> Integer.toString(ClientConfigCache.get(Config.MAX_REINFORCEMENTS)));
        f("maxReinforcementsTotal", () -> Integer.toString(
                ClientConfigCache.get(Config.MAX_REINFORCEMENTS) + ClientConfigCache.get(Config.MAX_EXTRA_SLOTS)));
        f("reinforcementCost", () -> Integer.toString(ClientConfigCache.get(Config.REINFORCEMENT_COST)));
        f("kogasaReinforcementPerLevel", () -> pct(ClientConfigCache.get(Config.KOGASA_REINFORCEMENT_MULTIPLIER_PER_LEVEL)));
        f("weaponAbsorbedRatio", () -> pct(ClientConfigCache.get(Config.WEAPON_ABSORBED_RATIO)));
        f("armorAbsorbedRatio", () -> pct(ClientConfigCache.get(Config.ARMOR_ABSORBED_RATIO)));
        f("durabilityFixed", () -> Integer.toString(ClientConfigCache.get(Config.DURABILITY_FIXED_PER_REINFORCEMENT)));
        f("durabilityRatio", () -> pct(ClientConfigCache.get(Config.DURABILITY_RATIO_PER_REINFORCEMENT)));

        // ---- 属性 ----
        f("miningSpeedRatioPerReinforcement", () -> pct(ClientConfigCache.get(Config.MINING_SPEED_RATIO_PER_REINFORCEMENT)));
        f("attackSpeedCap", () -> num(ClientConfigCache.get(Config.ATTACK_SPEED_CAP)));
        f("attackSpeedGrowthRatio", () -> pct(ClientConfigCache.get(Config.ATTACK_SPEED_GROWTH_RATIO)));
        f("knockbackBase", () -> pct(ClientConfigCache.get(Config.KNOCKBACK_BASE_RESISTANCE)));
        f("knockbackMaxPerPiece", () -> pct(ClientConfigCache.get(Config.KNOCKBACK_MAX_PER_PIECE)));

        // ---- 能力槽 / 封印 ----
        f("abilitySlotInitial", () -> pct(ClientConfigCache.get(Config.ABILITY_SLOT_INITIAL_GROWTH)));
        f("slot1", () -> pct(ClientConfigCache.get(Config.ABILITY_SLOT_INITIAL_GROWTH) + ClientConfigCache.get(Config.ABILITY_SLOT_GROWTH_INCREMENT)));
        f("slot2", () -> pct(ClientConfigCache.get(Config.ABILITY_SLOT_INITIAL_GROWTH) + 2 * ClientConfigCache.get(Config.ABILITY_SLOT_GROWTH_INCREMENT)));
        f("slot3", () -> pct(ClientConfigCache.get(Config.ABILITY_SLOT_INITIAL_GROWTH) + 3 * ClientConfigCache.get(Config.ABILITY_SLOT_GROWTH_INCREMENT)));
        f("abilitySlotMax", () -> Integer.toString(ClientConfigCache.get(Config.ABILITY_SLOT_MAX)));
        f("abilityInfusionCost", () -> Integer.toString(ClientConfigCache.get(Config.ABILITY_INFUSION_COST)));
        f("lockedUnlockGrowth", () -> pct(ClientConfigCache.get(Config.LOCKED_EQUIPMENT_UNLOCK_GROWTH)));

        // ---- 远程 ----
        f("bowPierceGrowth", () -> pct(ClientConfigCache.get(Config.BOW_PIERCE_GROWTH)));
        f("multishot2Growth", () -> pct(ClientConfigCache.get(Config.BOW_MULTISHOT_2_GROWTH)));
        f("multishot3Growth", () -> pct(ClientConfigCache.get(Config.BOW_MULTISHOT_3_GROWTH)));
        f("multishotSpread", () -> num(ClientConfigCache.get(Config.RANGED_MULTISHOT_SPREAD)));
        f("multishotSpreadHalf", () -> num(ClientConfigCache.get(Config.RANGED_MULTISHOT_SPREAD) / 2.0));
        f("autoReloadGrowth", () -> pct(ClientConfigCache.get(Config.CROSSBOW_AUTO_RELOAD_GROWTH)));
        f("noConsumeStartGrowth", () -> pct(ClientConfigCache.get(Config.NO_CONSUME_ARROW_START_GROWTH)));
        f("noConsumeFullGrowth", () -> pct(ClientConfigCache.get(Config.NO_CONSUME_ARROW_FULL_GROWTH)));
        f("drawSpeedAbsorbRatio", () -> pct(ClientConfigCache.get(Config.RANGED_DRAW_SPEED_ABSORB_RATIO)));

        // ---- 飞刀 ----
        f("knifeBreakChance", () -> pct(ClientConfigCache.get(Config.SAKUYA_KNIFE_BASE_BREAK_CHANCE)));
        f("knifeBreakReduction", () -> pct(ClientConfigCache.get(Config.SAKUYA_KNIFE_BREAK_REDUCTION)));
        f("knifeRepairCost", () -> Integer.toString(ClientConfigCache.get(Config.SAKUYA_KNIFE_REPAIR_COST)));
        f("knifeThrowVelocity", () -> num(ClientConfigCache.get(Config.SAKUYA_KNIFE_THROW_VELOCITY)));

        // ---- 文文 ----
        f("newsValuePerRecord", () -> pct(ClientConfigCache.get(Config.NEWS_VALUE_PER_RECORD)));
        f("newsValuePerRecordTenth", () -> pct(ClientConfigCache.get(Config.NEWS_VALUE_PER_RECORD) * 0.1));
        f("maxNewsValue", () -> pct(ClientConfigCache.get(Config.MAX_NEWS_VALUE)));
        f("maxNewsValuePlusOne", () -> num(ClientConfigCache.get(Config.MAX_NEWS_VALUE) + 1.0));
        f("ayaSpeedDuration", () -> Integer.toString(ClientConfigCache.get(Config.AYA_SPEED_DURATION_SECONDS)));
        f("ayaHasteDuration", () -> Integer.toString(ClientConfigCache.get(Config.AYA_HASTE_DURATION_SECONDS)));

        // ---- 琪露诺 ----
        f("cirnoSlowChance", () -> pct(ClientConfigCache.get(Config.CIRNO_SLOW_CHANCE)));
        f("cirnoSelfSlowChance", () -> pct(ClientConfigCache.get(Config.CIRNO_SELF_SLOW_CHANCE)));
        f("cirnoSlowDuration", () -> num(ClientConfigCache.get(Config.CIRNO_SLOW_DURATION)));
        f("cirnoSlowLevel", () -> Integer.toString(ClientConfigCache.get(Config.CIRNO_SLOW_LEVEL)));
        f("cirnoSelfSlowDuration", () -> num(ClientConfigCache.get(Config.CIRNO_SELF_SLOW_DURATION)));
        f("cirnoReviveGrowthCost", () -> pct(ClientConfigCache.get(Config.CIRNO_REVIVE_GROWTH_COST)));
        f("cirnoPotionLevelBonus", () -> Integer.toString(ClientConfigCache.get(Config.CIRNO_POTION_LEVEL_BONUS)));
        f("cirnoPotionDurationMultiplier", () -> num(ClientConfigCache.get(Config.CIRNO_POTION_DURATION_MULTIPLIER)));
        f("cirnoSlownessDurationSeconds", () -> Integer.toString(ClientConfigCache.get(Config.CIRNO_SLOWNESS_DURATION_TICKS) / 20));
        f("cirnoSlownessLevel", () -> Integer.toString(ClientConfigCache.get(Config.CIRNO_SLOWNESS_LEVEL)));

        // ---- 恋恋 ----
        f("koishiCritChance", () -> pct(ClientConfigCache.get(Config.KOISHI_CRIT_CHANCE)));
        f("koishiCritMultiplier", () -> num(ClientConfigCache.get(Config.KOISHI_CRIT_MULTIPLIER)));
        f("koishiGhastKills", () -> Integer.toString(ClientConfigCache.get(Config.KOISHI_GHAST_KILLS_REQUIRED)));
        f("koishiRangedCritChance", () -> pct(ClientConfigCache.get(Config.KOISHI_RANGED_CRIT_CHANCE)));
        f("koishiRangedCritMultiplier", () -> num(ClientConfigCache.get(Config.KOISHI_RANGED_CRIT_MULTIPLIER)));
        f("koishiProjectileImmuneMultiplier", () -> num(ClientConfigCache.get(Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER)));
        f("koishiProjectileImmuneSquared", () -> num(
                ClientConfigCache.get(Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER) * ClientConfigCache.get(Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER)));

        // ---- 小伞 ----
        f("kogasaStreakWindow", () -> Integer.toString(ClientConfigCache.get(Config.KOGASA_STREAK_RESET_SECONDS)));
        f("kogasaStreakBonus", () -> pct(ClientConfigCache.get(Config.KOGASA_STREAK_BONUS)));
        f("kogasaStreakMax", () -> Integer.toString(ClientConfigCache.get(Config.KOGASA_STREAK_MAX)));

        // ---- 天子 ----
        f("tenshiDomainThreshold", () -> num(ClientConfigCache.get(Config.TENSHI_DOMAIN_THRESHOLD)));
        f("tenshiBurstRatio", () -> pct(ClientConfigCache.get(Config.TENSHI_BURST_DAMAGE_RATIO)));
        f("tenshiDebuffDuration", () -> Integer.toString(ClientConfigCache.get(Config.TENSHI_DEBUFF_DURATION_SECONDS)));
        f("tenshiFlyingMultiplier", () -> num(ClientConfigCache.get(Config.TENSHI_FLYING_MOB_MULTIPLIER)));

        // ---- 咲夜 ----
        f("sakuyaTimestopSeconds", () -> Integer.toString(ClientConfigCache.get(Config.SAKUYA_TIMESTOP_SECONDS_PER_EQUIPMENT)));
        f("sakuyaTimestopRadius", () -> Integer.toString(ClientConfigCache.get(Config.SAKUYA_TIMESTOP_RADIUS)));
        f("sakuyaTimestopCooldown", () -> Integer.toString(ClientConfigCache.get(Config.SAKUYA_TIMESTOP_COOLDOWN_SECONDS)));
        f("sakuyaIngotChance", () -> pct(ClientConfigCache.get(Config.SAKUYA_INGOT_CHANCE)));
    }

    private KogasaManualConfigFunctions() {
    }

    private static void f(String name, Supplier<String> value) {
        VALUES.put(name, value);
    }

    private static String num(double v) {
        return KogasaNumberFormatHelper.formatTrimmed(v, 4);
    }

    private static String pct(double v) {
        return KogasaNumberFormatHelper.formatPercentTrimmed(v, 2);
    }

    /**
     * 客户端设置阶段调用。未安装 Patchouli 时跳过（编译期仅 compileOnly）。
     */
    public static void register() {
        if (!ModList.get().isLoaded("patchouli")) {
            return;
        }
        PatchouliAPI.get().registerFunction("kogasa_cfg", (arg, style) -> {
            Supplier<String> value = VALUES.get(arg);
            return value == null ? "?" : value.get();
        });
    }
}
