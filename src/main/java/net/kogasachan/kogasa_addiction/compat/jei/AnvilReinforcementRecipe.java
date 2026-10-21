package net.kogasachan.kogasa_addiction.compat.jei;

import net.minecraft.world.item.ItemStack;

import java.util.List;

public record AnvilReinforcementRecipe(ItemStack input, List<ItemStack> materials, ItemStack output, RecipeKind kind) {}