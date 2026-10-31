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

/**
 * /kogasa info —— 只读命令组。
 *
 * <p>只打印当前装备的数据，不修改任何状态，因此**不要求 OP**：
 * 普通玩家也能查看自己装备的强化、成长、能力与远程进度，便于理解数值。
 */
public final class KogasaInfoCommands {

    private KogasaInfoCommands() {
    }

    static void register(LiteralArgumentBuilder<CommandSourceStack> root) {
        root.then(Commands.literal("info")
                .executes(KogasaInfoCommands::info));
    }

    // ==================== info ====================

    private static int info(CommandContext<CommandSourceStack> ctx)
            throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        ItemStack held = KogasaCommandSupport.requireKogasaEquipment(ctx);
        if (held.isEmpty()) return 0;

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
                KogasaNumberFormatHelper.formatTrimmed(growth, 4),
                KogasaNumberFormatHelper.formatPercentTrimmed(growth, 2),
                KogasaNumberFormatHelper.formatPercentTrimmed(maxGrowth, 2)), false);

        // ===== 倍率 =====
        double reMult = KogasaEquipmentHelper.getReinforcementMultiplier(held);
        double growthMult = KogasaEquipmentHelper.getGrowthMultiplier(held);
        double newsMult = 1.0 + (KogasaAbilityData.hasAbility(held, KogasaAbility.AYA)
                ? KogasaAbilityData.getNewsValue(held) : 0.0);
        double totalMult = reMult * growthMult * newsMult;

        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.multiplier",
                KogasaNumberFormatHelper.formatTrimmed(reMult, 2),
                KogasaNumberFormatHelper.formatTrimmed(growthMult, 2),
                KogasaNumberFormatHelper.formatTrimmed(newsMult, 2),
                KogasaNumberFormatHelper.formatTrimmed(totalMult, 3)), false);

        // ===== 吸收值 =====
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "commands.kogasa_addiction.info.absorbed",
                KogasaNumberFormatHelper.formatTrimmed(absorbed, 2)), false);

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
                    KogasaNumberFormatHelper.formatTrimmed(news, 4),
                    KogasaNumberFormatHelper.formatPercentTrimmed(news, 2),
                    KogasaNumberFormatHelper.formatPercentTrimmed(maxNews, 2)), false);
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
                        KogasaNumberFormatHelper.formatPercentTrimmed(threshold, 2),
                        KogasaNumberFormatHelper.formatPercentTrimmed(growth, 2)), false);
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
                        KogasaNumberFormatHelper.formatPercentTrimmed(threshold, 2),
                        KogasaNumberFormatHelper.formatPercentTrimmed(growth, 2)), false);
            } else {
                double threshold = Config.BOW_MULTISHOT_2_GROWTH.get();
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.multishot.progress",
                        arrows,
                        KogasaNumberFormatHelper.formatPercentTrimmed(threshold, 2),
                        KogasaNumberFormatHelper.formatPercentTrimmed(growth, 2)), false);
            }

            // 弹药
            if (KogasaAbilityData.hasAmmo(held)) {
                var ammoType = KogasaAbilityData.getAmmoType(held);
                if (ammoType == KogasaAbilityData.AmmoType.POTION) {
                    ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(held);
                    var kind = KogasaAbilityData.getAmmoPotionKind(held);
                    if (potionId != null) {
                        Component potionName = KogasaDisplayHelper.getPotionDisplayName(potionId, kind);
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
                final String pct = KogasaNumberFormatHelper.formatPercentTrimmed(noConsume, 0);
                ctx.getSource().sendSuccess(() -> Component.translatable(
                        "commands.kogasa_addiction.info.ranged.no_consume", pct), false);
            } else {
                double start = Config.NO_CONSUME_ARROW_START_GROWTH.get();
                final String cur = KogasaNumberFormatHelper.formatPercentTrimmed(growth, 0);
                final String need = KogasaNumberFormatHelper.formatPercentTrimmed(start, 0);
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
                    KogasaNumberFormatHelper.formatTrimmed(charge, 1),
                    KogasaNumberFormatHelper.formatTrimmed(threshold, 1),
                    resistanceLevel,
                    KogasaNumberFormatHelper.formatPercentTrimmed(ratio, 0)
            ), false);

            // 蓄满时额外显示 ready 提示
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
                    KogasaNumberFormatHelper.formatTrimmed(flyingMult, 1)
            ), false);

            // 输出 Buff 说明
            ctx.getSource().sendSuccess(() -> Component.translatable(
                    "commands.kogasa_addiction.info.tenshi.buff"), false);
        }

        return 1;
    }

}
