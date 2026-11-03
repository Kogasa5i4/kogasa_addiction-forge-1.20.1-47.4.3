package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistrySetBuilder;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.DatapackBuiltinEntriesProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // 方块标签
        ModBlockTagsProvider blockTags = new ModBlockTagsProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        CompletableFuture<TagsProvider.TagLookup<Block>> blockTagLookup = blockTags.contentsGetter();

        // 物品标签
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, lookupProvider, blockTagLookup, existingFileHelper));

        // 配方
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output));

        // 战利品表（宝箱）
        generator.addProvider(event.includeServer(), new ModLootTableProvider(output));

        // 物品模型
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, existingFileHelper));

        // 进度
        generator.addProvider(event.includeServer(),
                new ForgeAdvancementProvider(output, event.getLookupProvider(),
                        existingFileHelper, List.of(new ModAdvancementGenerator())));

        // 伤害类型（同一注册表只能 add 一次，两个类型写在一个 bootstrap 里）
        RegistrySetBuilder builder = new RegistrySetBuilder();
        builder.add(Registries.DAMAGE_TYPE, (context) -> {
            context.register(
                    ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "koishi_ranged")),
                    new DamageType("koishi_ranged", DamageScaling.NEVER, 0.0F)
            );
            // 天子强制命中：原先借用 indirectMagic，死亡消息是通用的魔法击杀
            context.register(
                    ResourceKey.create(Registries.DAMAGE_TYPE, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "tenshi_forced")),
                    new DamageType("tenshi_forced", DamageScaling.NEVER, 0.0F)
            );
        });

        // 世界生成：模板池 → 结构 → 结构集（顺序不能反，后面的 bootstrap 需要前面的 lookup）
        builder.add(Registries.TEMPLATE_POOL, ModWorldgen::bootstrapPools);
        builder.add(Registries.STRUCTURE, ModWorldgen::bootstrapStructures);
        builder.add(Registries.STRUCTURE_SET, ModWorldgen::bootstrapStructureSets);

        // 1. 生成伤害类型 JSON
        generator.addProvider(event.includeServer(),
                new DatapackBuiltinEntriesProvider(output, lookupProvider, builder, Set.of(KogasaAddiction.MODID)));

        // 2. 用自定义 Provider 生成 Tag JSON（跳过 TagsProvider 的报错）
        generator.addProvider(event.includeServer(),
                new ModDamageTypeTagDataProvider(output));
    }
}