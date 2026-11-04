package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * {@code /kogasa nbt [路径]} —— 输出主手物品的 NBT（任意物品，主手为空则不输出）。
 *
 * <p>每一行输出都带"点击复制"：鼠标悬停有提示，点击把整行内容复制到剪贴板。
 * 路径用点分隔（如 {@code KogasaEquipment.Growth}），列表用数字下标（如 {@code Enchantments.0.id}）。
 */
public final class KogasaNbtCommands {

    private KogasaNbtCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("nbt")
                .executes(KogasaNbtCommands::dump)
                .then(Commands.argument("path", StringArgumentType.greedyString())
                        .executes(KogasaNbtCommands::dumpPath)));
    }

    /** 输出物品 id、数量与完整 NBT */
    private static int dump(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack held = KogasaCommandSupport.requireAnyHeldItem(ctx);
        if (held.isEmpty()) {
            return 0;
        }

        CommandSourceStack source = ctx.getSource();
        String id = String.valueOf(ForgeRegistries.ITEMS.getKey(held.getItem()));
        KogasaCommandSupport.sendCopyable(source, Component.translatable(
                "commands.kogasa_addiction.nbt.item",
                Component.literal(id),
                Component.literal(String.valueOf(held.getCount()))));

        CompoundTag tag = held.getTag();
        if (tag == null || tag.isEmpty()) {
            KogasaCommandSupport.sendCopyable(source,
                    Component.translatable("commands.kogasa_addiction.nbt.empty"));
        } else {
            KogasaCommandSupport.sendCopyable(source, Component.translatable(
                    "commands.kogasa_addiction.nbt.tag",
                    Component.literal(tag.getAsString())));
        }
        return 1;
    }

    /** 只输出指定路径的 NBT 值 */
    private static int dumpPath(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ItemStack held = KogasaCommandSupport.requireAnyHeldItem(ctx);
        if (held.isEmpty()) {
            return 0;
        }

        String path = StringArgumentType.getString(ctx, "path");
        CommandSourceStack source = ctx.getSource();

        CompoundTag root = held.getTag();
        if (root == null) {
            return missing(source, path);
        }

        Tag current = root;
        for (String part : path.split("\\.")) {
            if (part.isEmpty()) {
                continue;
            }
            if (current instanceof CompoundTag compound) {
                if (!compound.contains(part)) {
                    return missing(source, path);
                }
                current = compound.get(part);
            } else if (current instanceof ListTag list) {
                int index;
                try {
                    index = Integer.parseInt(part);
                } catch (NumberFormatException ex) {
                    return missing(source, path);
                }
                if (index < 0 || index >= list.size()) {
                    return missing(source, path);
                }
                current = list.get(index);
            } else {
                return missing(source, path);
            }
        }

        KogasaCommandSupport.sendCopyable(source, Component.translatable(
                "commands.kogasa_addiction.nbt.path",
                Component.literal(path),
                Component.literal(current == null ? "" : current.getAsString())));
        return 1;
    }

    private static int missing(CommandSourceStack source, String path) {
        source.sendFailure(Component.translatable(
                "commands.kogasa_addiction.nbt.path.missing", Component.literal(path)));
        return 0;
    }
}
