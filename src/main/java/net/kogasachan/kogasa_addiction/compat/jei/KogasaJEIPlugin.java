package net.kogasachan.kogasa_addiction.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.common.Tags;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@JeiPlugin
public class KogasaJEIPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "jei_plugin");

    /**
     * 构造强化后的输出：复制输入，设置强化次数为1，记录吸收材料
     */
    private static ItemStack buildReinforcedOutput(ItemStack input, Item materialItem) {
        ItemStack output = input.copy();
        KogasaEquipmentData.setReinforcements(output, 1);

        ResourceLocation materialId = ForgeRegistries.ITEMS.getKey(materialItem);
        if (materialId != null) {
            KogasaEquipmentData.addUsedMaterial(output, materialId.toString());
        }

        // 吸收值（可选，让 JEI 显示更真实的数值）
        double absorb = KogasaEquipmentHelper.getEquipmentBaseValue(new ItemStack(materialItem));
        if (absorb > 0) {
            KogasaEquipmentData.addAbsorbedValue(output, absorb);
        }

        return output;
    }

    /**
     * 4 种能力锭 × 示例装备 → 输出带能力的装备
     * 使用剑作为输入代表，tooltip 说明适用于所有 Kogasa 装备
     */
    private static void registerAbilityRecipes(List<AnvilReinforcementRecipe> recipes) {
        // 用 LinkedHashMap 保证顺序：kogasa → aya → cirno → koishi
        Map<Item, KogasaAbility> abilityIngots = new LinkedHashMap<>();
        abilityIngots.put(ModItems.KOGASA_INGOT.get(), KogasaAbility.KOGASA);
        abilityIngots.put(ModItems.AYA_INGOT.get(), KogasaAbility.AYA);
        abilityIngots.put(ModItems.CIRNO_INGOT.get(), KogasaAbility.CIRNO);
        abilityIngots.put(ModItems.KOISHI_INGOT.get(), KogasaAbility.KOISHI);

        ItemStack sampleEquipment = new ItemStack(ModItems.KOGASA_SWORD.get());

        for (Map.Entry<Item, KogasaAbility> entry : abilityIngots.entrySet()) {
            ItemStack input = sampleEquipment.copy();
            List<ItemStack> materials = List.of(new ItemStack(entry.getKey()));

            ItemStack output = input.copy();
            KogasaAbilityData.addAbility(output, entry.getValue());

            recipes.add(new AnvilReinforcementRecipe(
                    input, materials, output, RecipeKind.ABILITY));
        }
    }

    /**
     * 9 种装备 × 对应标签内材料 → 输出强化后的装备
     */
    private static void registerReinforcementRecipes(List<AnvilReinforcementRecipe> recipes) {
        Map<Item, TagKey<Item>> equipmentTags = new LinkedHashMap<>();
        equipmentTags.put(ModItems.KOGASA_SWORD.get(), ItemTags.SWORDS);
        equipmentTags.put(ModItems.KOGASA_PICKAXE.get(), ItemTags.PICKAXES);
        equipmentTags.put(ModItems.KOGASA_AXE.get(), ItemTags.AXES);
        equipmentTags.put(ModItems.KOGASA_SHOVEL.get(), ItemTags.SHOVELS);
        equipmentTags.put(ModItems.KOGASA_HOE.get(), ItemTags.HOES);
        equipmentTags.put(ModItems.KOGASA_HELMET.get(), Tags.Items.ARMORS_HELMETS);
        equipmentTags.put(ModItems.KOGASA_CHESTPLATE.get(), Tags.Items.ARMORS_CHESTPLATES);
        equipmentTags.put(ModItems.KOGASA_LEGGINGS.get(), Tags.Items.ARMORS_LEGGINGS);
        equipmentTags.put(ModItems.KOGASA_BOOTS.get(), Tags.Items.ARMORS_BOOTS);

        for (Map.Entry<Item, TagKey<Item>> entry : equipmentTags.entrySet()) {
            Item kogasaItem = entry.getKey();
            TagKey<Item> tag = entry.getValue();

            // 收集标签内所有物品（排除 Kogasa 自身和空气）
            List<ItemStack> materials = new ArrayList<>();
            BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
                Item item = holder.value();
                if (item == Items.AIR) return;
                if (item instanceof IKogasaEquipment) return;
                materials.add(new ItemStack(item));
            });
            if (materials.isEmpty()) continue;

            ItemStack input = new ItemStack(kogasaItem);
            ItemStack output = input.copy();
            KogasaEquipmentData.setReinforcements(output, 1);

            recipes.add(new AnvilReinforcementRecipe(
                    input, materials, output, RecipeKind.REINFORCEMENT));
        }
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        if (!ModList.get().isLoaded("jei")) return;
        registration.addRecipeCategories(new AnvilReinforcementCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        List<AnvilReinforcementRecipe> recipes = new ArrayList<>();

        // ========== 1. 能力配方（4 个，排前面） ==========
        registerAbilityRecipes(recipes);

        // ========== 2. 强化配方（9 个） ==========
        registerReinforcementRecipes(recipes);

        KogasaAddiction.LOGGER.info("Registered {} JEI anvil recipes", recipes.size());
        registration.addRecipes(AnvilReinforcementCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("jei")) return;
        // 将铁砧注册为“铁砧强化”类别的催化剂
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilReinforcementCategory.RECIPE_TYPE);
    }
}
