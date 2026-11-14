package net.kogasachan.kogasa_addiction.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 铁砧配方。单物品配方仍用便捷构造器；弓弩/能力锭的合并展示使用多输入、多输出。
 */
public record AnvilReinforcementRecipe(
        List<ItemStack> inputs,
        List<ItemStack> materials,
        List<ItemStack> outputs,
        RecipeKind kind) {

    public AnvilReinforcementRecipe(ItemStack input, List<ItemStack> materials,
                                    ItemStack output, RecipeKind kind) {
        this(List.of(input), materials, List.of(output), kind);
    }

    /**
     * 供旧调用/搜索使用的首个输入。
     */
    public ItemStack input() {
        return inputs.isEmpty() ? ItemStack.EMPTY : inputs.get(0);
    }

    /**
     * 供旧调用/搜索使用的首个输出。
     */
    public ItemStack output() {
        return outputs.isEmpty() ? ItemStack.EMPTY : outputs.get(0);
    }
}
