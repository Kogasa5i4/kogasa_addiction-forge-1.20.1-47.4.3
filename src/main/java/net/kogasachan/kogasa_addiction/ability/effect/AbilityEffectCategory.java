package net.kogasachan.kogasa_addiction.ability.effect;

/** 能力效果在 JEI / 手册中的分组顺序。 */
public enum AbilityEffectCategory {
    GENERAL("general"),
    MELEE("melee"),
    RANGED("ranged"),
    ARMOR("armor");

    private final String id;

    AbilityEffectCategory(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public String getNameKey() {
        return "jei.kogasa_addiction.ability_effect.category." + id;
    }
}
