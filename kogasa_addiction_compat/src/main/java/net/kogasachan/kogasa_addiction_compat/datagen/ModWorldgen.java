package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.feature.ConfiguredFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.heightproviders.TrapezoidHeight;
import net.minecraft.world.level.levelgen.placement.*;
import net.minecraft.world.level.levelgen.structure.templatesystem.TagMatchTest;
import net.minecraftforge.common.world.ForgeBiomeModifiers;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

/**
 * 彩虹矿石的世界生成：配置特征 → 放置特征 → 生物群系修饰器。
 *
 * <p>完全套用铁矿石：矿团大小 9、替换石头/深板岩；每区块 45 次（铁矿上部的一半），
 * 高度梯形分布 128~320。
 */
public final class ModWorldgen {

    public static final ResourceKey<ConfiguredFeature<?, ?>> RAINBOW_ORE =
            key(Registries.CONFIGURED_FEATURE, "rainbow_ore");
    public static final ResourceKey<PlacedFeature> RAINBOW_ORE_PLACED =
            key(Registries.PLACED_FEATURE, "rainbow_ore");
    public static final ResourceKey<net.minecraftforge.common.world.BiomeModifier> ADD_RAINBOW_ORE =
            ResourceKey.create(ForgeRegistries.Keys.BIOME_MODIFIERS,
                    ResourceLocation.fromNamespaceAndPath(KogasaCompatAddon.MODID, "add_rainbow_ore"));

    private ModWorldgen() {
    }

    public static void bootstrapConfiguredFeatures(BootstapContext<ConfiguredFeature<?, ?>> context) {
        var ore = CompatRegistry.RAINBOW_ORE.get().defaultBlockState();
        context.register(RAINBOW_ORE, new ConfiguredFeature<>(Feature.ORE,
                new OreConfiguration(List.of(
                        OreConfiguration.target(new TagMatchTest(BlockTags.STONE_ORE_REPLACEABLES), ore),
                        OreConfiguration.target(new TagMatchTest(BlockTags.DEEPSLATE_ORE_REPLACEABLES), ore)
                ), 9)));
    }

    public static void bootstrapPlacedFeatures(BootstapContext<PlacedFeature> context) {
        var configured = context.lookup(Registries.CONFIGURED_FEATURE).getOrThrow(RAINBOW_ORE);
        context.register(RAINBOW_ORE_PLACED, new PlacedFeature(configured, List.of(
                CountPlacement.of(45),
                InSquarePlacement.spread(),
                HeightRangePlacement.of(TrapezoidHeight.of(VerticalAnchor.absolute(128), VerticalAnchor.absolute(320))),
                BiomeFilter.biome()
        )));
    }

    public static void bootstrapBiomeModifiers(BootstapContext<net.minecraftforge.common.world.BiomeModifier> context) {
        HolderSet<Biome> overworld = context.lookup(Registries.BIOME)
                .getOrThrow(TagKey.create(Registries.BIOME, ResourceLocation.withDefaultNamespace("is_overworld")));
        HolderSet<PlacedFeature> features = HolderSet.direct(
                context.lookup(Registries.PLACED_FEATURE).getOrThrow(RAINBOW_ORE_PLACED));

        context.register(ADD_RAINBOW_ORE, new ForgeBiomeModifiers.AddFeaturesBiomeModifier(
                overworld, features, GenerationStep.Decoration.UNDERGROUND_ORES));
    }

    private static <T> ResourceKey<T> key(net.minecraft.resources.ResourceKey<? extends net.minecraft.core.Registry<T>> registry, String name) {
        return ResourceKey.create(registry, ResourceLocation.fromNamespaceAndPath(KogasaCompatAddon.MODID, name));
    }
}
