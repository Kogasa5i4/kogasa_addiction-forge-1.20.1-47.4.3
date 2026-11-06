package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.event.common.FirstJoinRewardHandler;
import net.kogasachan.kogasa_addiction.event.common.ManualGrantHandler;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKind;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKinds;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;

import static net.kogasachan.kogasa_addiction.command.KogasaCommandSupport.ABILITY_SUGGESTIONS;
import static net.kogasachan.kogasa_addiction.command.KogasaCommandSupport.ADMIN;

/** /kogasa grow|reinforce|reset|resetfirstjoin|seal|unseal —— 修改装备本体数据的命令组。 */
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
                .executes(KogasaEquipmentCommands::regrantFirstJoin));
        // 封印装备（战利品机制）的测试命令：seal 可指定能力，缺省随机
        root.then(Commands.literal("seal")
                .requires(ADMIN)
                .executes(ctx -> seal(ctx, null))
                .then(Commands.argument("ability", StringArgumentType.word())
                        .suggests(ABILITY_SUGGESTIONS)
                        .executes(ctx -> seal(ctx, StringArgumentType.getString(ctx, "ability")))));
        root.then(Commands.literal("unseal")
                .requires(ADMIN)
                .executes(KogasaEquipmentCommands::unseal));
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
        KogasaEquipmentKind heldKind = KogasaEquipmentKinds.of(held);
        boolean isRanged = heldKind == KogasaEquipmentKind.BOW
                || heldKind == KogasaEquipmentKind.CROSSBOW;
        boolean isArrowMaterial = isRanged && material.is(ItemTags.ARROWS);

        String materialId;

        if (isRanged) {
            if (isArrowMaterial) {
                materialId = "arrow:" + ForgeRegistries.ITEMS.getKey(material.getItem());
                // 箭矢使用独立 materialId，且不检查重复，允许重复使用
            } else if (KogasaEquipmentKinds.of(material) == heldKind) {
                // 弓/弩只能吸收同类型
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
            if (!KogasaEquipmentHelper.matchesReinforcementMaterial(held, material)) {
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


    // ==================== seal / unseal ====================

    /** 封印主手装备：注入指定（或随机）能力 + Locked=true，模拟战利品开出的封印装备。 */
    private static int seal(CommandContext<CommandSourceStack> ctx, @Nullable String abilityId)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        KogasaAbility ability;
        if (abilityId != null) {
            ability = KogasaAbility.byId(abilityId);
            if (ability == null) {
                ctx.getSource().sendFailure(Component.translatable(
                        "commands.kogasa_addiction.error.unknown_ability", abilityId));
                return 0;
            }
        } else {
            ability = KogasaAbility.values()[
                    player.getRandom().nextInt(KogasaAbility.values().length)];
        }

        KogasaAbilityData.forceAddAbility(held, ability);
        KogasaEquipmentData.setLocked(held, true);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.seal.success",
                ability.getDisplayName()), true);
        return 1;
    }

    private static int unseal(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

        if (!KogasaEquipmentData.isLocked(held)) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.unseal.already"), false);
            return 1;
        }

        KogasaEquipmentData.setLocked(held, false);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.unseal.success"), true);
        return 1;
    }

    /**
     * /kogasa resetfirstjoin：
     * 若装备或手册有任意一个“没发过”，先把两个标记都设为发过（不消耗任何东西），
     * 然后立即重发一份随机装备 + 一本手册；若两个都发过，则只重发、不改标记。
     */
    private static int regrantFirstJoin(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        CompoundTag data = player.getPersistentData();

        boolean equipmentGiven = data.getBoolean(FirstJoinRewardHandler.NBT_KEY);
        boolean manualGiven = data.getBoolean(ManualGrantHandler.NBT_KEY);
        if (!equipmentGiven || !manualGiven) {
            data.putBoolean(FirstJoinRewardHandler.NBT_KEY, true);
            data.putBoolean(ManualGrantHandler.NBT_KEY, true);
        }

        ItemStack equipment = FirstJoinRewardHandler.giveRandomEquipment(player);
        ManualGrantHandler.giveManualDirect(player, false);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.resetfirstjoin.success", equipment.getHoverName()), false);
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
