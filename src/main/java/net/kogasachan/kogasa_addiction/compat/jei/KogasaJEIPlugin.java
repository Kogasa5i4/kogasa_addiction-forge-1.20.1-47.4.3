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
import net.minecraft.nbt.CompoundTag;
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

        // ===== 弓弩强化配方：16 支箭 =====
        registerBowArrowRecipe(recipes, ModItems.AYA_BOW.get());
        registerBowArrowRecipe(recipes, ModItems.AYA_CROSSBOW.get());
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

        // 1. 能力配方（4 个）
        registerAbilityRecipes(recipes);

        // 2. 强化配方（11 个）
        registerReinforcementRecipes(recipes);

        // 3. 弹药配方（8 个）
        registerAmmoRecipes(recipes);

        KogasaAddiction.LOGGER.info("Registered {} JEI anvil recipes", recipes.size());
        registration.addRecipes(AnvilReinforcementCategory.RECIPE_TYPE, recipes);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("jei")) return;
        // 将铁砧注册为“铁砧强化”类别的催化剂
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilReinforcementCategory.RECIPE_TYPE);
    }

    /**
     * 弓弩用 16 支箭矢强化
     */
    private static void registerBowArrowRecipe(List<AnvilReinforcementRecipe> recipes, Item bowItem) {
        ItemStack input = new ItemStack(bowItem);

        // 用 ItemTags.ARROWS 里的前几个箭矢作为代表
        List<ItemStack> arrows = new ArrayList<>();
        BuiltInRegistries.ITEM.getTagOrEmpty(ItemTags.ARROWS).forEach(holder -> {
            Item item = holder.value();
            if (item == Items.AIR) return;
            ItemStack stack = new ItemStack(item, 16);  // count=16 让 JEI 显示 "16x"
            arrows.add(stack);
        });
        if (arrows.isEmpty()) return;

        ItemStack output = input.copy();
        KogasaEquipmentData.setReinforcements(output, 1);

        recipes.add(new AnvilReinforcementRecipe(
                input, arrows, output, RecipeKind.REINFORCEMENT));
    }

    /**
     * 弓弩装载弹药
     */
    private static void registerAmmoRecipes(List<AnvilReinforcementRecipe> recipes) {
        // 三种药水 × 两种弓弩
        registerPotionAmmoRecipes(recipes, ModItems.AYA_BOW.get(),
                Items.POTION, KogasaAbilityData.PotionKind.REGULAR);
        registerPotionAmmoRecipes(recipes, ModItems.AYA_BOW.get(),
                Items.SPLASH_POTION, KogasaAbilityData.PotionKind.SPLASH);
        registerPotionAmmoRecipes(recipes, ModItems.AYA_BOW.get(),
                Items.LINGERING_POTION, KogasaAbilityData.PotionKind.LINGERING);

        registerPotionAmmoRecipes(recipes, ModItems.AYA_CROSSBOW.get(),
                Items.POTION, KogasaAbilityData.PotionKind.REGULAR);
        registerPotionAmmoRecipes(recipes, ModItems.AYA_CROSSBOW.get(),
                Items.SPLASH_POTION, KogasaAbilityData.PotionKind.SPLASH);
        registerPotionAmmoRecipes(recipes, ModItems.AYA_CROSSBOW.get(),
                Items.LINGERING_POTION, KogasaAbilityData.PotionKind.LINGERING);

        // 烟花 × 两种弓弩
        registerFireworkAmmoRecipe(recipes, ModItems.AYA_BOW.get());
        registerFireworkAmmoRecipe(recipes, ModItems.AYA_CROSSBOW.get());
    }

    private static void registerPotionAmmoRecipes(List<AnvilReinforcementRecipe> recipes,
                                                  Item weaponItem, Item potionItem,
                                                  KogasaAbilityData.PotionKind kind) {
        ItemStack input = new ItemStack(weaponItem);
        ItemStack output = input.copy();

        // 用代表药水 ID 构造输出（tooltip 会说明"任意药水"）
        KogasaAbilityData.setPotionAmmo(output,
                ResourceLocation.fromNamespaceAndPath("minecraft", "healing"), kind);

        // 材料：列举常用的几种药水
        List<ItemStack> potions = new ArrayList<>();
        for (var potion : List.of(
                net.minecraft.world.item.alchemy.Potions.HEALING,
                net.minecraft.world.item.alchemy.Potions.SWIFTNESS,
                net.minecraft.world.item.alchemy.Potions.STRENGTH,
                net.minecraft.world.item.alchemy.Potions.HARMING,
                net.minecraft.world.item.alchemy.Potions.SLOWNESS,
                net.minecraft.world.item.alchemy.Potions.POISON)) {
            ItemStack stack = new ItemStack(potionItem);
            net.minecraft.world.item.alchemy.PotionUtils.setPotion(stack, potion);
            potions.add(stack);
        }

        recipes.add(new AnvilReinforcementRecipe(
                input, potions, output, RecipeKind.AMMO));
    }

    private static void registerFireworkAmmoRecipe(List<AnvilReinforcementRecipe> recipes,
                                                   Item weaponItem) {
        ItemStack input = new ItemStack(weaponItem);
        ItemStack output = input.copy();

        // 用空的烟花 NBT 标记
        CompoundTag emptyFireworkNbt = new CompoundTag();
        CompoundTag fireworkTag = new CompoundTag();
        fireworkTag.putByte("Flight", (byte) 1);
        emptyFireworkNbt.put("Fireworks", fireworkTag);
        KogasaAbilityData.setFireworkAmmo(output, emptyFireworkNbt);

        // 材料：烟花火箭
        List<ItemStack> fireworks = List.of(new ItemStack(Items.FIREWORK_ROCKET));

        recipes.add(new AnvilReinforcementRecipe(
                input, fireworks, output, RecipeKind.AMMO));
    }
}
