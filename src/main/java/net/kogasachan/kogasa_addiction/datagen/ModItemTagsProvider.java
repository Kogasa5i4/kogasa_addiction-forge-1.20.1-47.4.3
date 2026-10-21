package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ModItemTagsProvider extends ItemTagsProvider {

    public ModItemTagsProvider(PackOutput output,
                               CompletableFuture<HolderLookup.Provider> lookupProvider,
                               CompletableFuture<TagLookup<Block>> blockTags,
                               @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, blockTags, KogasaAddiction.MODID, existingFileHelper);
    }

    public static final TagKey<Item> ABILITY_INGOTS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "ability_ingots"));

    @Override
    protected void addTags(HolderLookup.@NotNull Provider provider) {
        // ===== 工具/武器：加入原版标签 =====
        this.tag(ItemTags.SWORDS).add(ModItems.KOGASA_SWORD.get());
        this.tag(ItemTags.PICKAXES).add(ModItems.KOGASA_PICKAXE.get());
        this.tag(ItemTags.AXES).add(ModItems.KOGASA_AXE.get());
        this.tag(ItemTags.SHOVELS).add(ModItems.KOGASA_SHOVEL.get());
        this.tag(ItemTags.HOES).add(ModItems.KOGASA_HOE.get());

        // ===== 护甲：加入 Forge 通用标签 =====
        this.tag(Tags.Items.ARMORS_HELMETS).add(ModItems.TENSHI_HELMET.get());
        this.tag(Tags.Items.ARMORS_CHESTPLATES).add(ModItems.TENSHI_CHESTPLATE.get());
        this.tag(Tags.Items.ARMORS_LEGGINGS).add(ModItems.TENSHI_LEGGINGS.get());
        this.tag(Tags.Items.ARMORS_BOOTS).add(ModItems.TENSHI_BOOTS.get());

        // ===== 箭矢：加入原版标签 =====
        this.tag(ItemTags.ARROWS)
                .add(ModItems.AYA_ARROW.get());

        // ===== 远程武器：加入 Forge 通用标签 =====
        this.tag(Tags.Items.TOOLS_BOWS).add(ModItems.AYA_BOW.get());
        this.tag(Tags.Items.TOOLS_CROSSBOWS).add(ModItems.AYA_CROSSBOW.get());

        // ==== 咲夜飞刀：加入 Forge 通用标签 =====
        this.tag(Tags.Items.TOOLS_TRIDENTS).add(ModItems.SAKUYA_KNIFE.get());
        this.tag(ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", "tools/knives")))
                .add(ModItems.SAKUYA_KNIFE.get());

        // ===== 能力锭：加入模组专属标签 =====
        this.tag(ABILITY_INGOTS)
                .add(ModItems.KOGASA_INGOT.get())
                .add(ModItems.AYA_INGOT.get())
                .add(ModItems.CIRNO_INGOT.get())
                .add(ModItems.KOISHI_INGOT.get())
                .add(ModItems.TENSHI_INGOT.get())
                .add(ModItems.SAKUYA_INGOT.get());
    }
}
