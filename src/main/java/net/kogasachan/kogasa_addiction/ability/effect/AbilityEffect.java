package net.kogasachan.kogasa_addiction.ability.effect;

/**
 * 一条能力效果：只在 JEI 分类里显示名称，悬停时显示说明与当前配置数值。
 */
public record AbilityEffect(String id, AbilityEffectCategory category,
                            String nameKey, String descriptionKey) {
}
