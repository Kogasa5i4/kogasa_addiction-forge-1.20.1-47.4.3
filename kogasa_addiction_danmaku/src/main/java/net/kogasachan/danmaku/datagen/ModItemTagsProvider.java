package net.kogasachan.danmaku.datagen;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.kogasachan.danmaku.item.DanmakuItems;
import net.kogasachan.danmaku.tag.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/** 物品标签 */
public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output,
                               CompletableFuture<HolderLookup.Provider> lookupProvider,
                               CompletableFuture<TagsProvider.TagLookup<Block>> blockTags,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, KogasaDanmaku.MODID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        // 全部弹幕物品（29 种弹型 + 发射器）
        IntrinsicTagAppender<Item> danmakuItems = tag(ModTags.Items.DANMAKU_ITEMS);
        for (RegistryObject<Item> item : DanmakuItems.BULLET_ITEMS) {
            danmakuItems.add(item.get());
        }
        danmakuItems.add(DanmakuItems.DANMAKU_WAND.get());
    }
}
