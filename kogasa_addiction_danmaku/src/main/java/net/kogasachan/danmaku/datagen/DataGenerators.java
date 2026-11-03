package net.kogasachan.danmaku.datagen;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.concurrent.CompletableFuture;

/**
 * 附属模组的 datagen 入口。
 *
 * <p>运行：{@code gradlew runData}（产物输出到 {@code src/generated/resources}）。
 * 目前包含：
 * <ul>
 *   <li>物品模型（29 种弹幕物品共用一张白色图标 + 发射器模型）；</li>
 *   <li>物品标签（{@code kogasa_addiction_danmaku:danmaku_items}）；</li>
 *   <li>语言文件（en_us / zh_cn，弹幕物品名由 {@code DanmakuType} 提供）。</li>
 * </ul>
 * 后续要加配方/方块/音效字幕等，直接在这里追加 provider 即可。
 */
@Mod.EventBusSubscriber(modid = KogasaDanmaku.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DataGenerators {

    private DataGenerators() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();

        // 方块标签（当前没有方块，留空；物品标签需要它的 lookup）
        ModBlockTagsProvider blockTags = new ModBlockTagsProvider(output, lookupProvider, existingFileHelper);
        generator.addProvider(event.includeServer(), blockTags);
        CompletableFuture<TagsProvider.TagLookup<Block>> blockTagLookup = blockTags.contentsGetter();

        // 物品标签
        generator.addProvider(event.includeServer(),
                new ModItemTagsProvider(output, lookupProvider, blockTagLookup, existingFileHelper));

        // 物品模型
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, existingFileHelper));

        // 语言文件
        generator.addProvider(event.includeClient(),
                new ModLanguageProvider(output, "en_us", false));
        generator.addProvider(event.includeClient(),
                new ModLanguageProvider(output, "zh_cn", true));
    }
}
