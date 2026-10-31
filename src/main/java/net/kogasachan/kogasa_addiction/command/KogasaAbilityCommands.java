package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.event.common.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.*;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.kogasachan.kogasa_addiction.command.KogasaCommandSupport.*;

/** /kogasa addability|removeability|clearabilities|newsvalue|setnewsvalue —— 能力侧数据命令组。 */
public final class KogasaAbilityCommands {

    private KogasaAbilityCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("addability")
                .requires(ADMIN)
                .then(Commands.argument("ability", StringArgumentType.word())
                        .suggests(ABILITY_SUGGESTIONS)
                        .executes(ctx -> addAbility(ctx, StringArgumentType.getString(ctx, "ability")))));
        root.then(Commands.literal("removeability")
                .requires(ADMIN)
                .then(Commands.argument("ability", StringArgumentType.word())
                        .suggests(ABILITY_SUGGESTIONS)
                        .executes(ctx -> removeAbility(ctx, StringArgumentType.getString(ctx, "ability")))));
        root.then(Commands.literal("clearabilities")
                .requires(ADMIN)
                .executes(KogasaAbilityCommands::clearAbilities));
        root.then(Commands.literal("newsvalue")
                .requires(ADMIN)
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, 100.0))
                        .executes(ctx -> newsValue(ctx, DoubleArgumentType.getDouble(ctx, "amount")))));
        root.then(Commands.literal("setnewsvalue")
                .requires(ADMIN)
                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                        .executes(ctx -> setNewsValue(ctx, DoubleArgumentType.getDouble(ctx, "value")))));
    }

    // ==================== addability ====================

    private static int addAbility(CommandContext<CommandSourceStack> ctx, String abilityId)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        KogasaAbility ability = KogasaAbility.byId(abilityId);
        if (ability == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.unknown_ability", abilityId));
            return 0;
        }

        if (KogasaAbilityData.hasAbility(held, ability)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.already_has_ability",
                    ability.getDisplayName()));
            return 0;
        }

        int current = KogasaAbilityData.getUsedSlots(held);
        int max = KogasaAbilityData.getMaxAbilitySlots(held);
        if (current >= max) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.ability_slots_full", current, max));
            return 0;
        }

        KogasaAbilityData.addAbility(held, ability);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.addability.success",
                ability.getDisplayName()), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }


    // ==================== removeability ====================

    private static int removeAbility(CommandContext<CommandSourceStack> ctx, String abilityId)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        KogasaAbility ability = KogasaAbility.byId(abilityId);
        if (ability == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.unknown_ability", abilityId));
            return 0;
        }

        if (!KogasaAbilityData.removeAbility(held, ability)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.doesnt_have_ability",
                    ability.getDisplayName()));
            return 0;
        }

        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.removeability.success",
                ability.getDisplayName()), true);
        return 1;
    }


    // ==================== clearabilities ====================

    private static int clearAbilities(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        KogasaAbilityData.clearAbilities(held);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.clearabilities.success"), true);
        return 1;
    }


    // ==================== newsvalue ====================

    private static int newsValue(CommandContext<CommandSourceStack> ctx, double amount)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        if (!KogasaAbilityData.hasAbility(held, KogasaAbility.AYA)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.requires_aya"));
            return 0;
        }

        double before = KogasaAbilityData.getNewsValue(held);
        KogasaAbilityData.addNewsValue(held, amount);
        double after = KogasaAbilityData.getNewsValue(held);

        player.getInventory().setChanged();

        final double b = before, a = after;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.newsvalue.success",
                KogasaNumberFormatHelper.formatTrimmed(b, 4),
                KogasaNumberFormatHelper.formatTrimmed(a, 4)), true);
        return 1;
    }


    // ==================== setnewsvalue ====================

    private static int setNewsValue(CommandContext<CommandSourceStack> ctx, double value)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        if (!KogasaAbilityData.hasAbility(held, KogasaAbility.AYA)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.requires_aya"));
            return 0;
        }

        double max = Config.MAX_NEWS_VALUE.get();
        double newVal = Math.min(value, max);
        KogasaAbilityData.setNewsValue(held, newVal);
        player.getInventory().setChanged();

        final double v = newVal;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setnewsvalue.success",
                KogasaNumberFormatHelper.formatTrimmed(v, 4)), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }

}
