package net.kogasachan.kogasa_addiction.recipe;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, KogasaAddiction.MODID);

    public static final RegistryObject<RecipeSerializer<AyaArrowTippingRecipe>>
            AYA_ARROW_TIPPING_SERIALIZER = RECIPE_SERIALIZERS.register(
            "aya_arrow_tipping",
            () -> new SimpleCraftingRecipeSerializer<>(AyaArrowTippingRecipe::new));
}