package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.recipe.ModRecipes;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModRecipeProvider extends RecipeProvider {

    public ModRecipeProvider(PackOutput output) {
        super(output);
    }

    @Override
    protected void buildRecipes(@NotNull Consumer<FinishedRecipe> writer) {

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

        // 天子头盔
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TENSHI_HELMET.get())
                .pattern("III")
                .pattern("I I")
                .define('I', ModItems.TENSHI_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.TENSHI_INGOT.get()))
                .save(writer);

        // 天子胸甲
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TENSHI_CHESTPLATE.get())
                .pattern("I I")
                .pattern("III")
                .pattern("III")
                .define('I', ModItems.TENSHI_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.TENSHI_INGOT.get()))
                .save(writer);

        // 天子护腿
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TENSHI_LEGGINGS.get())
                .pattern("III")
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.TENSHI_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.TENSHI_INGOT.get()))
                .save(writer);

        // 天子靴子
        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.TENSHI_BOOTS.get())
                .pattern("I I")
                .pattern("I I")
                .define('I', ModItems.TENSHI_INGOT.get())
                .unlockedBy("has_ingot", has(ModItems.TENSHI_INGOT.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AYA_BOW.get())
                .pattern(" #X")
                .pattern("# X")
                .pattern(" #X")
                .define('#', ModItems.AYA_INGOT.get())
                .define('X', Items.STRING)
                .unlockedBy("has_aya_ingot", has(ModItems.AYA_INGOT.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AYA_CROSSBOW.get())
                .pattern("X#X")
                .pattern("&Y&")
                .pattern(" X ")
                .define('#', Items.IRON_INGOT)
                .define('&', Items.STRING)
                .define('X', ModItems.AYA_INGOT.get())
                .define('Y', Items.TRIPWIRE_HOOK)
                .unlockedBy("has_aya_ingot", has(ModItems.AYA_INGOT.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.AYA_ARROW.get(), 4)
                .pattern("X")
                .pattern("#")
                .pattern("Y")
                .define('X', ModItems.AYA_INGOT.get())
                .define('#', Items.STICK)
                .define('Y', Items.FEATHER)
                .unlockedBy("has_aya_ingot", has(ModItems.AYA_INGOT.get()))
                .save(writer);

        ShapedRecipeBuilder.shaped(RecipeCategory.COMBAT, ModItems.SAKUYA_KNIFE.get(), 1)
                .pattern(" X ")
                .pattern("X#X")
                .pattern(" X ")
                .define('X', ModItems.SAKUYA_INGOT.get())
                .define('#', Items.TRIDENT)
                .unlockedBy("has_sakuya_ingot", has(ModItems.SAKUYA_INGOT.get()))
                .save(writer);

        SpecialRecipeBuilder.special(ModRecipes.AYA_ARROW_TIPPING_SERIALIZER.get())
                .save(writer, KogasaAddiction.MODID + ":aya_arrow_tipping");
    }
}