package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 兼容附属的 datagen 入口：模型 / 方块状态 / 语言 / 标签 / 战利品表 / 世界生成。
 *
 * <p>帕秋莉的 {@code patchouli_books/**} 不属于原版 datagen 体系（Patchouli 自己的资源格式），
 * 仍然手写保留。
 */
@Mod.EventBusSubscriber(modid = KogasaCompatAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {

    private DataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        var lookupProvider = event.getLookupProvider();

        // 模型 / 方块状态
        generator.addProvider(event.includeClient(),
                new ModBlockStateProvider(output, existingFileHelper));
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, existingFileHelper));

        // 语言
        generator.addProvider(event.includeClient(),
                new ModLanguageProvider(output, "en_us", false));
        generator.addProvider(event.includeClient(),
                new ModLanguageProvider(output, "zh_cn", true));

        // 标签
        ModBlockTagsProvider blockTags = new ModBlockTagsProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, lookupProvider, blockTags.contentsGetter(), existingFileHelper));

        // 战利品表
        generator.addProvider(event.includeServer(), new ModLootTableProvider(output));

        // 世界生成：配置特征 → 放置特征 → 生物群系修饰器
        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.CONFIGURED_FEATURE, ModWorldgen::bootstrapConfiguredFeatures);
        builder.add(Registries.PLACED_FEATURE, ModWorldgen::bootstrapPlacedFeatures);
        builder.add(net.minecraftforge.registries.ForgeRegistries.Keys.BIOME_MODIFIERS, ModWorldgen::bootstrapBiomeModifiers);
        generator.addProvider(event.includeServer(),
                new net.minecraftforge.common.data.DatapackBuiltinEntriesProvider(
                        output, lookupProvider, builder, java.util.Set.of(KogasaCompatAddon.MODID)));
    }
}
