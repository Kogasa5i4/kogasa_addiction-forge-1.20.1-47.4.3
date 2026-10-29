package net.kogasachan.kogasa_addiction.recipe;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Aya 箭矢 + 滞留药水 → 8 支带药水 NBT 的 Aya 箭矢
 * 参考原版 TippedArrowRecipe 的实现。
 */
public class AyaArrowTippingRecipe extends CustomRecipe {

    public AyaArrowTippingRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(@NotNull CraftingContainer container, @NotNull Level level) {
        if (container.getWidth() != 3 || container.getHeight() != 3) return false;

        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                ItemStack stack = container.getItem(j + i * 3);
                if (stack.isEmpty()) return false;

                if (i == 1 && j == 1) {
                    // 中心必须是滞留药水
                    if (!stack.is(Items.LINGERING_POTION)) return false;
                } else {
                    // 其他 8 个位置必须是 Aya 箭矢
                    if (!stack.is(ModItems.AYA_ARROW.get())) return false;
                }
            }
        }
        return true;
    }

    @Override
    public @NotNull ItemStack assemble(@NotNull CraftingContainer container,
                                       @NotNull RegistryAccess registryAccess) {
        ItemStack potionStack = container.getItem(4);  // 中心位置
        if (!potionStack.is(Items.LINGERING_POTION)) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(ModItems.AYA_ARROW.get(), 8);
        PotionUtils.setPotion(result, PotionUtils.getPotion(potionStack));
        PotionUtils.setCustomEffects(result, PotionUtils.getCustomEffects(potionStack));
        return result;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= 3 && height >= 3;
    }

    @Override
    public @NotNull RecipeSerializer<?> getSerializer() {
        return ModRecipes.AYA_ARROW_TIPPING_SERIALIZER.get();
    }
}