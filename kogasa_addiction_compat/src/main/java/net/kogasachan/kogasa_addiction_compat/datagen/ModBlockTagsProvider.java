package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/** 方块标签：挖掘工具、挖掘等级、Forge 通用矿石标签。 */
public class ModBlockTagsProvider extends BlockTagsProvider {

    public static final TagKey<Block> FORGE_ORES =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores"));
    public static final TagKey<Block> FORGE_ORES_RAINBOW =
            TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores/rainbow"));

    public ModBlockTagsProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, KogasaCompatAddon.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        Block ore = CompatRegistry.RAINBOW_ORE.get();
        tag(BlockTags.MINEABLE_WITH_PICKAXE).add(ore);
        tag(BlockTags.NEEDS_STONE_TOOL).add(ore);
        tag(FORGE_ORES).add(ore);
        tag(FORGE_ORES_RAINBOW).add(ore);
    }
}
