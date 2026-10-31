package net.kogasachan.kogasa_addiction.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.datagen.ModItemTagsProvider;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.KogasaAbilityIngotItem;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
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
import org.jetbrains.annotations.NotNull;

import java.util.*;

@JeiPlugin
public class KogasaJEIPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "jei_plugin");

    /**
     * 6 种能力锭 × 示例装备 → 输出带能力的装备
     * 使用剑作为输入代表，tooltip 说明适用于所有 Kogasa 装备
     */
    private static void registerAbilityRecipes(List<AnvilReinforcementRecipe> recipes) {
        ItemStack sampleEquipment = new ItemStack(ModItems.KOGASA_SWORD.get());

        // 1. 遍历标签内所有物品
        BuiltInRegistries.ITEM.getTagOrEmpty(ModItemTagsProvider.ABILITY_INGOTS).forEach(holder -> {
            Item ingotItem = holder.value();

            // 2. 过滤：只处理本模组的能力锭类物品
            if (ingotItem instanceof KogasaAbilityIngotItem abilityIngot) {
                KogasaAbility ability = abilityIngot.getAbility();

                ItemStack input = sampleEquipment.copy();
                List<ItemStack> materials = List.of(new ItemStack(ingotItem));

                ItemStack output = input.copy();
                KogasaAbilityData.addAbility(output, ability);

                recipes.add(new AnvilReinforcementRecipe(
                        input, materials, output, RecipeKind.ABILITY));
            }
        });
    }

    /**
     * 全部可强化装备 × 对应标签内材料 → 输出强化后的装备。
     *
     * <p>除 Fumo 之外全部收录（Fumo 是万能工具，按需求不在 JEI 展示强化配方）。
     * 每件装备可以对应多个标签 —— 咲夜飞刀在运行时会走三叉戟特例，
     * 额外接受剑与刀类材料，这里必须一并列出，否则 JEI 会漏展示。
     */
    private static void registerReinforcementRecipes(List<AnvilReinforcementRecipe> recipes) {
        Map<Item, List<TagKey<Item>>> equipmentTags = new LinkedHashMap<>();
        equipmentTags.put(ModItems.KOGASA_SWORD.get(), List.of(ItemTags.SWORDS));
        equipmentTags.put(ModItems.KOGASA_PICKAXE.get(), List.of(ItemTags.PICKAXES));
        equipmentTags.put(ModItems.KOGASA_AXE.get(), List.of(ItemTags.AXES));
        equipmentTags.put(ModItems.KOGASA_SHOVEL.get(), List.of(ItemTags.SHOVELS));
        equipmentTags.put(ModItems.KOGASA_HOE.get(), List.of(ItemTags.HOES));
        equipmentTags.put(ModItems.TENSHI_HELMET.get(), List.of(Tags.Items.ARMORS_HELMETS));
        equipmentTags.put(ModItems.TENSHI_CHESTPLATE.get(), List.of(Tags.Items.ARMORS_CHESTPLATES));
        equipmentTags.put(ModItems.TENSHI_LEGGINGS.get(), List.of(Tags.Items.ARMORS_LEGGINGS));
        equipmentTags.put(ModItems.TENSHI_BOOTS.get(), List.of(Tags.Items.ARMORS_BOOTS));
        equipmentTags.put(ModItems.AYA_BOW.get(), List.of(Tags.Items.TOOLS_BOWS));
        equipmentTags.put(ModItems.AYA_CROSSBOW.get(), List.of(Tags.Items.TOOLS_CROSSBOWS));
        // 咲夜飞刀：运行时 matchesReinforcementMaterial 走三叉戟特例，接受三叉戟 / 剑 / 刀
        equipmentTags.put(ModItems.SAKUYA_KNIFE.get(), List.of(
                Tags.Items.TOOLS_TRIDENTS,
                ItemTags.SWORDS,
                ModItemTagsProvider.KNIVES));

        for (Map.Entry<Item, List<TagKey<Item>>> entry : equipmentTags.entrySet()) {
            Item kogasaItem = entry.getKey();

            // 收集所有标签内物品（排除 Kogasa 自身和空气），跨标签去重
            Map<Item, ItemStack> materials = new LinkedHashMap<>();
            for (TagKey<Item> tag : entry.getValue()) {
                BuiltInRegistries.ITEM.getTagOrEmpty(tag).forEach(holder -> {
                    Item item = holder.value();
                    if (item == Items.AIR) return;
                    // 防止用本模组装备互相强化（运行时对同名物品也会拦下）
                    if (item instanceof IKogasaEquipment) return;
                    materials.putIfAbsent(item, new ItemStack(item));
                });
            }
            if (materials.isEmpty()) continue;

            ItemStack input = new ItemStack(kogasaItem);
            ItemStack output = input.copy();
            KogasaEquipmentData.setReinforcements(output, 1);

            recipes.add(new AnvilReinforcementRecipe(
                    input, new ArrayList<>(materials.values()), output, RecipeKind.REINFORCEMENT));
        }

        // ===== 弓弩强化配方：16 支箭 =====
        registerBowArrowRecipe(recipes, ModItems.AYA_BOW.get());
        registerBowArrowRecipe(recipes, ModItems.AYA_CROSSBOW.get());
    }

    /**
     * 弓弩用 16 支箭矢强化
     */
    private static void registerBowArrowRecipe(List<AnvilReinforcementRecipe> recipes, Item bowItem) {
        ItemStack input = new ItemStack(bowItem);

        // 用 ItemTags.ARROWS 中的所有箭矢作为代表
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

        // 用代表药水 ID 构造输出；材料列表仅展示常见药水，实际支持任意药水
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

        // 用 Flight=1 的最小烟花 NBT 标记
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

    private static void registerRepairRecipe(List<AnvilReinforcementRecipe> recipes) {
        recipes.add(new AnvilReinforcementRecipe(
                new ItemStack(ModItems.BROKEN_SAKUYA_KNIFE.get()),
                new ArrayList<>(Collections.singleton(new ItemStack(ModItems.SAKUYA_INGOT.get()))),
                new ItemStack(ModItems.SAKUYA_KNIFE.get()),
                RecipeKind.REPAIR));
    }

    private static void registerClearRecipe(List<AnvilReinforcementRecipe> recipes) {
        Item baseSword = ModItems.KOGASA_SWORD.get();

        // 构造输入：带有强化 1、成长 0.5、KOGASA 能力的小伞剑
        ItemStack input = new ItemStack(baseSword);
        KogasaEquipmentData.setReinforcements(input, 1);
        KogasaEquipmentData.setGrowth(input, 0.5);
        KogasaEquipmentData.addUsedMaterial(input, "minecraft:wooden_sword");
        KogasaAbilityData.addAbility(input, KogasaAbility.KOGASA);

        // ==================== 1. 水桶：清除强化 ====================
        // 构造输出：保留成长和能力，清除强化
        ItemStack output1 = new ItemStack(baseSword);
        KogasaEquipmentData.setGrowth(output1, 0.5);
        KogasaAbilityData.addAbility(output1, KogasaAbility.KOGASA);

        recipes.add(new AnvilReinforcementRecipe(
                input,
                new ArrayList<>(List.of(new ItemStack(Items.WATER_BUCKET))),
                output1,
                RecipeKind.CLEAR
        ));

        // ==================== 2. 奶桶：清除能力 ====================
        // 构造输出：保留成长和强化，清除能力
        ItemStack output2 = new ItemStack(baseSword);
        KogasaEquipmentData.setReinforcements(output2, 1);
        KogasaEquipmentData.setGrowth(output2, 0.5);
        KogasaEquipmentData.addUsedMaterial(output2, "minecraft:wooden_sword");

        recipes.add(new AnvilReinforcementRecipe(
                input,
                new ArrayList<>(List.of(new ItemStack(Items.MILK_BUCKET))),
                output2,
                RecipeKind.CLEAR
        ));
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

        // 1. 能力配方（6 个）
        registerAbilityRecipes(recipes);

        // 2. 强化配方（11 类装备 + 2 种弓弩箭矢）
        registerReinforcementRecipes(recipes);

        // 3. 弹药配方（8 个）
        registerAmmoRecipes(recipes);

        // 4. 修复配方（1 个）
        registerRepairRecipe(recipes);

        // 5. 清除配方（2 个）
        registerClearRecipe(recipes);

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
