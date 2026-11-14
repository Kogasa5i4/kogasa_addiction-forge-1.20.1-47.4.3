package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * 结构用的“基础群系 - 排除群系”组合 tag。
 */
public final class ModBiomeTags {

    public static final TagKey<Biome> OVERWORLD_NO_OCEAN = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "overworld_no_ocean"));

    public static final TagKey<Biome> SNOWY_NO_OCEAN = TagKey.create(
            Registries.BIOME,
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "snowy_no_ocean"));

    private ModBiomeTags() {
    }
}
