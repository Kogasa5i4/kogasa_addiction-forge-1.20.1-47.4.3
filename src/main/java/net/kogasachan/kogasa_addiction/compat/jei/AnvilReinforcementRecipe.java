package net.kogasachan.kogasa_addiction.compat.jei;

import net.minecraft.world.item.ItemStack;

/**
 * @param input 定义配方所需的数据，例如：ItemStack input, ItemStack material, ItemStack output
 */
public record AnvilReinforcementRecipe(ItemStack input, java.util.List<ItemStack> material, ItemStack output) {
}
