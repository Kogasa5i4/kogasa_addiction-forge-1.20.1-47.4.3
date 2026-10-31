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

/** /kogasa grow|reinforce|reset|resetfirstjoin —— 修改装备本体数据的命令组。 */
public final class KogasaEquipmentCommands {

    private KogasaEquipmentCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root, CommandBuildContext buildContext) {
        root.then(Commands.literal("grow")
                .requires(ADMIN)
                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, 100.0))
                        .executes(ctx -> grow(ctx, DoubleArgumentType.getDouble(ctx, "amount")))));
        root.then(Commands.literal("reinforce")
                .requires(ADMIN)
                .then(Commands.argument("item", ItemArgument.item(buildContext))
                        .executes(ctx -> reinforce(ctx,
                                ItemArgument.getItem(ctx, "item").createItemStack(1, false)))));
        root.then(Commands.literal("reset")
                .requires(ADMIN)
                .executes(KogasaEquipmentCommands::reset));
        root.then(Commands.literal("resetfirstjoin")
                .requires(ADMIN)
                .executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    player.getPersistentData().remove("KogasaFirstJoinReward");
                    // 下次登录时由 FirstJoinRewardHandler 重新发放
                    return 1;
                }));
    }

    // ==================== grow ====================

    private static int grow(CommandContext<CommandSourceStack> ctx, double amount)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        double current = KogasaEquipmentData.getGrowth(held);
        double max = Config.MAX_GROWTH.get();
        double newValue = Math.min(current + amount, max);

        KogasaEquipmentData.setGrowth(held, newValue);
        player.getInventory().setChanged();

        final double oldV = current;
        final double newV = newValue;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.grow.success",
                KogasaNumberFormatHelper.formatTrimmed(oldV, 4),
                KogasaNumberFormatHelper.formatTrimmed(newV, 4)), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }


    // ==================== reinforce ====================

    private static int reinforce(CommandContext<CommandSourceStack> ctx, ItemStack material)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;
        if (material.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.invalid_material"));
            return 0;
        }

        int re = KogasaEquipmentData.getReinforcements(held);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(held);
        if (re >= maxRe) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.max_reinforcement", re, maxRe));
            return 0;
        }

        // ===== 判断材料类型 =====
        boolean isRanged = isRangedWeapon(held);
        boolean isArrowMaterial = isRanged && material.is(ItemTags.ARROWS);

        String materialId;

        if (isRanged) {
            if (isArrowMaterial) {
                materialId = "arrow:" + ForgeRegistries.ITEMS.getKey(material.getItem());
                // 箭矢使用独立 materialId，且不检查重复，允许重复使用
            } else if (material.getItem() instanceof BowItem
                    || material.getItem() instanceof CrossbowItem) {
                ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(material.getItem());
                if (rightId == null) return 0;
                materialId = rightId.toString();
                if (KogasaEquipmentData.hasUsedMaterial(held, materialId)) {
                    ctx.getSource().sendFailure(Component.translatable(
                            "commands.kogasa_addiction.error.already_absorbed", materialId));
                    return 0;
                }
            } else {
                ctx.getSource().sendFailure(Component.translatable(
                        "commands.kogasa_addiction.error.wrong_type"));
                return 0;
            }
        } else {
            TagKey<Item> matchTag = KogasaEquipmentHelper.getMatchingTag(held);
            if (matchTag == null || !material.is(matchTag)) {
                ctx.getSource().sendFailure(Component.translatable(
                        "commands.kogasa_addiction.error.wrong_type"));
                return 0;
            }
            ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(material.getItem());
            if (rightId == null) return 0;
            materialId = rightId.toString();
            if (KogasaEquipmentData.hasUsedMaterial(held, materialId)) {
                ctx.getSource().sendFailure(Component.translatable(
                        "commands.kogasa_addiction.error.already_absorbed", materialId));
                return 0;
            }
        }

        // ===== 应用 =====
        KogasaEquipmentData.setReinforcements(held, re + 1);
        KogasaEquipmentData.addUsedMaterial(held, materialId);

        double baseValue = KogasaEquipmentHelper.getEquipmentBaseValue(material);
        double ratio = KogasaEquipmentHelper.getAbsorbedRatio(material);
        double absorb = baseValue * ratio;
        if (absorb > 0) {
            KogasaEquipmentData.addAbsorbedValue(held, absorb);
        }

        player.getInventory().setChanged();

        final int oldRe = re;
        final int newRe = re + 1;
        final double absorbValue = absorb;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.reinforce.success",
                oldRe, newRe, KogasaNumberFormatHelper.formatTrimmed(absorbValue, 2)), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }


    // ==================== reset ====================

    private static int reset(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        // 两个 NBT 根一起清：老实现只删 KogasaEquipment，会留下能力 / 弹药 / 新闻值，
        // 而成长归零后能力槽上限回到 1、装备上却还挂着 2 个能力，形成"超槽"非法状态。
        if (!KogasaEquipmentData.hasData(held) && !KogasaAbilityData.hasData(held)) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.reset.already_clean"), false);
            return 1;
        }

        KogasaEquipmentData.clearAll(held);
        KogasaAbilityData.clearAllData(held);

        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.reset.success"), true);
        return 1;
    }

}
