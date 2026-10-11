package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> writer) {
        var ingot = Ingredient.of(ModItems.KOGASA_INGOT.get());

        // 剑
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.KOGASA_SWORD.get())
                .pattern("I")
                .pattern("I")
                .pattern("S")
                .define('I', ModItems.KOGASA_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 镐
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.KOGASA_PICKAXE.get())
                .pattern("III")
                .pattern(" S ")
                .pattern(" S ")
                .define('I', ModItems.KOGASA_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 斧
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.KOGASA_AXE.get())
                .pattern("II")
                .pattern("IS")
                .pattern(" S")
                .define('I', ModItems.KOGASA_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 铲
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.KOGASA_SHOVEL.get())
                .pattern("I")
                .pattern("S")
                .pattern("S")
                .define('I', ModItems.KOGASA_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 锄
        ShapedRecipeBuilder.shaped(RecipeCategory.TOOLS, ModItems.KOGASA_HOE.get())
                .pattern("II")
                .pattern(" S")
                .pattern(" S")
                .define('I', ModItems.KOGASA_INGOT.get())
                .define('S', Items.STICK)
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 头盔
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.KOGASA_HELMET.get())
                .pattern("III")
                .pattern("I I")
                .define('I', ModItems.KOGASA_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 胸甲
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.KOGASA_CHESTPLATE.get())
                .pattern("I I")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.KOGASA_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 护腿
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.KOGASA_LEGGINGS.get())
                .pattern("III")
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.KOGASA_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);

        // 靴子
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.KOGASA_BOOTS.get())
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.KOGASA_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.KOGASA_INGOT.get()))
                .save(writer);
    }
}