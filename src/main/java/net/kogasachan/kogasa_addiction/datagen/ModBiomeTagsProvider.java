package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.BiomeTagsProvider;
import net.minecraft.tags.BiomeTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 用 Forge tag 的 remove 能力生成“基础群系 - 海洋”组合 tag。
 * 这样 hut/koishi 仍覆盖全部主世界群系，只是不会在海洋生成。
 */
public class ModBiomeTagsProvider extends BiomeTagsProvider {

    public ModBiomeTagsProvider(PackOutput output,
                                CompletableFuture<HolderLookup.Provider> lookupProvider,
                                @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, KogasaAddiction.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        tag(ModBiomeTags.OVERWORLD_NO_OCEAN)
                .addTag(BiomeTags.IS_OVERWORLD)
                .remove(BiomeTags.IS_OCEAN);

        tag(ModBiomeTags.SNOWY_NO_OCEAN)
                .addTag(Tags.Biomes.IS_SNOWY)
                .remove(BiomeTags.IS_OCEAN);
    }
}
