package net.kogasachan.kogasa_addiction.datagen;

import com.mojang.datafixers.util.Pair;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstapContext;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.VerticalAnchor;
import net.minecraft.world.level.levelgen.heightproviders.ConstantHeight;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureSet;
import net.minecraft.world.level.levelgen.structure.TerrainAdjustment;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.pools.StructurePoolElement;
import net.minecraft.world.level.levelgen.structure.pools.StructureTemplatePool;
import net.minecraft.world.level.levelgen.structure.structures.JigsawStructure;
import net.minecraft.world.level.levelgen.GenerationStep;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 世界生成 datagen：6 个结构、6 个结构集、6 个模板池。
 *
 * <p>全部由"结构名"驱动，参数集中在 {@link #DEFS}：生物群系、起始高度、是否投影到地表、
 * 随机间隔/间距/salt。新增结构只要在这张表加一行。
 */
public final class ModWorldgen {

    /** 结构定义：名称、生物群系（tag 以 # 开头）、起始高度、是否投影到 WORLD_SURFACE_WG */
    private record Def(String name, String biome, int startHeight, boolean projectToSurface,
                       int spacing, int separation, int salt) {
    }

    private static final List<Def> DEFS = List.of(
            new Def("cirno", "#forge:is_snowy", 0, true, 36, 9, 999951467),
            new Def("hut", "#minecraft:is_overworld", 0, true, 48, 12, 986751432),
            new Def("koishi", "#minecraft:is_overworld", 0, true, 32, 8, 102851455),
            new Def("mystia_izakaya", "#minecraft:is_forest", 0, true, 16, 4, 144233985),
            new Def("tree", "minecraft:cherry_grove", 0, true, 48, 12, 987652031),
            new Def("wall", "minecraft:lush_caves", -30, false, 16, 4, 789156432)
    );

    private ModWorldgen() {
    }

    // ===== 模板池 =====

    public static void bootstrapPools(BootstapContext<StructureTemplatePool> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);
        Holder<StructureTemplatePool> empty = pools.getOrThrow(
                ResourceKey.create(Registries.TEMPLATE_POOL, ResourceLocation.withDefaultNamespace("empty")));

        for (Def def : DEFS) {
            StructurePoolElement element = StructurePoolElement
                    .single(KogasaAddiction.MODID + ":" + def.name())
                    .apply(StructureTemplatePool.Projection.RIGID);
            StructureTemplatePool pool = new StructureTemplatePool(empty, List.of(Pair.of(element, 1)));
            context.register(poolKey(def.name()), pool);
        }
    }

    // ===== 结构 =====

    public static void bootstrapStructures(BootstapContext<Structure> context) {
        HolderGetter<StructureTemplatePool> pools = context.lookup(Registries.TEMPLATE_POOL);

        for (Def def : DEFS) {
            HolderSet<Biome> biomes = biomes(context, def.biome());
            Structure.StructureSettings settings = new Structure.StructureSettings(
                    biomes, Map.of(),
                    GenerationStep.Decoration.SURFACE_STRUCTURES,
                    TerrainAdjustment.BEARD_THIN);

            JigsawStructure structure = new JigsawStructure(
                    settings,
                    pools.getOrThrow(poolKey(def.name())),
                    Optional.empty(),
                    1,
                    ConstantHeight.of(VerticalAnchor.absolute(def.startHeight())),
                    false,
                    def.projectToSurface() ? Optional.of(Heightmap.Types.WORLD_SURFACE_WG) : Optional.empty(),
                    80);

            context.register(structureKey(def.name()), structure);
        }
    }

    // ===== 结构集 =====

    public static void bootstrapStructureSets(BootstapContext<StructureSet> context) {
        HolderGetter<Structure> structures = context.lookup(Registries.STRUCTURE);

        for (Def def : DEFS) {
            StructureSet set = new StructureSet(
                    List.of(StructureSet.entry(structures.getOrThrow(structureKey(def.name())), 1)),
                    new RandomSpreadStructurePlacement(def.spacing(), def.separation(),
                            RandomSpreadType.LINEAR, def.salt()));
            context.register(setKey(def.name()), set);
        }
    }

    // ===== 工具 =====

    private static HolderSet<Biome> biomes(BootstapContext<?> context, String reference) {
        HolderGetter<Biome> biomes = context.lookup(Registries.BIOME);
        if (reference.startsWith("#")) {
            return biomes.getOrThrow(TagKey.create(Registries.BIOME, parseId(reference.substring(1))));
        }
        return HolderSet.direct(biomes.getOrThrow(ResourceKey.create(Registries.BIOME, parseId(reference))));
    }

    private static ResourceLocation parseId(String value) {
        int idx = value.indexOf(':');
        if (idx < 0) {
            return ResourceLocation.fromNamespaceAndPath("minecraft", value);
        }
        return ResourceLocation.fromNamespaceAndPath(value.substring(0, idx), value.substring(idx + 1));
    }

    private static ResourceKey<StructureTemplatePool> poolKey(String name) {
        return ResourceKey.create(Registries.TEMPLATE_POOL,
                ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, name + "/start_pool"));
    }

    private static ResourceKey<Structure> structureKey(String name) {
        return ResourceKey.create(Registries.STRUCTURE,
                ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, name));
    }

    private static ResourceKey<StructureSet> setKey(String name) {
        return ResourceKey.create(Registries.STRUCTURE_SET,
                ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, name));
    }
}
