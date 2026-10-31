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

/** /kogasa enchant|growenchant|triggerenchant|removeenchant|clearenchants|setenchantlevel|resetgrowthenchant —— 附魔与成长附魔命令组。 */
public final class KogasaEnchantCommands {

    private KogasaEnchantCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandBuildContext buildContext) {
        root.then(Commands.literal("enchant")
                .requires(ADMIN)
                .then(Commands.argument("enchantment",
                                ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                .executes(ctx -> enchant(ctx,
                                        ResourceArgument.getEnchantment(ctx, "enchantment").value(),
                                        IntegerArgumentType.getInteger(ctx, "level"))))));
        root.then(Commands.literal("growenchant")
                .requires(ADMIN)
                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                        .executes(ctx -> growEnchant(ctx, IntegerArgumentType.getInteger(ctx, "count")))));
        root.then(Commands.literal("triggerenchant")
                .requires(ADMIN)
                .executes(KogasaEnchantCommands::triggerEnchant));
        root.then(Commands.literal("removeenchant")
                .requires(ADMIN)
                .then(Commands.argument("enchantment",
                                ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .suggests(EXISTING_ENCHANT_SUGGESTIONS)
                        .executes(ctx -> removeEnchant(ctx,
                                ResourceArgument.getEnchantment(ctx, "enchantment").value()))));
        root.then(Commands.literal("clearenchants")
                .requires(ADMIN)
                .executes(KogasaEnchantCommands::clearEnchants));
        root.then(Commands.literal("setenchantlevel")
                .requires(ADMIN)
                .then(Commands.argument("enchantment",
                                ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                        .suggests(EXISTING_ENCHANT_SUGGESTIONS)
                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                .executes(ctx -> setEnchantLevel(ctx,
                                        ResourceArgument.getEnchantment(ctx, "enchantment").value(),
                                        IntegerArgumentType.getInteger(ctx, "level"))))));
        root.then(Commands.literal("resetgrowthenchant")
                .requires(ADMIN)
                .executes(KogasaEnchantCommands::resetGrowthEnchant));
    }

    // ==================== enchant ====================

    private static int enchant(CommandContext<CommandSourceStack> ctx,
                               Enchantment enchantment, int level)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(held);
        enchants.put(enchantment, level);
        EnchantmentHelper.setEnchantments(enchants, held);

        player.getInventory().setChanged();

        String enchName = enchantment.getFullname(level).getString();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.enchant.success", enchName), true);
        return 1;
    }


    // ==================== growenchant ====================

    private static int growEnchant(CommandContext<CommandSourceStack> ctx, int count)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        // 预检：已满级直接失败
        if (KogasaGrowthHandler.isFullyMaxed(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.growenchant.maxed"));
            return 0;
        }

        int success = 0;
        int fail = 0;

        for (int i = 0; i < count; i++) {
            if (KogasaGrowthHandler.grantOrUpgradeEnchant(held, player)) {
                success++;
            } else {
                fail++;
            }
        }

        // 累加 granted，让玩家看到"叠了多少次"
        int granted = KogasaEquipmentData.getGrowthEnchantsGranted(held);
        KogasaEquipmentData.setGrowthEnchantsGranted(held, granted + success);

        player.getInventory().setChanged();

        // 音效
        if (success > 0) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
        }
        if (fail > 0) {
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.5F);
        }

        final int s = success;
        final int f = fail;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.growenchant.success", s, f), true);
        return success;
    }


    // ==================== triggerenchant ====================

    private static int triggerEnchant(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        double before = KogasaEquipmentData.getGrowth(held);
        int grantedBefore = KogasaEquipmentData.getGrowthEnchantsGranted(held);

        KogasaGrowthHandler.checkGrowthEnchant(held, player);

        int grantedAfter = KogasaEquipmentData.getGrowthEnchantsGranted(held);
        int delta = grantedAfter - grantedBefore;

        player.getInventory().setChanged();

        final int d = delta;
        final String growthStr = KogasaNumberFormatHelper.formatTrimmed(before, 4);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.triggerenchant.success", growthStr, d), true);
        return 1;
    }


    // ==================== removeenchant ====================

    private static int removeEnchant(CommandContext<CommandSourceStack> ctx,
                                     Enchantment enchantment)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(held);
        if (!enchants.containsKey(enchantment)) {
            String enchName = enchantment.getFullname(1).getString();
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.removeenchant.not_present", enchName));
            return 0;
        }

        enchants.remove(enchantment);
        EnchantmentHelper.setEnchantments(enchants, held);
        player.getInventory().setChanged();

        String enchName = enchantment.getFullname(1).getString();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.removeenchant.success", enchName), true);
        return 1;
    }


    // ==================== clearenchants ====================

    private static int clearEnchants(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(held);
        if (enchants.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.clearenchants.empty"));
            return 0;
        }

        final int count = enchants.size();
        EnchantmentHelper.setEnchantments(new HashMap<>(), held);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.clearenchants.success", count), true);
        return 1;
    }


    // ==================== setenchantlevel ====================

    private static int setEnchantLevel(CommandContext<CommandSourceStack> ctx,
                                       Enchantment enchantment, int level)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(held);
        if (!enchants.containsKey(enchantment)) {
            String enchName = enchantment.getFullname(1).getString();
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.setenchantlevel.not_present", enchName));
            return 0;
        }

        final int oldLevel = enchants.get(enchantment);
        enchants.put(enchantment, level);
        EnchantmentHelper.setEnchantments(enchants, held);
        player.getInventory().setChanged();

        String enchName = enchantment.getFullname(level).getString();
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setenchantlevel.success",
                enchName, oldLevel, level), true);
        return 1;
    }


    // ==================== resetgrowthenchant ====================

    private static int resetGrowthEnchant(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        int before = KogasaEquipmentData.getGrowthEnchantsGranted(held);
        boolean hadFailFlag = KogasaEquipmentData.hasGrowthEnchantFailNotified(held);

        if (before <= 0 && !hadFailFlag) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.resetgrowthenchant.already_zero"));
            return 0;
        }

        KogasaEquipmentData.setGrowthEnchantsGranted(held, 0);
        KogasaEquipmentData.setGrowthEnchantFailNotified(held, false);
        player.getInventory().setChanged();

        final int b = before;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.resetgrowthenchant.success", b), true);
        return 1;
    }

}
