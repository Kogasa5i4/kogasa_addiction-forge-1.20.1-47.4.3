package net.kogasachan.kogasa_addiction.ability.tooltip;

/**
 * 一个能力在装备 tooltip 中的专属描述渲染器。
 *
 * <p>实现类注册到 {@link KogasaAbilityTooltips}；
 * 新增能力时默认没有渲染器（tooltip 不显示专属段落），可选补一个实现。
 */
@FunctionalInterface
public interface KogasaAbilityTooltip {

    /**
     * 把该能力的描述追加进 {@code ctx}
     */
    void append(KogasaAbilityTooltipContext ctx);
}
