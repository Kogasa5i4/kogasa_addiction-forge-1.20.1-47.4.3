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
import net.kogasachan.kogasa_addiction.event.common.KogasaGrowthHandler;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
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
import net.minecraft.world.item.alchemy.PotionUtils;
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
                        // /kogasa setammo potion <potion>
                        .then(Commands.literal("setammo")
                                .then(Commands.literal("potion")
                                        .then(Commands.argument("potion", ResourceArgument.resource(buildContext, Registries.POTION))
                                                .executes(ctx -> setAmmoPotion(ctx,
                                                        ResourceArgument.getResource(ctx, "potion", Registries.POTION).key().location(),
                                                        KogasaAbilityData.PotionKind.REGULAR))))
                                .then(Commands.literal("splash")
                                        .then(Commands.argument("potion", ResourceArgument.resource(buildContext, Registries.POTION))
                                                .executes(ctx -> setAmmoPotion(ctx,
                                                        ResourceArgument.getResource(ctx, "potion", Registries.POTION).key().location(),
                                                        KogasaAbilityData.PotionKind.SPLASH))))
                                .then(Commands.literal("lingering")
                                        .then(Commands.argument("potion", ResourceArgument.resource(buildContext, Registries.POTION))
                                                .executes(ctx -> setAmmoPotion(ctx,
                                                        ResourceArgument.getResource(ctx, "potion", Registries.POTION).key().location(),
                                                        KogasaAbilityData.PotionKind.LINGERING))))
                                .then(Commands.literal("firework")
                                        .executes(KogasaCommands::setAmmoFirework)))
                        // /kogasa clearammo
                        .then(Commands.literal("clearammo")
                                .executes(KogasaCommands::clearAmmo))
                        // /kogasa resetfirstjoin
                        .then(Commands.literal("resetfirstjoin")
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                                    player.getPersistentData().remove("KogasaFirstJoinReward");
                                    // 下次登录/执行 /kogasa givefirstjoin 时发放
                                    return 1;
                                }))
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
        KogasaAdvancementHelper.checkAndGrant(player, held);
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

        // ===== 判断材料类型 =====
        boolean isRanged = isRangedWeapon(held);
        boolean isArrowMaterial = isRanged && material.is(ItemTags.ARROWS);

        String materialId;
        int materialCost = 1;

        if (isRanged) {
            if (isArrowMaterial) {
                materialId = "arrow:" + ForgeRegistries.ITEMS.getKey(material.getItem());
                materialCost = 16;
                // 箭矢不限重复
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
        final int cost = materialCost;
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.reinforce.success",
                oldRe, newRe, String.format("%.2f", absorbValue)), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
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
        double maxGrowth = Config.MAX_GROWTH.get();
        double absorbed = KogasaEquipmentData.getAbsorbedValue(held);
        int growthEnchants = KogasaEquipmentData.getGrowthEnchantsGranted(held);
        List<String> usedMaterials = KogasaEquipmentData.getUsedMaterials(held);
        boolean isRanged = isRangedWeapon(held);

        // ===== 标题 =====
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.header",
                held.getHoverName()), false);

        // ===== 强化 =====
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.reinforcement",
                re, maxRe), false);

        // ===== 成长 =====
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.growth",
                String.format("%.4f", growth),
                String.format("%.2f%%", growth * 100),
                String.format("%.2f%%", maxGrowth * 100)), false);

        // ===== 倍率 =====
        double reMult = KogasaEquipmentHelper.getReinforcementMultiplier(held);
        double growthMult = KogasaEquipmentHelper.getGrowthMultiplier(held);
        double newsMult = 1.0 + (KogasaAbilityData.hasAbility(held, KogasaAbility.AYA)
                ? KogasaAbilityData.getNewsValue(held) : 0.0);
        double totalMult = reMult * growthMult * newsMult;

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.multiplier",
                String.format("%.2f", reMult),
                String.format("%.2f", growthMult),
                String.format("%.2f", newsMult),
                String.format("%.3f", totalMult)), false);

        // ===== 吸收值 =====
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.absorbed",
                String.format("%.2f", absorbed)), false);

        // ===== 成长附魔次数 =====
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.growth_enchant",
                growthEnchants), false);

        // ===== 已吸收材料 =====
        if (usedMaterials.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.materials.none"), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.materials.header",
                    usedMaterials.size()), false);
            for (String id : usedMaterials) {
                Component name = getMaterialDisplayName(id);
                ctx.getSource().sendSuccess(() -> Component.literal("  §7- §f")
                        .append(name), false);
            }
        }

        // ===== 当前附魔 =====
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(held);
        if (!enchants.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.enchants.header",
                    enchants.size()), false);
            for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
                Component name = entry.getKey().getFullname(entry.getValue());
                ctx.getSource().sendSuccess(() -> Component.literal("  §7- §f")
                        .append(name), false);
            }
        }

        // ===== 能力 =====
        List<KogasaAbility> abilities = KogasaAbilityData.getAbilities(held);
        int maxSlots = KogasaAbilityData.getMaxAbilitySlots(held);
        int usedSlots = KogasaAbilityData.getUsedSlots(held);

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.abilities.header",
                usedSlots, maxSlots), false);

        if (abilities.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.abilities.none"), false);
        } else {
            for (KogasaAbility ability : abilities) {
                ctx.getSource().sendSuccess(() -> Component.literal("  §7- §f")
                        .append(ability.getDisplayName()), false);
            }
        }

        // ===== AYA 新闻值 =====
        if (KogasaAbilityData.hasAbility(held, KogasaAbility.AYA)) {
            double news = KogasaAbilityData.getNewsValue(held);
            double maxNews = Config.MAX_NEWS_VALUE.get();
            int killed = KogasaAbilityData.getKilledMobCount(held);
            int mined = KogasaAbilityData.getMinedBlockCount(held);
            int damaged = KogasaAbilityData.getDamageTakenCount(held);

            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.news_value",
                    String.format("%.4f", news),
                    String.format("%.2f%%", news * 100),
                    String.format("%.2f%%", maxNews * 100)), false);
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.news_records",
                    killed, mined, damaged), false);
        }

        // ===== 远程武器 =====
        if (isRanged) {
            // 穿透进度
            int pierce = KogasaEquipmentHelper.getBowPierceLevel(held);
            if (pierce > 0) {
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.pierce.unlocked", pierce), false);
            } else {
                double threshold = Config.BOW_PIERCE_GROWTH.get();
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.pierce.locked",
                        String.format("%.2f%%", threshold * 100),
                        String.format("%.2f%%", growth * 100)), false);
            }

            // 多发进度
            int arrows = KogasaEquipmentHelper.getBowArrowCount(held);
            if (arrows >= 3) {
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.multishot.max"), false);
            } else if (arrows == 2) {
                double threshold = Config.BOW_MULTISHOT_3_GROWTH.get();
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.multishot.progress",
                        arrows,
                        String.format("%.2f%%", threshold * 100),
                        String.format("%.2f%%", growth * 100)), false);
            } else {
                double threshold = Config.BOW_MULTISHOT_2_GROWTH.get();
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.multishot.progress",
                        arrows,
                        String.format("%.2f%%", threshold * 100),
                        String.format("%.2f%%", growth * 100)), false);
            }

            // 弹药
            if (KogasaAbilityData.hasAmmo(held)) {
                var ammoType = KogasaAbilityData.getAmmoType(held);
                if (ammoType == KogasaAbilityData.AmmoType.POTION) {
                    ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(held);
                    var kind = KogasaAbilityData.getAmmoPotionKind(held);
                    if (potionId != null) {
                        Component potionName = getPotionDisplayName(potionId, kind);
                        ctx.getSource().sendSuccess(() -> Component.translatable(
                                        "commands.kogasa_addiction.info.ammo.potion")
                                .append(potionName), false);
                    }
                } else if (ammoType == KogasaAbilityData.AmmoType.FIREWORK) {
                    ctx.getSource().sendSuccess(() -> Component.translatable(
                            "commands.kogasa_addiction.info.ammo.firework"), false);
                }
            } else {
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ammo.none"), false);
            }

            // 不消耗箭矢概率
            double noConsume = KogasaEquipmentHelper.getNoConsumeArrowChance(held);
            if (noConsume >= 1.0) {
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.no_consume.max"), false);
            } else if (noConsume > 0) {
                final String pct = String.format("%.0f%%", noConsume * 100);
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.no_consume", pct), false);
            } else {
                double start = Config.NO_CONSUME_ARROW_START_GROWTH.get();
                final String cur = String.format("%.0f%%", growth * 100);
                final String need = String.format("%.0f%%", start * 100);
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.no_consume.locked", cur, need), false);
            }
        }

        // ===== 天子能力状态 =====
        boolean hasTenshiArmor = false;
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof IKogasaEquipment && KogasaAbilityData.hasAbility(armor, KogasaAbility.TENSHI)) {
                hasTenshiArmor = true;
                break;
            }
        }

        if (hasTenshiArmor) {
            double threshold = Config.TENSHI_DOMAIN_THRESHOLD.get();
            double ratio = Config.TENSHI_BURST_DAMAGE_RATIO.get() * 100.0;
            double charge = player.getPersistentData().getDouble("KogasaTenshiCharge");
            boolean isReady = player.getPersistentData().getBoolean("KogasaTenshiBurstReady");
            int resistanceLevel = (int) Math.min(5, Math.floor(charge / (threshold / 4.0)));

            // 输出护甲蓄力状态 (传入：当前值, 阈值, 抗性等级, 爆发百分比)
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.tenshi.armor",
                    String.format("%.1f", charge),
                    String.format("%.1f", threshold),
                    resistanceLevel,
                    String.format("%.0f%%", ratio)
            ), false);

            // 如果蓄满，额外提示一下（这里复用了你之前的 ready 键，或者不需要可以删）
            if (isReady) {
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.tenshi.ready"), false);
            }
        }

        // 检查武器是否带有天子能力
        if (held.getItem() instanceof IKogasaEquipment && KogasaAbilityData.hasAbility(held, KogasaAbility.TENSHI) && isRangedWeapon(held)) {
            int debuffSecs = Config.TENSHI_DEBUFF_DURATION_SECONDS.get();
            double flyingMult = Config.TENSHI_FLYING_MOB_MULTIPLIER.get();

            // 输出武器命中状态 (传入：Debuff秒数, 飞行生物倍率)
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.tenshi.weapon",
                    debuffSecs,
                    String.format("%.1f", flyingMult)
            ), false);

            // 输出 Buff 说明
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.tenshi.buff"), false);
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
        KogasaAdvancementHelper.checkAndGrant(player, held);
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
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }

    // ==================== setammo ====================

    private static int setAmmoPotion(CommandContext<CommandSourceStack> ctx,
                                     ResourceLocation potionId,
                                     KogasaAbilityData.PotionKind kind)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }
        if (!isRangedWeapon(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_ranged"));
            return 0;
        }

        // 槽位检查
        int used = KogasaAbilityData.getUsedSlots(held);
        int max = KogasaAbilityData.getMaxAbilitySlots(held);
        if (used >= max) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.ability_slots_full", used, max));
            return 0;
        }

        KogasaAbilityData.setPotionAmmo(held, potionId, kind);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setammo.potion.success",
                potionId.toString(),
                kind.name().toLowerCase()), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }

    private static int setAmmoFirework(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }
        if (!isRangedWeapon(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_ranged"));
            return 0;
        }

        // 从副手取烟花 NBT
        ItemStack offhand = player.getOffhandItem();
        if (!offhand.is(Items.FIREWORK_ROCKET)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.firework_required"));
            return 0;
        }
        CompoundTag nbt = offhand.getTag();
        if (nbt == null || !nbt.contains("Fireworks")) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.firework_invalid"));
            return 0;
        }

        // 槽位检查
        int used = KogasaAbilityData.getUsedSlots(held);
        int max = KogasaAbilityData.getMaxAbilitySlots(held);
        if (used >= max) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.ability_slots_full", used, max));
            return 0;
        }

        KogasaAbilityData.setFireworkAmmo(held, nbt.copy());
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.setammo.firework.success"), true);
        KogasaAdvancementHelper.checkAndGrant(player, held);
        return 1;
    }

// ==================== clearammo ====================

    private static int clearAmmo(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = player.getMainHandItem();

        if (!(held.getItem() instanceof IKogasaEquipment)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.not_kogasa"));
            return 0;
        }
        if (!KogasaAbilityData.hasAmmo(held)) {
            ctx.getSource().sendFailure(Component.translatable(
                    "commands.kogasa_addiction.error.no_ammo"));
            return 0;
        }

        KogasaAbilityData.clearAmmo(held);
        player.getInventory().setChanged();

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.clearammo.success"), true);
        return 1;
    }

    // ==================== 辅助 ====================

    private static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    private static Component getMaterialDisplayName(String id) {
        String actualId = id;
        boolean isArrow = false;
        if (id.startsWith("arrow:")) {
            actualId = id.substring("arrow:".length());
            isArrow = true;
        }
        ResourceLocation loc = ResourceLocation.tryParse(actualId);
        if (loc == null) return Component.literal(id);

        var item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) return Component.literal(id);

        Component name = new ItemStack(item).getHoverName();
        if (isArrow) {
            return Component.translatable(
                    "tooltip.kogasa_addiction.reinforcement.entry.arrow", name);
        }
        return name;
    }

    private static Component getPotionDisplayName(ResourceLocation potionId,
                                                  KogasaAbilityData.PotionKind kind) {
        var potion = ForgeRegistries.POTIONS.getValue(potionId);
        if (potion == null) return Component.literal(potionId.toString());

        ItemStack potionStack = switch (kind) {
            case REGULAR -> new ItemStack(Items.POTION);
            case SPLASH -> new ItemStack(Items.SPLASH_POTION);
            case LINGERING -> new ItemStack(Items.LINGERING_POTION);
        };
        PotionUtils.setPotion(potionStack, potion);
        return potionStack.getHoverName();
    }
}