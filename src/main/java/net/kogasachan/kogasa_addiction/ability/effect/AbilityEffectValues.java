package net.kogasachan.kogasa_addiction.ability.effect;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.event.client.ClientConfigCache;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * JEI 悬停时显示的“当前配置数值”，与 Alt 装备提示使用同一套 ClientConfigCache。
 */
public final class AbilityEffectValues {

    private static final String PREFIX = "jei.kogasa_addiction.ability_effect.current.";

    private AbilityEffectValues() {
    }

    public static @Nullable Component get(AbilityEffect effect) {
        return switch (effect.id()) {
            case "aya.haste" -> {
                if (!ClientConfigCache.get(Config.ENABLE_AYA_RANGED)) {
                    yield disabled();
                }
                yield Component.translatable(PREFIX + "aya.haste",
                        ClientConfigCache.get(Config.AYA_SPEED_LEVEL),
                        ClientConfigCache.get(Config.AYA_SPEED_DURATION_SECONDS),
                        ClientConfigCache.get(Config.AYA_HASTE_LEVEL),
                        ClientConfigCache.get(Config.AYA_HASTE_DURATION_SECONDS));
            }
            case "cirno.slow" -> Component.translatable(PREFIX + "cirno.slow",
                    percent(ClientConfigCache.get(Config.CIRNO_SLOW_CHANCE)),
                    KogasaNumberFormatHelper.formatTrimmed(ClientConfigCache.get(Config.CIRNO_SLOW_DURATION), 1),
                    ClientConfigCache.get(Config.CIRNO_SLOW_LEVEL),
                    percent(ClientConfigCache.get(Config.CIRNO_SELF_SLOW_CHANCE)));
            case "cirno.arrow" -> {
                if (!ClientConfigCache.get(Config.ENABLE_CIRNO_RANGED)) {
                    yield disabled();
                }
                yield Component.translatable(PREFIX + "cirno.arrow",
                        ClientConfigCache.get(Config.CIRNO_POTION_LEVEL_BONUS),
                        KogasaNumberFormatHelper.formatTrimmed(
                                ClientConfigCache.get(Config.CIRNO_POTION_DURATION_MULTIPLIER), 2),
                        ClientConfigCache.get(Config.CIRNO_SLOWNESS_LEVEL),
                        KogasaNumberFormatHelper.formatTrimmed(
                                ClientConfigCache.get(Config.CIRNO_SLOWNESS_DURATION_TICKS) / 20.0, 1));
            }
            case "cirno.revive" -> ClientConfigCache.get(Config.CIRNO_REVIVE_ENABLED)
                    ? Component.translatable(PREFIX + "cirno.revive",
                    KogasaNumberFormatHelper.formatTrimmed(
                            ClientConfigCache.get(Config.CIRNO_REVIVE_GROWTH_COST), 2))
                    : Component.translatable(PREFIX + "disabled");
            case "kogasa.multiplier" -> Component.translatable(PREFIX + "kogasa.multiplier",
                    percent(ClientConfigCache.get(Config.KOGASA_REINFORCEMENT_MULTIPLIER_PER_LEVEL)));
            case "kogasa.combo" -> {
                if (!ClientConfigCache.get(Config.ENABLE_KOGASA_RANGED)) {
                    yield disabled();
                }
                int max = ClientConfigCache.get(Config.KOGASA_STREAK_MAX);
                yield Component.translatable(PREFIX + "kogasa.combo",
                        percent(ClientConfigCache.get(Config.KOGASA_STREAK_BONUS)),
                        ClientConfigCache.get(Config.KOGASA_STREAK_RESET_SECONDS),
                        max == 0 ? "∞" : Integer.toString(max));
            }
            case "koishi.melee_crit" -> Component.translatable(PREFIX + "koishi.melee_crit",
                    percent(ClientConfigCache.get(Config.KOISHI_CRIT_CHANCE)),
                    KogasaNumberFormatHelper.formatTrimmed(ClientConfigCache.get(Config.KOISHI_CRIT_MULTIPLIER), 2));
            case "koishi.ranged_crit" -> {
                if (!ClientConfigCache.get(Config.ENABLE_KOISHI_RANGED)) {
                    yield disabled();
                }
                yield Component.translatable(PREFIX + "koishi.ranged_crit",
                        percent(ClientConfigCache.get(Config.KOISHI_RANGED_CRIT_CHANCE)),
                        KogasaNumberFormatHelper.formatTrimmed(
                                ClientConfigCache.get(Config.KOISHI_RANGED_CRIT_MULTIPLIER), 2));
            }
            case "koishi.special" -> {
                if (!ClientConfigCache.get(Config.ENABLE_KOISHI_RANGED)) {
                    yield disabled();
                }
                double multiplier = ClientConfigCache.get(Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER);
                yield Component.translatable(PREFIX + "koishi.special",
                        KogasaNumberFormatHelper.formatTrimmed(multiplier, 2),
                        KogasaNumberFormatHelper.formatTrimmed(multiplier * multiplier, 2));
            }
            case "sakuya.time_stop" -> Component.translatable(PREFIX + "sakuya.time_stop",
                    ClientConfigCache.get(Config.SAKUYA_TIMESTOP_SECONDS_PER_EQUIPMENT),
                    ClientConfigCache.get(Config.SAKUYA_TIMESTOP_RADIUS),
                    ClientConfigCache.get(Config.SAKUYA_TIMESTOP_COOLDOWN_SECONDS));
            case "tenshi.charge" -> Component.translatable(PREFIX + "tenshi.charge",
                    KogasaNumberFormatHelper.formatTrimmed(ClientConfigCache.get(Config.TENSHI_DOMAIN_THRESHOLD), 1));
            case "tenshi.burst" -> Component.translatable(PREFIX + "tenshi.burst",
                    percent(ClientConfigCache.get(Config.TENSHI_BURST_DAMAGE_RATIO)));
            case "tenshi.debuff" -> Component.translatable(PREFIX + "tenshi.debuff",
                    ClientConfigCache.get(Config.TENSHI_DEBUFF_DURATION_SECONDS),
                    ClientConfigCache.get(Config.TENSHI_DEBUFF_LEVEL));
            case "tenshi.flying" -> Component.translatable(PREFIX + "tenshi.flying",
                    KogasaNumberFormatHelper.formatTrimmed(ClientConfigCache.get(Config.TENSHI_FLYING_MOB_MULTIPLIER), 2));
            case "tenshi.buff" -> Component.translatable(PREFIX + "tenshi.buff",
                    ClientConfigCache.get(Config.TENSHI_BUFF_DURATION_SECONDS),
                    ClientConfigCache.get(Config.TENSHI_BUFF_DURATION_CAP_SECONDS),
                    ClientConfigCache.get(Config.TENSHI_BUFF_LEVEL_CAP));
            default -> null;
        };
    }

    private static Component disabled() {
        return Component.translatable(PREFIX + "disabled");
    }

    private static String percent(double value) {
        return KogasaNumberFormatHelper.formatPercentTrimmed(value, 0);
    }
}
