package net.kogasachan.kogasa_addiction.compat.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityRegistry;
import net.kogasachan.kogasa_addiction.ability.effect.KogasaAbilityEffects;
import net.kogasachan.kogasa_addiction.datagen.ModItemTagsProvider;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.KogasaAbilityIngotItem;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKind;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKinds;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
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

import java.util.*;
import java.util.stream.Stream;

@JeiPlugin
public class KogasaJEIPlugin implements IModPlugin {
    private static final ResourceLocation PLUGIN_UID = ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "jei_plugin");
    private boolean anvilRecipesAdded;

    /**
     * ForgeRegistries 版"按标签取物品"：标签缺失时返回空流。
     */
    private static final List<Item> VANILLA_SWORDS = List.of(
            Items.WOODEN_SWORD, Items.STONE_SWORD, Items.IRON_SWORD,
            Items.GOLDEN_SWORD, Items.DIAMOND_SWORD, Items.NETHERITE_SWORD);

    private static Stream<Item> itemsInTag(TagKey<Item> tag) {
        // 逐物品用 ItemStack#is(TagKey) 判断。必须放在 JEI runtime 阶段调用：
        // 数据包 tag 在 JEI registerRecipes 阶段可能尚未绑定，过早扫描只会拿到空集。
        List<Item> found = ForgeRegistries.ITEMS.getValues().stream()
                .filter(item -> !new ItemStack(item).isEmpty() && new ItemStack(item).is(tag))
                .toList();
        if (found.isEmpty() && tag.equals(ItemTags.SWORDS)) {
            // 仅当 tag 确实为空时兜底原版剑；正常情况使用完整 tag + 外部兼容扫描。
            return VANILLA_SWORDS.stream();
        }
        return found.stream();
    }

    /**
     * 6 种能力锭 × 示例装备 → 输出带能力的装备
     * 使用剑作为输入代表，tooltip 说明适用于所有 Kogasa 装备
     */
    private static void registerAbilityRecipes(List<AnvilReinforcementRecipe> recipes) {
        ItemStack sampleEquipment = new ItemStack(ModItems.KOGASA_SWORD.get());
        List<ItemStack> ingots = new ArrayList<>();
        List<ItemStack> outputs = new ArrayList<>();

        itemsInTag(ModItemTagsProvider.ABILITY_INGOTS).forEach(ingotItem -> {
            if (ingotItem instanceof KogasaAbilityIngotItem abilityIngot) {
                KogasaAbility ability = abilityIngot.getAbility();
                ingots.add(new ItemStack(ingotItem));
                ItemStack output = sampleEquipment.copy();
                KogasaAbilityData.addAbility(output, ability);
                outputs.add(output);
            }
        });
        if (ingots.isEmpty()) return;

        // 能力锭细节由独立的能力页面展示；铁砧页只保留一组“任意能力锭 → 能力装备”的示例。
        recipes.add(new AnvilReinforcementRecipe(
                List.of(sampleEquipment), ingots, outputs, RecipeKind.ABILITY));
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
                ModItemTagsProvider.FORGE_TOOLS_KNIVES));

        for (Map.Entry<Item, List<TagKey<Item>>> entry : equipmentTags.entrySet()) {
            Item kogasaItem = entry.getKey();

            // 收集所有标签内物品（排除 Kogasa 自身和空气），跨标签去重
            Map<String, ItemStack> materials = new LinkedHashMap<>();
            for (TagKey<Item> tag : entry.getValue()) {
                itemsInTag(tag).forEach(item -> {
                    if (item == Items.AIR) return;
                    // 防止用本模组装备互相强化（运行时对同名物品也会拦下）
                    if (item instanceof IKogasaEquipment) return;
                    addMaterialVariants(materials, new ItemStack(item));
                });
            }

            // 显式同步运行时按类名兼容的 ISS / 灾变装备。
            KogasaEquipmentKind kind = KogasaEquipmentKinds.of(new ItemStack(kogasaItem));
            if (kind != null) {
                for (Item item : ForgeRegistries.ITEMS.getValues()) {
                    if (item == Items.AIR || item instanceof IKogasaEquipment) continue;
                    ItemStack stack = new ItemStack(item);
                    // tag 可能被外部数据包追加，也可能有模组装备没打 tag；
                    // 以运行时类型判定为准补全，避免 JEI 只显示“原版/早期 tag”物品。
                    boolean add = KogasaEquipmentKinds.of(stack) == kind;
                    if (kind == KogasaEquipmentKind.SWORD || kind == KogasaEquipmentKind.KNIFE) {
                        add |= item instanceof net.minecraft.world.item.SwordItem
                                || KogasaEquipmentHelper.isExternalWeapon(stack);
                    } else if (kind == KogasaEquipmentKind.AXE) {
                        add |= KogasaEquipmentHelper.isExternalWeapon(stack);
                    } else if (kind == KogasaEquipmentKind.HELMET
                            || kind == KogasaEquipmentKind.CHESTPLATE
                            || kind == KogasaEquipmentKind.LEGGINGS
                            || kind == KogasaEquipmentKind.BOOTS) {
                        var slot = KogasaEquipmentHelper.getArmorEquipmentSlot(stack);
                        add |= KogasaEquipmentHelper.isExternalArmor(stack)
                                && ((kind == KogasaEquipmentKind.HELMET && slot == net.minecraft.world.entity.EquipmentSlot.HEAD)
                                || (kind == KogasaEquipmentKind.CHESTPLATE && slot == net.minecraft.world.entity.EquipmentSlot.CHEST)
                                || (kind == KogasaEquipmentKind.LEGGINGS && slot == net.minecraft.world.entity.EquipmentSlot.LEGS)
                                || (kind == KogasaEquipmentKind.BOOTS && slot == net.minecraft.world.entity.EquipmentSlot.FEET));
                    }
                    if (add) addMaterialVariants(materials, stack);
                }
            }
            if (materials.isEmpty()) continue;

            ItemStack input = new ItemStack(kogasaItem);
            ItemStack output = input.copy();
            KogasaEquipmentData.setReinforcements(output, 1);

            recipes.add(new AnvilReinforcementRecipe(
                    input, new ArrayList<>(materials.values()), output, RecipeKind.REINFORCEMENT));
        }

        // ===== 弓弩强化配方：16 支箭（弓/弩合并展示）=====
        registerBowArrowRecipe(recipes, List.of(ModItems.AYA_BOW.get(), ModItems.AYA_CROSSBOW.get()));
    }

    /**
     * 把注册物品展开成实际变体（例如拔刀剑的 bladeState）；无展开器时保留原 ItemStack。
     */
    private static void addMaterialVariants(Map<String, ItemStack> materials, ItemStack base) {
        List<ItemStack> expanded = KogasaEquipmentHelper.expandMaterialStacks(base);
        if (expanded.isEmpty()) expanded = List.of(base.copy());
        for (ItemStack stack : expanded) {
            if (stack == null || stack.isEmpty()) continue;
            String key = stack.getItem() + "|" + stack.getTag();
            materials.putIfAbsent(key, stack.copy());
        }
    }

    /**
     * 弓弩用 16 支箭矢强化
     */
    private static void registerBowArrowRecipe(List<AnvilReinforcementRecipe> recipes, List<Item> weapons) {
        List<ItemStack> inputs = weapons.stream().map(ItemStack::new).toList();
        List<ItemStack> arrows = new ArrayList<>();
        itemsInTag(ItemTags.ARROWS).forEach(item -> {
            if (item == Items.AIR) return;
            arrows.add(new ItemStack(item, 16));
        });
        if (arrows.isEmpty()) return;

        List<ItemStack> outputs = inputs.stream().map(input -> {
            ItemStack output = input.copy();
            KogasaEquipmentData.setReinforcements(output, 1);
            return output;
        }).toList();

        recipes.add(new AnvilReinforcementRecipe(inputs, arrows, outputs, RecipeKind.REINFORCEMENT));
    }

    /**
     * 弓弩装载弹药
     */
    private static List<Item> rangedWeapons() {
        return List.of(ModItems.AYA_BOW.get(), ModItems.AYA_CROSSBOW.get());
    }

    private static void registerAmmoRecipes(List<AnvilReinforcementRecipe> recipes) {
        List<Item> weapons = rangedWeapons();

        // 三种药水；弓/弩合并为同一页
        registerPotionAmmoRecipes(recipes, weapons, Items.POTION,
                KogasaAbilityData.PotionKind.REGULAR);
        registerPotionAmmoRecipes(recipes, weapons, Items.SPLASH_POTION,
                KogasaAbilityData.PotionKind.SPLASH);
        registerPotionAmmoRecipes(recipes, weapons, Items.LINGERING_POTION,
                KogasaAbilityData.PotionKind.LINGERING);

        registerFireworkAmmoRecipe(recipes, weapons);
        registerYhAmmoRecipes(recipes, weapons);
        registerSpellAmmoRecipes(recipes, weapons);
    }

    private static void registerSpellAmmoRecipes(List<AnvilReinforcementRecipe> recipes,
                                                 List<Item> weapons) {
        if (ModList.get().isLoaded("irons_spellbooks")) {
            Item scroll = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("irons_spellbooks", "scroll"));
            if (scroll != null) {
                addSpellAmmoRecipe(recipes, weapons, List.of(new ItemStack(scroll)));
            }
        }

        if (ModList.get().isLoaded("goety")) {
            List<ItemStack> ammo = new ArrayList<>();
            for (Item item : ForgeRegistries.ITEMS.getValues()) {
                if (implementsInterface(item, "com.Polarice3.Goety.api.items.magic.IFocus")) {
                    ammo.add(new ItemStack(item));
                }
            }
            addSpellAmmoRecipe(recipes, weapons, ammo);
        }

        if (ModList.get().isLoaded("ars_nouveau")) {
            Item parchment = ForgeRegistries.ITEMS.getValue(
                    ResourceLocation.fromNamespaceAndPath("ars_nouveau", "spell_parchment"));
            if (parchment != null) {
                addSpellAmmoRecipe(recipes, weapons, List.of(new ItemStack(parchment)));
            }
        }
    }

    private static boolean implementsInterface(Item item, String interfaceName) {
        for (Class<?> c = item.getClass(); c != null; c = c.getSuperclass()) {
            for (Class<?> i : c.getInterfaces()) {
                if (i.getName().equals(interfaceName)) return true;
            }
        }
        return false;
    }

    private static void addSpellAmmoRecipe(List<AnvilReinforcementRecipe> recipes,
                                           List<Item> weapons, List<ItemStack> ammo) {
        if (ammo.isEmpty()) return;
        List<ItemStack> inputs = weapons.stream().map(ItemStack::new).toList();
        List<ItemStack> outputs = inputs.stream().map(input -> {
            ItemStack output = input.copy();
            KogasaAbilityData.setSpellAmmo(output, ammo.get(0), false);
            return output;
        }).toList();
        recipes.add(new AnvilReinforcementRecipe(inputs, new ArrayList<>(ammo), outputs,
                RecipeKind.AMMO_SPELL));
    }

    private static void registerYhAmmoRecipes(List<AnvilReinforcementRecipe> recipes,
                                              List<Item> weapons) {
        if (!ModList.get().isLoaded("youkaishomecoming")) return;
        addYhAmmoRecipe(recipes, weapons, "danmaku",
                KogasaAbilityData.AmmoType.YH_DANMAKU, RecipeKind.AMMO_YH);
        addYhAmmoRecipe(recipes, weapons, "laser",
                KogasaAbilityData.AmmoType.YH_LASER, RecipeKind.AMMO_YH);
        addYhAmmoRecipe(recipes, weapons, "preset_spell",
                KogasaAbilityData.AmmoType.YH_SPELL, RecipeKind.AMMO_YH);
        // 自定义符卡独立显示：NBT 可改变实际形态，JEI 只展示默认形态
        addYhAmmoRecipe(recipes, weapons, "custom_spell",
                KogasaAbilityData.AmmoType.YH_SPELL, RecipeKind.AMMO_YH_CUSTOM);
    }

    private static void addYhAmmoRecipe(List<AnvilReinforcementRecipe> recipes,
                                        List<Item> weapons, String tagPath,
                                        KogasaAbilityData.AmmoType type, RecipeKind kind) {
        TagKey<Item> tag = ItemTags.create(ResourceLocation.fromNamespaceAndPath("youkaishomecoming", tagPath));
        List<ItemStack> ammo = new ArrayList<>();
        itemsInTag(tag).forEach(item -> {
            if (item != Items.AIR) ammo.add(new ItemStack(item));
        });
        if (ammo.isEmpty()) return;

        List<ItemStack> inputs = weapons.stream().map(ItemStack::new).toList();
        List<ItemStack> outputs = inputs.stream().map(input -> {
            ItemStack output = input.copy();
            KogasaAbilityData.setYhAmmo(output, ammo.get(0), type);
            return output;
        }).toList();
        recipes.add(new AnvilReinforcementRecipe(inputs, ammo, outputs, kind));
    }

    private static void registerPotionAmmoRecipes(List<AnvilReinforcementRecipe> recipes,
                                                  List<Item> weapons, Item potionItem,
                                                  KogasaAbilityData.PotionKind kind) {
        List<ItemStack> inputs = weapons.stream().map(ItemStack::new).toList();
        List<ItemStack> outputs = inputs.stream().map(input -> {
            ItemStack output = input.copy();
            KogasaAbilityData.setPotionAmmo(output,
                    ResourceLocation.fromNamespaceAndPath("minecraft", "healing"), kind);
            return output;
        }).toList();

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

        recipes.add(new AnvilReinforcementRecipe(inputs, potions, outputs, RecipeKind.AMMO));
    }

    private static void registerFireworkAmmoRecipe(List<AnvilReinforcementRecipe> recipes,
                                                   List<Item> weapons) {
        List<ItemStack> inputs = weapons.stream().map(ItemStack::new).toList();
        CompoundTag emptyFireworkNbt = new CompoundTag();
        CompoundTag fireworkTag = new CompoundTag();
        fireworkTag.putByte("Flight", (byte) 1);
        emptyFireworkNbt.put("Fireworks", fireworkTag);
        List<ItemStack> outputs = inputs.stream().map(input -> {
            ItemStack output = input.copy();
            KogasaAbilityData.setFireworkAmmo(output, emptyFireworkNbt);
            return output;
        }).toList();

        recipes.add(new AnvilReinforcementRecipe(inputs,
                List.of(new ItemStack(Items.FIREWORK_ROCKET)), outputs, RecipeKind.AMMO));
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

        ItemStack input = new ItemStack(baseSword);
        KogasaEquipmentData.setReinforcements(input, 1);
        KogasaEquipmentData.setGrowth(input, 0.5);
        KogasaEquipmentData.addUsedMaterial(input, "minecraft:wooden_sword");
        KogasaAbilityData.addAbility(input, KogasaAbility.KOGASA);

        ItemStack clearReinforcement = new ItemStack(baseSword);
        KogasaEquipmentData.setGrowth(clearReinforcement, 0.5);
        KogasaAbilityData.addAbility(clearReinforcement, KogasaAbility.KOGASA);

        ItemStack clearAbility = new ItemStack(baseSword);
        KogasaEquipmentData.setReinforcements(clearAbility, 1);
        KogasaEquipmentData.setGrowth(clearAbility, 0.5);
        KogasaEquipmentData.addUsedMaterial(clearAbility, "minecraft:wooden_sword");

        // 水桶清强化、奶桶清能力合并到同一页，避免两条几乎相同的 JEI 配方。
        recipes.add(new AnvilReinforcementRecipe(
                List.of(input),
                List.of(new ItemStack(Items.WATER_BUCKET), new ItemStack(Items.MILK_BUCKET)),
                List.of(clearReinforcement, clearAbility),
                RecipeKind.CLEAR));
    }

    private static List<AnvilReinforcementRecipe> buildAnvilRecipes() {
        List<AnvilReinforcementRecipe> recipes = new ArrayList<>();
        registerAbilityRecipes(recipes);
        registerReinforcementRecipes(recipes);
        registerAmmoRecipes(recipes);
        registerRepairRecipe(recipes);
        registerClearRecipe(recipes);
        return recipes;
    }

    @Override
    public void onRuntimeAvailable(@NotNull IJeiRuntime runtime) {
        if (anvilRecipesAdded) return;
        List<AnvilReinforcementRecipe> recipes = buildAnvilRecipes();
        runtime.getRecipeManager().addRecipes(AnvilReinforcementCategory.RECIPE_TYPE, recipes);
        anvilRecipesAdded = true;
        KogasaAddiction.LOGGER.info("Registered {} JEI anvil recipes at runtime", recipes.size());
    }

    @Override
    public void onRuntimeUnavailable() {
        anvilRecipesAdded = false;
    }

    @Override
    public @NotNull ResourceLocation getPluginUid() {
        return PLUGIN_UID;
    }

    @Override
    public void registerCategories(@NotNull IRecipeCategoryRegistration registration) {
        if (!ModList.get().isLoaded("jei")) return;
        registration.addRecipeCategories(
                new AnvilReinforcementCategory(registration.getJeiHelpers().getGuiHelper()),
                new AbilityIngotCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(@NotNull IRecipeRegistration registration) {
        // 铁砧配方依赖数据包 tag（剑/工具/箭矢等），必须等 JEI runtime 可用、
        // tag 完整绑定后再构建，见 onRuntimeAvailable。这里只注册不依赖 tag 的能力说明页。

        // ===== 能力锭信息分类：六种能力各一条 =====
        List<AbilityIngotRecipe> abilityRecipes = new ArrayList<>();
        List<Item> abilityEquipment = List.of(
                ModItems.KOGASA_SWORD.get(),
                ModItems.KOGASA_PICKAXE.get(),
                ModItems.KOGASA_AXE.get(),
                ModItems.KOGASA_SHOVEL.get(),
                ModItems.KOGASA_HOE.get(),
                ModItems.TENSHI_HELMET.get(),
                ModItems.TENSHI_CHESTPLATE.get(),
                ModItems.TENSHI_LEGGINGS.get(),
                ModItems.TENSHI_BOOTS.get(),
                ModItems.AYA_BOW.get(),
                ModItems.AYA_CROSSBOW.get(),
                ModItems.SAKUYA_KNIFE.get());

        KogasaAbilityRegistry.ingots().forEach((ability, ingot) -> {
            List<ItemStack> outputs = new ArrayList<>();
            for (Item equipment : abilityEquipment) {
                ItemStack output = new ItemStack(equipment);
                KogasaAbilityData.addAbility(output, ability);
                outputs.add(output);
            }
            abilityRecipes.add(new AbilityIngotRecipe(
                    ability,
                    new ItemStack(ingot.get()),
                    outputs,
                    KogasaAbilityEffects.get(ability)));
        });

        registration.addRecipes(AbilityIngotCategory.RECIPE_TYPE, abilityRecipes);
    }

    @Override
    public void registerRecipeCatalysts(@NotNull IRecipeCatalystRegistration registration) {
        if (!ModList.get().isLoaded("jei")) return;
        // 将铁砧注册为“铁砧强化”类别的催化剂
        registration.addRecipeCatalyst(new ItemStack(Items.ANVIL), AnvilReinforcementCategory.RECIPE_TYPE);

        // 六种能力锭作为“能力锭”信息分类的催化剂
        KogasaAbilityRegistry.ingots().values().forEach(ingot ->
                registration.addRecipeCatalyst(new ItemStack(ingot.get()), AbilityIngotCategory.RECIPE_TYPE));
    }
}
