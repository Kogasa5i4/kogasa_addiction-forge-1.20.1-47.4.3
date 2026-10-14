package net.kogasachan.kogasa_addiction.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.event.server.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaCommands {
    private static final SuggestionProvider<CommandSourceStack> ABILITY_SUGGESTIONS =
            (ctx, builder) -> {
                for (KogasaAbility ability : KogasaAbility.values()) {
                    builder.suggest(ability.getId());
                }
                return builder.buildFuture();
            };

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        CommandBuildContext buildContext = event.getBuildContext();

        dispatcher.register(
                Commands.literal("kogasa")
                        .requires(source -> source.hasPermission(2))
                        // /kogasa grow <amount>
                        .then(Commands.literal("grow")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(ctx -> grow(ctx,
                                                DoubleArgumentType.getDouble(ctx, "amount")))))
                        // /kogasa reinforce <item>
                        .then(Commands.literal("reinforce")
                                .then(Commands.argument("item", ItemArgument.item(buildContext))
                                        .executes(ctx -> reinforce(ctx,
                                                ItemArgument.getItem(ctx, "item").createItemStack(1, false)))))
                        // /kogasa reset
                        .then(Commands.literal("reset")
                                .executes(KogasaCommands::reset))
                        // /kogasa info
                        .then(Commands.literal("info")
                                .executes(KogasaCommands::info))
                        // /kogasa enchant <enchantment> <level>
                        .then(Commands.literal("enchant")
                                .then(Commands.argument("enchantment",
                                                ResourceArgument.resource(buildContext, Registries.ENCHANTMENT))
                                        .then(Commands.argument("level", IntegerArgumentType.integer(1, 255))
                                                .executes(ctx -> enchant(ctx,
                                                        ResourceArgument.getEnchantment(ctx, "enchantment").value(),
                                                        IntegerArgumentType.getInteger(ctx, "level"))))))
                        // /kogasa growenchant <count>
                        .then(Commands.literal("growenchant")
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> growEnchant(ctx,
                                                IntegerArgumentType.getInteger(ctx, "count")))))
                        // /kogasa triggerenchant
                        .then(Commands.literal("triggerenchant")
                                .executes(KogasaCommands::triggerEnchant))
                        // /kogasa addability <ability>
                        .then(Commands.literal("addability")
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests(ABILITY_SUGGESTIONS)
                                        .executes(ctx -> addAbility(ctx,
                                                StringArgumentType.getString(ctx, "ability")))))
                        // /kogasa removeability <ability>
                        .then(Commands.literal("removeability")
                                .then(Commands.argument("ability", StringArgumentType.word())
                                        .suggests(ABILITY_SUGGESTIONS)
                                        .executes(ctx -> removeAbility(ctx,
                                                StringArgumentType.getString(ctx, "ability")))))
                        // /kogasa clearabilities
                        .then(Commands.literal("clearabilities")
                                .executes(KogasaCommands::clearAbilities))
                        // /kogasa newsvalue <amount>
                        .then(Commands.literal("newsvalue")
                                .then(Commands.argument("amount", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(ctx -> newsValue(ctx,
                                                DoubleArgumentType.getDouble(ctx, "amount")))))
                        // /kogasa setnewsvalue <value>
                        .then(Commands.literal("setnewsvalue")
                                .then(Commands.argument("value", DoubleArgumentType.doubleArg(0.0, 100.0))
                                        .executes(ctx -> setNewsValue(ctx,
                                                DoubleArgumentType.getDouble(ctx, "value")))))
        );
    }

    // ==================== grow ====================

    private static int grow(CommandContext<CommandSourceStack> ctx, double amount)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

        double current = KogasaEquipmentData.getGrowth(held);
        double max = Config.MAX_GROWTH.get();
        double newValue = Math.min(current + amount, max);

        KogasaEquipmentData.setGrowth(held, newValue);
        player.getInventory().setChanged();

        final double oldV = current;
        final double newV = newValue;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.grow.success",
                String.format("%.4f", oldV),
                String.format("%.4f", newV)), true);
        return 1;
    }

    // ==================== reinforce ====================

    private static int reinforce(CommandContext<CommandSourceStack> ctx, ItemStack material)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }
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

        TagKey<Item> matchTag = KogasaEquipmentHelper.getMatchingTag(held);
        if (matchTag == null || !material.is(matchTag)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.wrong_type"));
            return 0;
        }

        ResourceLocation materialId = ForgeRegistries.ITEMS.getKey(material.getItem());
        if (materialId == null) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.invalid_material"));
            return 0;
        }
        if (KogasaEquipmentData.hasUsedMaterial(held, materialId.toString())) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.already_absorbed", materialId.toString()));
            return 0;
        }

        KogasaEquipmentData.setReinforcements(held, re + 1);
        KogasaEquipmentData.addUsedMaterial(held, materialId.toString());

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
                oldRe, newRe, String.format("%.2f", absorbValue)), true);
        return 1;
    }

    // ==================== reset ====================

    private static int reset(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

        // 清空整个 KogasaEquipment 复合标签
        if (held.getTag() != null) {
            held.getTag().remove("KogasaEquipment");
        }

        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.reset.success"), true);
        return 1;
    }

    // ==================== info ====================

    private static int info(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

        int re = KogasaEquipmentData.getReinforcements(held);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(held);
        double growth = KogasaEquipmentData.getGrowth(held);
        double absorbed = KogasaEquipmentData.getAbsorbedValue(held);
        int growthEnchants = KogasaEquipmentData.getGrowthEnchantsGranted(held);
        List<String> usedMaterials = KogasaEquipmentData.getUsedMaterials(held);

        // 标题
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.header",
                held.getHoverName()), false);

        // 强化
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.reinforcement",
                re, maxRe), false);

        // 成长
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.growth",
                String.format("%.4f", growth),
                String.format("%.2f%%", growth * 100)), false);

        // 吸收值
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.absorbed",
                String.format("%.2f", absorbed)), false);

        // 成长附魔次数
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.growth_enchant",
                growthEnchants), false);

        // 已吸收材料
        if (usedMaterials.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.materials.none"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.materials.header",
                    usedMaterials.size()), false);
            for (String id : usedMaterials) {
                ctx.getSource().sendSuccess(() -> Component.literal("  §7- " + id), false);
            }
        }

        // 当前附魔
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(held);
        if (!enchants.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.enchants.header",
                    enchants.size()), false);
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                String name = entry.getKey().getFullname(entry.getValue()).getString();
                ctx.getSource().sendSuccess(() -> Component.literal("  §7- " + name), false);
            }
        }

        // 能力
        List<KogasaAbility> abilities = KogasaAbilityData.getAbilities(held);
        int maxSlots = KogasaAbilityData.getMaxAbilitySlots(held);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.abilities.header",
                abilities.size(), maxSlots), false);

        if (abilities.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.abilities.none"), false);
        } else {
            for (KogasaAbility ability : abilities) {
                ctx.getSource().sendSuccess(() -> Component.literal(
                        "  §7- " + ability.getDisplayName().getString()), false);
            }
        }

        // AYA 新闻值（仅当有 AYA 能力）
        if (KogasaAbilityData.hasAbility(held, KogasaAbility.AYA)) {
            double news = KogasaAbilityData.getNewsValue(held);
            int killed = KogasaAbilityData.getKilledMobCount(held);
            int mined = KogasaAbilityData.getMinedBlockCount(held);
            int damaged = KogasaAbilityData.getDamageTakenCount(held);

            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.news_value",
                    String.format("%.4f", news),
                    String.format("%.2f%%", news * 100)), false);
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.news_records",
                    killed, mined, damaged), false);
        }

        return 1;
    }

    // ==================== enchant ====================

    private static int enchant(CommandContext<CommandSourceStack> ctx,
                               Enchantment enchantment, int level)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

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
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

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
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

        double before = KogasaEquipmentData.getGrowth(held);
        int grantedBefore = KogasaEquipmentData.getGrowthEnchantsGranted(held);

        KogasaGrowthHandler.checkGrowthEnchant(held, player);

        int grantedAfter = KogasaEquipmentData.getGrowthEnchantsGranted(held);
        int delta = grantedAfter - grantedBefore;

        player.getInventory().setChanged();

        final int d = delta;
        final String growthStr = String.format("%.4f", before);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.triggerenchant.success", growthStr, d), true);
        return 1;
    }

    // ==================== addability ====================

    private static int addAbility(CommandContext<CommandSourceStack> ctx, String abilityId)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

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

        int current = KogasaAbilityData.getAbilities(held).size();
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
        return 1;
    }

// ==================== removeability ====================

    private static int removeAbility(CommandContext<CommandSourceStack> ctx, String abilityId)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

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
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

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
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

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
                String.format("%.4f", b),
                String.format("%.4f", a)), true);
        return 1;
    }

    // ==================== setnewsvalue ====================

    private static int setNewsValue(CommandContext<CommandSourceStack> ctx, double value)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }

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
                String.format("%.4f", v)), true);
        return 1;
    }
}