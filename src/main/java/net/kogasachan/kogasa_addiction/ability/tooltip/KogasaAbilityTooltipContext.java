package net.kogasachan.kogasa_addiction.ability.tooltip;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 单个能力的 tooltip 渲染上下文。
 * 由 {@link KogasaAbilityTooltip} 的实现者读取，并把描述行追加进 tooltip。
 */
public final class KogasaAbilityTooltipContext {

    private final ItemStack stack;
    private final double growth;
    private final List<Component> tooltip;

    public KogasaAbilityTooltipContext(ItemStack stack, double growth, List<Component> tooltip) {
        this.stack = stack;
        this.growth = growth;
        this.tooltip = tooltip;
    }

    /**
     * 被查看的装备
     */
    public ItemStack stack() {
        return stack;
    }

    /**
     * 该装备当前的成长值
     */
    public double growth() {
        return growth;
    }

    /**
     * 追加一行 tooltip
     */
    public void add(Component line) {
        tooltip.add(line);
    }
}
