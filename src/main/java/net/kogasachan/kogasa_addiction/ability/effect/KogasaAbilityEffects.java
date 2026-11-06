package net.kogasachan.kogasa_addiction.ability.effect;

import net.kogasachan.kogasa_addiction.ability.KogasaAbility;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 六种能力的效果条目，供 JEI 分类统一读取。 */
public final class KogasaAbilityEffects {

    private static final Map<KogasaAbility, List<AbilityEffect>> EFFECTS =
            new EnumMap<>(KogasaAbility.class);

    private static final String PREFIX = "jei.kogasa_addiction.ability_effect.";

    private static AbilityEffect effect(AbilityEffectCategory category, String id) {
        return new AbilityEffect(id, category, PREFIX + id + ".name", PREFIX + id + ".desc");
    }

    static {
        EFFECTS.put(KogasaAbility.AYA, List.of(
                effect(AbilityEffectCategory.GENERAL, "aya.news"),
                effect(AbilityEffectCategory.GENERAL, "aya.records"),
                effect(AbilityEffectCategory.RANGED, "aya.haste")
        ));
        EFFECTS.put(KogasaAbility.CIRNO, List.of(
                effect(AbilityEffectCategory.MELEE, "cirno.slow"),
                effect(AbilityEffectCategory.RANGED, "cirno.arrow"),
                effect(AbilityEffectCategory.GENERAL, "cirno.revive")
        ));
        EFFECTS.put(KogasaAbility.KOGASA, List.of(
                effect(AbilityEffectCategory.GENERAL, "kogasa.multiplier"),
                effect(AbilityEffectCategory.RANGED, "kogasa.combo")
        ));
        EFFECTS.put(KogasaAbility.KOISHI, List.of(
                effect(AbilityEffectCategory.MELEE, "koishi.melee_crit"),
                effect(AbilityEffectCategory.RANGED, "koishi.ranged_crit"),
                effect(AbilityEffectCategory.RANGED, "koishi.special")
        ));
        EFFECTS.put(KogasaAbility.SAKUYA, List.of(
                effect(AbilityEffectCategory.MELEE, "sakuya.instant"),
                effect(AbilityEffectCategory.GENERAL, "sakuya.time_stop")
        ));
        EFFECTS.put(KogasaAbility.TENSHI, List.of(
                effect(AbilityEffectCategory.ARMOR, "tenshi.charge"),
                effect(AbilityEffectCategory.MELEE, "tenshi.burst"),
                effect(AbilityEffectCategory.MELEE, "tenshi.debuff"),
                effect(AbilityEffectCategory.MELEE, "tenshi.flying"),
                effect(AbilityEffectCategory.MELEE, "tenshi.buff")
        ));
    }

    private KogasaAbilityEffects() {
    }

    public static List<AbilityEffect> get(KogasaAbility ability) {
        return EFFECTS.getOrDefault(ability, List.of());
    }

    public static List<AbilityEffect> inCategory(KogasaAbility ability, AbilityEffectCategory category) {
        return get(ability).stream().filter(e -> e.category() == category).toList();
    }
}
