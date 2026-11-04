package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.kogasachan.kogasa_addiction_compat.compat.CompatTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 物品标签：兼容白名单（空默认值）、Forge 通用矿石/宝石标签。
 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public static final TagKey<Item> FORGE_ORES =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ores"));
    public static final TagKey<Item> FORGE_ORES_RAINBOW =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ores/rainbow"));
    public static final TagKey<Item> FORGE_ORES_IN_GROUND_STONE =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "ores_in_ground/stone"));
    public static final TagKey<Item> FORGE_GEMS =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "gems"));
    public static final TagKey<Item> FORGE_GEMS_RAINBOW =
            TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "gems/rainbow"));

    public ModItemTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                               CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, KogasaCompatAddon.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        Item crystal = CompatRegistry.RAINBOW_CRYSTAL.get();

        // 兼容白名单：默认空，数据包作者可追加
        tag(CompatTags.COMPATIBLE);

        copy(ModBlockTagsProvider.FORGE_ORES, FORGE_ORES);
        copy(ModBlockTagsProvider.FORGE_ORES_RAINBOW, FORGE_ORES_RAINBOW);

        tag(FORGE_ORES_IN_GROUND_STONE).add(CompatRegistry.RAINBOW_ORE_ITEM.get());
        tag(FORGE_GEMS).add(crystal);
        tag(FORGE_GEMS_RAINBOW).add(crystal);
    }
}
