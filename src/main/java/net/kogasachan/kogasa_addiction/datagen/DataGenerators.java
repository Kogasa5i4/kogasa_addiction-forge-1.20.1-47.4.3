package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;
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

        // 物品模型
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, existingFileHelper));

        // 进度
        generator.addProvider(event.includeServer(),
                new ForgeAdvancementProvider(output, event.getLookupProvider(),
                        existingFileHelper, List.of(new ModAdvancementGenerator())));
    }
}