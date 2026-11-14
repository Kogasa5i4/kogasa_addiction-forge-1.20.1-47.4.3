package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.tags.IReverseTag;
import net.minecraftforge.registries.tags.ITagManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

/**
 * /kogasa tag 【block】 —— 只读：列出手持物品/方块的全部数据包 tag。
 */
public final class KogasaTagCommands {

    private KogasaTagCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("tag")
                .executes(KogasaTagCommands::heldTags)
                .then(Commands.literal("block")
                        .executes(KogasaTagCommands::lookedBlockTags)));
    }

    private static int heldTags(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireAnyHeldItem(ctx);
        if (held.isEmpty()) return 0;

        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(held.getItem());
        KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                "commands.kogasa_addiction.tag.header", held.getHoverName(), itemId.toString()));

        List<ResourceLocation> itemTags = itemTags(held.getItem())
                .map(TagKey::location)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .toList();
        KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                "commands.kogasa_addiction.tag.item", itemTags.size()));
        if (itemTags.isEmpty()) {
            KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                    "commands.kogasa_addiction.tag.none"));
        } else {
            for (ResourceLocation id : itemTags) {
                KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.literal("  §7#§f" + id));
            }
        }

        if (held.getItem() instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            List<ResourceLocation> blockTags = blockTags(block)
                    .map(TagKey::location)
                    .sorted(Comparator.comparing(ResourceLocation::toString))
                    .toList();
            KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                    "commands.kogasa_addiction.tag.block",
                    ForgeRegistries.BLOCKS.getKey(block).toString(), blockTags.size()));
            if (blockTags.isEmpty()) {
                KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                        "commands.kogasa_addiction.tag.none"));
            } else {
                for (ResourceLocation id : blockTags) {
                    KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.literal("  §7#§f" + id));
                }
            }
        }
        return 1;
    }

    private static int lookedBlockTags(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HitResult hit = player.pick(5.0D, 0.0F, false);
        if (!(hit instanceof BlockHitResult blockHit) || hit.getType() != HitResult.Type.BLOCK) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.tag.no_block"), false);
            return 0;
        }

        BlockState state = player.level().getBlockState(blockHit.getBlockPos());
        Block block = state.getBlock();
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(block);
        KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                "commands.kogasa_addiction.tag.header_block", blockId.toString(), blockHit.getBlockPos().toShortString()));

        List<ResourceLocation> tags = new ArrayList<>(blockTags(block)
                .map(TagKey::location).toList());
        tags.sort(Comparator.comparing(ResourceLocation::toString));
        KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                "commands.kogasa_addiction.tag.block", blockId.toString(), tags.size()));
        if (tags.isEmpty()) {
            KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.translatable(
                    "commands.kogasa_addiction.tag.none"));
        } else {
            for (ResourceLocation id : tags) {
                KogasaCommandSupport.sendCopyable(ctx.getSource(), Component.literal("  §7#§f" + id));
            }
        }
        return 1;
    }

    private static Stream<TagKey<Item>> itemTags(Item item) {
        ITagManager<Item> manager = ForgeRegistries.ITEMS.tags();
        if (manager == null) return Stream.empty();
        return manager.getReverseTag(item)
                .map(IReverseTag::getTagKeys)
                .orElseGet(Stream::empty);
    }

    private static Stream<TagKey<Block>> blockTags(Block block) {
        ITagManager<Block> manager = ForgeRegistries.BLOCKS.tags();
        if (manager == null) return Stream.empty();
        return manager.getReverseTag(block)
                .map(IReverseTag::getTagKeys)
                .orElseGet(Stream::empty);
    }
}
