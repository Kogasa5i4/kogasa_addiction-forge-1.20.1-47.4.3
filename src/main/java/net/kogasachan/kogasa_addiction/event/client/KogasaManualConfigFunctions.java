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
        f("growthPerUse", () -> pct(Config.GROWTH_PER_USE.get()));
        f("maxGrowth", () -> num(Config.MAX_GROWTH.get()));
        f("maxGrowthPlusOne", () -> num(Config.MAX_GROWTH.get() + 1.0));
        f("growthUnlockReinforcements", () -> Integer.toString(Config.GROWTH_UNLOCK_REINFORCEMENTS.get()));
        f("growthPerEnchant", () -> pct(Config.GROWTH_PER_ENCHANT.get()));
        f("maxGrowthEnchants", () -> Integer.toString(Config.MAX_GROWTH_ENCHANTS.get()));
        f("growthPerExtraSlot", () -> pct(Config.GROWTH_PER_EXTRA_SLOT.get()));
        f("maxExtraSlots", () -> Integer.toString(Config.MAX_EXTRA_SLOTS.get()));
        f("maxReinforcements", () -> Integer.toString(Config.MAX_REINFORCEMENTS.get()));
        f("maxReinforcementsTotal", () -> Integer.toString(
                Config.MAX_REINFORCEMENTS.get() + Config.MAX_EXTRA_SLOTS.get()));
        f("reinforcementCost", () -> Integer.toString(Config.REINFORCEMENT_COST.get()));
        f("kogasaReinforcementPerLevel", () -> pct(Config.KOGASA_REINFORCEMENT_MULTIPLIER_PER_LEVEL.get()));
        f("weaponAbsorbedRatio", () -> pct(Config.WEAPON_ABSORBED_RATIO.get()));
        f("armorAbsorbedRatio", () -> pct(Config.ARMOR_ABSORBED_RATIO.get()));
        f("durabilityFixed", () -> Integer.toString(Config.DURABILITY_FIXED_PER_REINFORCEMENT.get()));
        f("durabilityRatio", () -> pct(Config.DURABILITY_RATIO_PER_REINFORCEMENT.get()));

        // ---- 属性 ----
        f("miningSpeedRatioPerReinforcement", () -> pct(Config.MINING_SPEED_RATIO_PER_REINFORCEMENT.get()));
        f("attackSpeedCap", () -> num(Config.ATTACK_SPEED_CAP.get()));
        f("attackSpeedGrowthRatio", () -> pct(Config.ATTACK_SPEED_GROWTH_RATIO.get()));
        f("knockbackBase", () -> pct(Config.KNOCKBACK_BASE_RESISTANCE.get()));
        f("knockbackMaxPerPiece", () -> pct(Config.KNOCKBACK_MAX_PER_PIECE.get()));

        // ---- 能力槽 / 封印 ----
        f("abilitySlotInitial", () -> pct(Config.ABILITY_SLOT_INITIAL_GROWTH.get()));
        f("slot1", () -> pct(Config.ABILITY_SLOT_INITIAL_GROWTH.get() + Config.ABILITY_SLOT_GROWTH_INCREMENT.get()));
        f("slot2", () -> pct(Config.ABILITY_SLOT_INITIAL_GROWTH.get() + 2 * Config.ABILITY_SLOT_GROWTH_INCREMENT.get()));
        f("slot3", () -> pct(Config.ABILITY_SLOT_INITIAL_GROWTH.get() + 3 * Config.ABILITY_SLOT_GROWTH_INCREMENT.get()));
        f("abilitySlotMax", () -> Integer.toString(Config.ABILITY_SLOT_MAX.get()));
        f("abilityInfusionCost", () -> Integer.toString(Config.ABILITY_INFUSION_COST.get()));
        f("lockedUnlockGrowth", () -> pct(Config.LOCKED_EQUIPMENT_UNLOCK_GROWTH.get()));

        // ---- 远程 ----
        f("bowPierceGrowth", () -> pct(Config.BOW_PIERCE_GROWTH.get()));
        f("multishot2Growth", () -> pct(Config.BOW_MULTISHOT_2_GROWTH.get()));
        f("multishot3Growth", () -> pct(Config.BOW_MULTISHOT_3_GROWTH.get()));
        f("multishotSpread", () -> num(Config.RANGED_MULTISHOT_SPREAD.get()));
        f("multishotSpreadHalf", () -> num(Config.RANGED_MULTISHOT_SPREAD.get() / 2.0));
        f("autoReloadGrowth", () -> pct(Config.CROSSBOW_AUTO_RELOAD_GROWTH.get()));
        f("noConsumeStartGrowth", () -> pct(Config.NO_CONSUME_ARROW_START_GROWTH.get()));
        f("noConsumeFullGrowth", () -> pct(Config.NO_CONSUME_ARROW_FULL_GROWTH.get()));
        f("drawSpeedAbsorbRatio", () -> pct(Config.RANGED_DRAW_SPEED_ABSORB_RATIO.get()));

        // ---- 飞刀 ----
        f("knifeBreakChance", () -> pct(Config.SAKUYA_KNIFE_BASE_BREAK_CHANCE.get()));
        f("knifeBreakReduction", () -> pct(Config.SAKUYA_KNIFE_BREAK_REDUCTION.get()));
        f("knifeRepairCost", () -> Integer.toString(Config.SAKUYA_KNIFE_REPAIR_COST.get()));
        f("knifeThrowVelocity", () -> num(Config.SAKUYA_KNIFE_THROW_VELOCITY.get()));

        // ---- 文文 ----
        f("newsValuePerRecord", () -> pct(Config.NEWS_VALUE_PER_RECORD.get()));
        f("newsValuePerRecordTenth", () -> pct(Config.NEWS_VALUE_PER_RECORD.get() * 0.1));
        f("maxNewsValue", () -> pct(Config.MAX_NEWS_VALUE.get()));
        f("maxNewsValuePlusOne", () -> num(Config.MAX_NEWS_VALUE.get() + 1.0));
        f("ayaSpeedDuration", () -> Integer.toString(Config.AYA_SPEED_DURATION_SECONDS.get()));
        f("ayaHasteDuration", () -> Integer.toString(Config.AYA_HASTE_DURATION_SECONDS.get()));

        // ---- 琪露诺 ----
        f("cirnoSlowChance", () -> pct(Config.CIRNO_SLOW_CHANCE.get()));
        f("cirnoSelfSlowChance", () -> pct(Config.CIRNO_SELF_SLOW_CHANCE.get()));
        f("cirnoSlowDuration", () -> num(Config.CIRNO_SLOW_DURATION.get()));
        f("cirnoSlowLevel", () -> Integer.toString(Config.CIRNO_SLOW_LEVEL.get()));
        f("cirnoSelfSlowDuration", () -> num(Config.CIRNO_SELF_SLOW_DURATION.get()));
        f("cirnoReviveGrowthCost", () -> pct(Config.CIRNO_REVIVE_GROWTH_COST.get()));
        f("cirnoPotionLevelBonus", () -> Integer.toString(Config.CIRNO_POTION_LEVEL_BONUS.get()));
        f("cirnoPotionDurationMultiplier", () -> num(Config.CIRNO_POTION_DURATION_MULTIPLIER.get()));
        f("cirnoSlownessDurationSeconds", () -> Integer.toString(Config.CIRNO_SLOWNESS_DURATION_TICKS.get() / 20));
        f("cirnoSlownessLevel", () -> Integer.toString(Config.CIRNO_SLOWNESS_LEVEL.get()));

        // ---- 恋恋 ----
        f("koishiCritChance", () -> pct(Config.KOISHI_CRIT_CHANCE.get()));
        f("koishiCritMultiplier", () -> num(Config.KOISHI_CRIT_MULTIPLIER.get()));
        f("koishiGhastKills", () -> Integer.toString(Config.KOISHI_GHAST_KILLS_REQUIRED.get()));
        f("koishiRangedCritChance", () -> pct(Config.KOISHI_RANGED_CRIT_CHANCE.get()));
        f("koishiRangedCritMultiplier", () -> num(Config.KOISHI_RANGED_CRIT_MULTIPLIER.get()));
        f("koishiProjectileImmuneMultiplier", () -> num(Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER.get()));
        f("koishiProjectileImmuneSquared", () -> num(
                Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER.get() * Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER.get()));

        // ---- 小伞 ----
        f("kogasaStreakWindow", () -> Integer.toString(Config.KOGASA_STREAK_RESET_SECONDS.get()));
        f("kogasaStreakBonus", () -> pct(Config.KOGASA_STREAK_BONUS.get()));
        f("kogasaStreakMax", () -> Integer.toString(Config.KOGASA_STREAK_MAX.get()));

        // ---- 天子 ----
        f("tenshiDomainThreshold", () -> num(Config.TENSHI_DOMAIN_THRESHOLD.get()));
        f("tenshiBurstRatio", () -> pct(Config.TENSHI_BURST_DAMAGE_RATIO.get()));
        f("tenshiDebuffDuration", () -> Integer.toString(Config.TENSHI_DEBUFF_DURATION_SECONDS.get()));
        f("tenshiFlyingMultiplier", () -> num(Config.TENSHI_FLYING_MOB_MULTIPLIER.get()));

        // ---- 咲夜 ----
        f("sakuyaTimestopSeconds", () -> Integer.toString(Config.SAKUYA_TIMESTOP_SECONDS_PER_EQUIPMENT.get()));
        f("sakuyaTimestopRadius", () -> Integer.toString(Config.SAKUYA_TIMESTOP_RADIUS.get()));
        f("sakuyaTimestopCooldown", () -> Integer.toString(Config.SAKUYA_TIMESTOP_COOLDOWN_SECONDS.get()));
        f("sakuyaIngotChance", () -> pct(Config.SAKUYA_INGOT_CHANCE.get()));
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
