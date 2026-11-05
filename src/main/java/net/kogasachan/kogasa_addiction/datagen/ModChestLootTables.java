package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.loot.KogasaLockedEquipmentFunction;
import net.minecraft.ChatFormatting;
import net.minecraft.data.loot.LootTableSubProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.EnchantRandomlyFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.functions.SetNameFunction;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.NumberProvider;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;

/**
 * 本模组所有宝箱战利品表（由 datagen 生成到 {@code data/kogasa_addiction/loot_tables/chests/}）。
 *
 * <p>之前这些 JSON 是手写的；迁移过来后增删物品只需要改这里。
 */
public class ModChestLootTables implements LootTableSubProvider {

    @Override
    public void generate(BiConsumer<ResourceLocation, LootTable.Builder> output) {
        output.accept(id("cirno"), chest()
                .withPool(pool(UniformGenerator.between(5, 8))
                        .add(count(Items.ICE, 20, 4, 12))
                        .add(count(Items.PACKED_ICE, 12, 2, 6))
                        .add(count(Items.SNOWBALL, 15, 4, 16))
                        .add(count(Items.BLUE_ICE, 5, 1, 3))
                        .add(item(Items.POWDER_SNOW_BUCKET, 6))
                        .add(item(ModItems.CIRNO_INGOT.get(), 1)))
                .withPool(pool(ConstantValue.exactly(3))
                        .add(empty(6))
                        .add(count(Items.DIAMOND, 4, 1, 3))
                        .add(count(Items.LAPIS_LAZULI, 6, 4, 12))
                        .add(count(Items.PRISMARINE_CRYSTALS, 5, 2, 5))
                        .add(item(Items.HEART_OF_THE_SEA, 1))
                        .add(count(ModItems.CIRNO_INGOT.get(), 9, 1, 3)))
                .withPool(pool(ConstantValue.exactly(1))
                        .add(empty(5))
                        .add(named(count(Items.BLUE_ICE, 1, 1, 1),
                                "item.kogasa_addiction.cirno_treasure", ChatFormatting.AQUA))));

        output.accept(id("cirno_barrel"), chest()
                .withPool(pool(UniformGenerator.between(3, 7))
                        .add(count(Items.ICE, 20, 2, 6))
                        .add(count(Items.PACKED_ICE, 12, 1, 2))
                        .add(count(Items.SNOWBALL, 15, 1, 4))
                        .add(item(ModItems.CIRNO_INGOT.get(), 1))));

        output.accept(id("daiyousei"), chest()
                .withPool(pool(UniformGenerator.between(5, 8))
                        .add(count(Items.POPPY, 12, 2, 6))
                        .add(count(Items.DANDELION, 12, 2, 6))
                        .add(count(Items.CORNFLOWER, 8, 1, 4))
                        .add(count(Items.OXEYE_DAISY, 8, 1, 4))
                        .add(count(Items.HONEY_BOTTLE, 6, 1, 3))
                        .add(count(Items.HONEYCOMB, 6, 2, 5))
                        .add(count(Items.GLOW_BERRIES, 6, 2, 6))
                        .add(count(Items.LILY_OF_THE_VALLEY, 5, 1, 3))
                        .add(count(Items.AZALEA_LEAVES, 6, 4, 10))
                        .add(count(Items.MOSS_BLOCK, 5, 2, 6)))
                .withPool(pool(ConstantValue.exactly(3))
                        .add(empty(6))
                        .add(count(Items.HONEY_BLOCK, 5, 1, 3))
                        .add(count(Items.EMERALD, 6, 2, 6))
                        .add(count(Items.GLOWSTONE_DUST, 5, 4, 12))
                        .add(item(Items.SPORE_BLOSSOM, 3))
                        .add(count(Items.BIG_DRIPLEAF, 3, 1, 2)))
                .withPool(pool(ConstantValue.exactly(1))
                        .add(empty(4))
                        .add(named(item(Items.TOTEM_OF_UNDYING, 2),
                                "item.kogasa_addiction.daiyousei_bomb", ChatFormatting.GREEN)))
                .withPool(pool(ConstantValue.exactly(1))
                        .add(empty(2))
                        .add(item(ModItems.CIRNO_INGOT.get(), 1))));

        output.accept(id("hat"), chest()
                .withPool(pool(UniformGenerator.between(1, 2))
                        .add(count(Items.IRON_INGOT, 12, 2, 6))
                        .add(count(Items.GOLD_INGOT, 10, 1, 4))
                        .add(count(Items.DIAMOND, 5, 1, 2))
                        .add(count(Items.EMERALD, 5, 1, 3))
                        .add(item(Items.IRON_PICKAXE, 1))
                        .add(item(Items.IRON_SWORD, 1)))
                .withPool(pool(UniformGenerator.between(1, 3))
                        .add(count(Items.BREAD, 15, 2, 6))
                        .add(count(Items.COOKED_BEEF, 12, 1, 4))
                        .add(count(Items.GOLDEN_CARROT, 6, 1, 3))
                        .add(count(Items.PUMPKIN_PIE, 8, 1, 3)))
                .withPool(pool(UniformGenerator.between(1, 2))
                        .add(count(Items.ENDER_PEARL, 8, 1, 3))
                        .add(count(Items.END_STONE, 10, 4, 16))
                        .add(count(Items.CHORUS_FRUIT, 6, 1, 4))
                        .add(count(Items.PURPUR_BLOCK, 5, 2, 8))
                        .add(count(Items.SHULKER_SHELL, 4, 1, 2)))
                .withPool(pool(ConstantValue.exactly(1))
                        .add(empty(80))
                        .add(item(Items.ENDER_EYE, 40))
                        .add(item(Items.ENCHANTED_GOLDEN_APPLE, 10))
                        .add(item(Items.DRAGON_HEAD, 1))));

        output.accept(id("hut"), chest()
                .withPool(pool(UniformGenerator.between(5, 8))
                        .add(count(Items.IRON_INGOT, 10, 1, 4))
                        .add(count(Items.EMERALD, 5, 1, 3))
                        .add(count(ModItems.KOGASA_INGOT.get(), 2, 1, 2))
                        .add(count(Items.BREAD, 15, 2, 6))
                        .add(count(Items.APPLE, 10, 1, 3))
                        .add(count(Items.COOKED_BEEF, 8, 1, 3))
                        .add(count(Items.TORCH, 12, 4, 12))
                        .add(count(Items.STICK, 8, 4, 10))));

        output.accept(id("hut_barrel"), chest()
                .withPool(pool(UniformGenerator.between(6, 12))
                        .add(count(Items.DIAMOND, 8, 1, 3))
                        .add(count(Items.NETHERITE_SCRAP, 3, 1, 2))
                        .add(item(Items.GOLDEN_APPLE, 6))
                        .add(item(Items.ENCHANTED_GOLDEN_APPLE, 1))
                        .add(item(ModItems.FUMO.get(), 1))));

        output.accept(id("koishi"), chest()
                .withPool(pool(ConstantValue.exactly(1))
                        .add(item(Items.GOLDEN_SWORD, 2))
                        .add(item(Items.IRON_SWORD, 6))
                        .add(item(Items.DIAMOND_SWORD, 3))
                        .add(item(Items.NETHERITE_SWORD, 1)))
                .withPool(pool(ConstantValue.exactly(1))
                        .add(count(ModItems.KOISHI_INGOT.get(), 1, 1, 4)))
                .withPool(pool(UniformGenerator.between(2, 3))
                        .add(count(Items.BONE, 10, 1, 4))
                        .add(count(Items.STRING, 10, 1, 4))
                        .add(count(Items.EGG, 8, 1, 3))
                        .add(count(Items.GLOWSTONE_DUST, 8, 2, 6))
                        .add(count(Items.NETHERRACK, 6, 4, 10))
                        .add(count(Items.REDSTONE, 6, 2, 8))
                        .add(count(Items.POPPY, 5, 1, 3)))
                .withPool(pool(ConstantValue.exactly(3))
                        .add(empty(30))
                        .add(count(Items.EXPERIENCE_BOTTLE, 15, 2, 6))
                        .add(count(Items.DIAMOND, 18, 1, 3))
                        .add(count(Items.ANCIENT_DEBRIS, 10, 1, 2))
                        .add(count(Items.NETHERITE_SCRAP, 6, 1, 4))
                        .add(count(Items.NETHERITE_INGOT, 2, 1, 8))
                        .add(count(Items.NETHERITE_BLOCK, 1, 1, 16))
                        .add(count(Items.NETHER_STAR, 4, 1, 4))));

        output.accept(id("mystia"), chest()
                .withPool(pool(UniformGenerator.between(3, 4))
                        .add(count(Items.GOLDEN_CARROT, 10, 2, 4))
                        .add(count(Items.PUMPKIN_PIE, 10, 1, 3))
                        .add(count(Items.COOKED_BEEF, 10, 2, 4))
                        .add(count(Items.BAKED_POTATO, 8, 2, 5))
                        .add(item(Items.CAKE, 5))
                        .add(count(Items.COOKIE, 8, 4, 8))
                        .add(item(Items.GOLDEN_APPLE, 3))
                        .add(item(Items.ENCHANTED_GOLDEN_APPLE, 1))));

        output.accept(id("mystia_cooked"), chest()
                .withPool(pool(UniformGenerator.between(6, 12))
                        .add(count(Items.COOKED_BEEF, 12, 2, 4))
                        .add(count(Items.COOKED_PORKCHOP, 12, 2, 4))
                        .add(count(Items.COOKED_MUTTON, 8, 1, 3))
                        .add(count(Items.COOKED_RABBIT, 6, 1, 2))
                        .add(count(Items.COOKED_COD, 8, 1, 3))
                        .add(count(Items.COOKED_SALMON, 6, 1, 2))
                        .add(count(Items.BAKED_POTATO, 6, 2, 4))));

        output.accept(id("mystia_feature"), chest()
                .withPool(pool(UniformGenerator.between(6, 12))
                        .add(item(Items.ENDER_PEARL, 10))
                        .add(item(Items.BLAZE_POWDER, 10))
                        .add(item(Items.GHAST_TEAR, 6))
                        .add(item(Items.PHANTOM_MEMBRANE, 8))
                        .add(item(Items.MAGMA_CREAM, 8))
                        .add(item(Items.SLIME_BALL, 10))
                        .add(item(Items.PRISMARINE_SHARD, 8))
                        .add(item(Items.PRISMARINE_CRYSTALS, 6))
                        .add(item(Items.NAUTILUS_SHELL, 4))
                        .add(item(Items.RABBIT_FOOT, 5))
                        .add(item(Items.SPIDER_EYE, 8))
                        .add(item(Items.BONE, 8)))
                .withPool(pool(ConstantValue.exactly(3))
                        .add(item(Items.SUGAR, 10))
                        .add(item(Items.COCOA_BEANS, 8))
                        .add(item(Items.GUNPOWDER, 8))
                        .add(item(Items.GLOWSTONE_DUST, 8))
                        .add(item(Items.INK_SAC, 6))
                        .add(item(Items.GLOW_INK_SAC, 4))
                        .add(item(Items.HONEYCOMB, 6))
                        .add(item(Items.SUGAR_CANE, 8))
                        .add(item(Items.WHEAT, 8))
                        .add(item(Items.EGG, 6)))
                .withPool(pool(ConstantValue.exactly(2))
                        .add(empty(6))
                        .add(item(Items.PUFFERFISH, 6))
                        .add(item(Items.POISONOUS_POTATO, 6))
                        .add(item(Items.FERMENTED_SPIDER_EYE, 4))
                        .add(item(Items.DRAGON_BREATH, 2))
                        .add(item(Items.ENDER_EYE, 2))
                        .add(item(Items.SHULKER_SHELL, 2))));

        output.accept(id("mystia_raw"), chest()
                .withPool(pool(UniformGenerator.between(6, 12))
                        .add(count(Items.BEEF, 12, 2, 4))
                        .add(count(Items.PORKCHOP, 12, 2, 4))
                        .add(count(Items.MUTTON, 8, 1, 3))
                        .add(count(Items.RABBIT, 6, 1, 2))
                        .add(count(Items.COD, 8, 1, 3))
                        .add(count(Items.SALMON, 6, 1, 2))));

        output.accept(id("mystia_veg"), chest()
                .withPool(pool(UniformGenerator.between(6, 12))
                        .add(count(Items.CARROT, 12, 2, 5))
                        .add(count(Items.POTATO, 12, 2, 5))
                        .add(count(Items.APPLE, 10, 1, 4))
                        .add(count(Items.MELON_SLICE, 10, 2, 6))
                        .add(count(Items.SWEET_BERRIES, 8, 2, 5))
                        .add(count(Items.BEETROOT, 6, 1, 3))
                        .add(count(Items.PUMPKIN, 4, 1, 2))
                        .add(item(Items.GOLDEN_APPLE, 1))));

        output.accept(id("satori"), chest()
                .withPool(pool(UniformGenerator.between(2, 3))
                        .add(count(Items.BOOK, 12, 1, 4))
                        .add(count(Items.PAPER, 10, 2, 8))
                        .add(count(Items.INK_SAC, 8, 1, 3))
                        .add(item(Items.WRITABLE_BOOK, 6))
                        .add(count(Items.FEATHER, 8, 2, 6)))
                .withPool(pool(UniformGenerator.between(1, 2))
                        .add(enchantMending(Items.ENCHANTED_BOOK, 10)))
                .withPool(pool(UniformGenerator.between(1, 2))
                        .add(count(Items.SOUL_SAND, 10, 2, 6))
                        .add(count(Items.GLOWSTONE, 8, 1, 4))
                        .add(count(Items.NETHER_BRICKS, 6, 4, 12))
                        .add(count(Items.QUARTZ, 6, 2, 6)))
                .withPool(pool(ConstantValue.exactly(1))
                        .add(empty(3))
                        .add(item(Items.GOLDEN_APPLE, 2))
                        .add(item(Items.ENCHANTED_GOLDEN_APPLE, 1)))
                .withPool(pool(ConstantValue.exactly(1))
                        .add(count(ModItems.KOISHI_INGOT.get(), 1, 2, 6))));

        output.accept(id("tree"), chest()
                .withPool(pool(UniformGenerator.between(12, 25))
                        .add(count(Items.DIAMOND_BLOCK, 10, 1, 4))
                        .add(count(Items.EMERALD_BLOCK, 15, 1, 3))
                        .add(count(Items.GOLDEN_APPLE, 10, 1, 3))
                        .add(count(Items.ENCHANTED_GOLDEN_APPLE, 8, 1, 3))
                        .add(item(Items.NETHERITE_BLOCK, 3))
                        .add(count(ModItems.KOGASA_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.AYA_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.KOISHI_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.CIRNO_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.TENSHI_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.SAKUYA_INGOT.get(), 5, 3, 5))
                        .add(lockedEquipment(Items.AIR, 2))));

        output.accept(id("wall"), chest()
                .withPool(pool(UniformGenerator.between(3, 8))
                        .add(count(Items.GOLD_INGOT, 20, 1, 4))
                        .add(count(Items.ENDER_PEARL, 15, 1, 2))
                        .add(count(Items.LAPIS_LAZULI, 18, 2, 6))
                        .add(count(Items.NETHER_WART, 14, 2, 5))
                        .add(count(Items.BLAZE_ROD, 12, 1, 2))
                        .add(count(Items.BLAZE_POWDER, 16, 2, 6))
                        .add(item(Items.WITHER_SKELETON_SKULL, 1))
                        .add(count(ModItems.KOGASA_INGOT.get(), 2, 1, 3))
                        .add(count(ModItems.AYA_INGOT.get(), 2, 1, 3))
                        .add(count(ModItems.KOISHI_INGOT.get(), 2, 1, 3))
                        .add(count(ModItems.CIRNO_INGOT.get(), 2, 1, 3))
                        .add(count(ModItems.TENSHI_INGOT.get(), 2, 1, 3))
                        .add(count(ModItems.SAKUYA_INGOT.get(), 2, 1, 3))
                        .add(lockedEquipment(Items.AIR, 2))));

        output.accept(id("wall_barrel"), chest()
                .withPool(pool(UniformGenerator.between(5, 16))
                        .add(item(Items.DIAMOND_BLOCK, 3))
                        .add(item(Items.EMERALD_BLOCK, 5))
                        .add(item(Items.NETHERITE_INGOT, 2))
                        .add(count(Items.ANCIENT_DEBRIS, 4, 1, 2))
                        .add(item(Items.NETHERITE_BLOCK, 1))
                        .add(named(item(Items.COAL_BLOCK, 6), "block.minecraft.netherite_block", null))
                        .add(named(item(Items.OBSIDIAN, 6), "block.minecraft.netherite_block", null))
                        .add(count(ModItems.KOGASA_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.AYA_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.KOISHI_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.CIRNO_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.TENSHI_INGOT.get(), 5, 3, 5))
                        .add(count(ModItems.SAKUYA_INGOT.get(), 5, 3, 5))
                        .add(lockedEquipment(Items.AIR, 2))));
    }

    // ===== 构建辅助 =====

    private static ResourceLocation id(String name) {
        return ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "chests/" + name);
    }

    private static LootTable.Builder chest() {
        return LootTable.lootTable().setParamSet(LootContextParamSets.CHEST);
    }

    private static LootPool.Builder pool(NumberProvider rolls) {
        return LootPool.lootPool().setRolls(rolls);
    }

    private static LootPoolSingletonContainer.Builder<?> item(ItemLike item, int weight) {
        return LootItem.lootTableItem(item).setWeight(weight);
    }

    private static LootPoolSingletonContainer.Builder<?> count(ItemLike item, int weight, int min, int max) {
        LootPoolSingletonContainer.Builder<?> builder = LootItem.lootTableItem(item).setWeight(weight);
        builder.apply(SetItemCountFunction.setCount(UniformGenerator.between(min, max)));
        return builder;
    }

    private static LootPoolSingletonContainer.Builder<?> empty(int weight) {
        return EmptyLootItem.emptyItem().setWeight(weight);
    }

    private static LootPoolSingletonContainer.Builder<?> named(LootPoolSingletonContainer.Builder<?> builder,
                                                               String translateKey, @Nullable ChatFormatting color) {
        Style style = Style.EMPTY.withItalic(false);
        if (color != null) {
            style = style.withColor(color);
        }
        builder.apply(SetNameFunction.setName(Component.translatable(translateKey).withStyle(style)));
        return builder;
    }

    private static LootPoolSingletonContainer.Builder<?> enchantMending(Item item, int weight) {
        LootPoolSingletonContainer.Builder<?> builder = LootItem.lootTableItem(item).setWeight(weight);
        builder.apply(EnchantRandomlyFunction.randomEnchantment().withEnchantment(Enchantments.MENDING));
        return builder;
    }

    private static LootPoolSingletonContainer.Builder<?> lockedEquipment(Item item, int weight) {
        LootPoolSingletonContainer.Builder<?> builder = LootItem.lootTableItem(item).setWeight(weight);
        builder.apply(() -> new KogasaLockedEquipmentFunction());
        return builder;
    }
}
