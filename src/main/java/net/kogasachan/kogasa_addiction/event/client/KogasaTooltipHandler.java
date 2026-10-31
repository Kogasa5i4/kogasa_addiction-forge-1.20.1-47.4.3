package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.client.ClientTimestopState;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.*;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IKogasaEquipment)) return;

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());

        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        List<String> used = KogasaEquipmentData.getUsedMaterials(stack);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(stack);

        double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);

        // 找到原版攻击伤害行的索引并替换
        String damageAttrKey = Component.translatable(
                "attribute.name.generic.attack_damage").getString();

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line.getString().contains(damageAttrKey)) {
                double damage = KogasaEquipmentHelper.getDisplayAttackDamage(stack);
                Component newLine = Component.translatable(
                                "tooltip.kogasa_addiction.vanilla_attack_damage",
                                KogasaNumberFormatHelper.formatTrimmed(damage, 1))
                        .withStyle(line.getStyle()) // 继承原样式
                        .withStyle(ChatFormatting.DARK_GREEN);
                tooltip.set(i, newLine);
                break;
            }
        }

        // ===== 强化信息（Shift） =====
        if (Screen.hasShiftDown()) {
            // 强化次数
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.reinforcement", re, maxRe)
                    .withStyle(ChatFormatting.GOLD));

            // 成长倍率
            double growthMult = KogasaEquipmentHelper.getGrowthMultiplier(stack);

            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.multiplier.growth",
                            KogasaNumberFormatHelper.formatTrimmed(growthMult, 2))
                    .withStyle(ChatFormatting.YELLOW));

            // 已吸收装备
            if (used.isEmpty()) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.reinforcement.none")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (String id : used) {
                    Component itemName = KogasaDisplayHelper.getItemDisplayName(id);
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.reinforcement.entry", itemName)
                            .withStyle(ChatFormatting.GRAY));
                }
            }

            // 吸收值
            if (absorbed > 0) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.absorbed", KogasaNumberFormatHelper.formatTrimmed(absorbed, 1))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // ===== 成长信息（Ctrl，仅解锁后显示） =====
        if (re >= ModConfigCache.growthUnlockReinforcements) {
            if (Screen.hasControlDown()) {
                String percent = KogasaNumberFormatHelper.formatPercentTrimmed(growth, 2);
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.growth", percent)
                        .withStyle(ChatFormatting.GREEN));

                // 成长附魔进度
                int grantedEnchants = KogasaEquipmentData.getGrowthEnchantsGranted(stack);
                int maxEnchants = Config.MAX_GROWTH_ENCHANTS.get();
                if (Config.ENABLE_GROWTH_ENCHANT.get() && maxEnchants > 0) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.growth_enchant", grantedEnchants, maxEnchants)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.ctrl")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        // ===== 能力信息（Alt） =====
        if (Screen.hasAltDown()) {
            List<KogasaAbility> abilities = KogasaAbilityData.getAbilities(stack);
            int maxSlots = KogasaAbilityData.getMaxAbilitySlots(stack);
            int usedSlots = abilities.size();

            // 一次性解析出集合，后面 6 个能力分支都复用它，
            // 避免每帧对同一件装备重复解析 NBT 能力列表
            Set<KogasaAbility> abilitySet = abilities.isEmpty()
                    ? EnumSet.noneOf(KogasaAbility.class)
                    : EnumSet.copyOf(abilities);

            // 标题
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.abilities", usedSlots, maxSlots)
                    .withStyle(ChatFormatting.AQUA));

            // 能力列表 / 空提示
            if (abilities.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.abilities.none")
                        .withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.abilities.how_to")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (KogasaAbility ability : abilities) {
                    tooltip.add(Component.literal("  ✦ ")
                            .append(ability.getDisplayName())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            // 槽位满且未解锁第 2 槽 → 显示解锁提示（只在已占用能力槽时显示）
            if (usedSlots >= maxSlots && maxSlots < 2) {
                String secondSlotGrowth = KogasaNumberFormatHelper.formatPercentTrimmed(Config.GROWTH_FOR_SECOND_ABILITY_SLOT.get(), 0);
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.abilities.full_hint",
                                secondSlotGrowth)
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            if (abilitySet.contains(KogasaAbility.AYA)) {
                double news = KogasaAbilityData.getNewsValue(stack);
                int killed = KogasaAbilityData.getKilledMobCount(stack);
                int mined = KogasaAbilityData.getMinedBlockCount(stack);
                int damage = KogasaAbilityData.getDamageTakenCount(stack);

                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.aya.news_value",
                                KogasaNumberFormatHelper.formatPercentTrimmed(news, 2),
                                "×" + KogasaNumberFormatHelper.formatTrimmed(1.0 + news, 3))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.aya.records",
                                killed, mined, damage)
                        .withStyle(ChatFormatting.GRAY));
            }

            if (abilitySet.contains(KogasaAbility.CIRNO)) {
                if (!Config.CIRNO_REVIVE_ENABLED.get()) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.cirno.revive_disabled")
                            .withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    double cost = Config.CIRNO_REVIVE_GROWTH_COST.get();
                    boolean canRevive = growth >= cost;

                    MutableComponent reviveLine = Component.translatable(
                            "tooltip.kogasa_addiction.cirno.revive_status",
                            KogasaNumberFormatHelper.formatTrimmed(cost, 2));
                    reviveLine.withStyle(canRevive ? ChatFormatting.GREEN : ChatFormatting.RED);
                    tooltip.add(reviveLine);
                }
            }

            if (abilitySet.contains(KogasaAbility.KOISHI)) {
                boolean isRangedWeapon = stack.getItem() instanceof BowItem
                        || stack.getItem() instanceof CrossbowItem;
                if (isRangedWeapon) {
                    if (Config.ENABLE_KOISHI_RANGED.get()) {
                        double mult = Config.KOISHI_RANGED_CRIT_MULTIPLIER.get();
                        double specialTargetMult = Config.KOISHI_PROJECTILE_IMMUNE_MULTIPLIER.get();
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.koishi.ranged_crit_info",
                                        KogasaNumberFormatHelper.formatPercentTrimmed(Config.KOISHI_RANGED_CRIT_CHANCE.get(), 0),
                                        KogasaNumberFormatHelper.formatTrimmed(mult, 2))
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.koishi.ranged_special_targets",
                                        KogasaNumberFormatHelper.formatTrimmed(specialTargetMult, 2),
                                        KogasaNumberFormatHelper.formatTrimmed(specialTargetMult * specialTargetMult, 2))
                                .withStyle(ChatFormatting.GRAY));
                    } else {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.koishi.ranged_disabled")
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                } else {
                    double mult = Config.KOISHI_CRIT_MULTIPLIER.get();
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.koishi.crit_info",
                                    KogasaNumberFormatHelper.formatPercentTrimmed(Config.KOISHI_CRIT_CHANCE.get(), 0),
                                    KogasaNumberFormatHelper.formatTrimmed(mult, 2))
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }

            if (abilitySet.contains(KogasaAbility.KOGASA)) {
                double reMult = KogasaEquipmentHelper.getReinforcementMultiplier(stack);
                double totalMult = KogasaEquipmentHelper.getTotalMultiplier(stack);

                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.multiplier.reinforcement",
                                KogasaNumberFormatHelper.formatTrimmed(reMult, 2))
                        .withStyle(ChatFormatting.YELLOW));

                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.multiplier.total",
                                KogasaNumberFormatHelper.formatTrimmed(totalMult, 3))
                        .withStyle(ChatFormatting.GOLD));
            }

            if (abilitySet.contains(KogasaAbility.SAKUYA)) {
                if (!ClientTimestopState.isFrozen()) {
                    tooltip.add(Component.translatable("tooltip.kogasa_addiction.timestop.available")
                            .withStyle(ChatFormatting.BLUE));
                } else {
                    tooltip.add(Component.translatable("tooltip.kogasa_addiction.timestop.unavailable")
                            .withStyle(ChatFormatting.RED));
                }
            }

            // ===== 天子能力 =====
            if (abilitySet.contains(KogasaAbility.TENSHI)) {
                boolean isArmor = stack.getItem() instanceof ArmorItem;
                boolean isWeapon = stack.getItem() instanceof SwordItem
                        || stack.getItem() instanceof DiggerItem
                        || stack.getItem() instanceof BowItem
                        || stack.getItem() instanceof CrossbowItem
                        || stack.getItem() instanceof TridentItem;

                // 护甲专属描述与动态状态
                if (isArmor) {
                    double threshold = Config.TENSHI_DOMAIN_THRESHOLD.get();

                    // 动态：读取玩家当前蓄力状态
                    net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
                    if (player != null) {
                        boolean hasTenshiArmor = false;
                        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                            if (KogasaAbilityData.hasAbility(player.getItemBySlot(slot), KogasaAbility.TENSHI)) {
                                hasTenshiArmor = true;
                                break;
                            }
                        }

                        if (hasTenshiArmor) {
                            double charge = player.getPersistentData().getDouble("KogasaTenshiCharge");
                            boolean isReady = player.getPersistentData().getBoolean("KogasaTenshiBurstReady");
                            int resistance = (int) Math.min(5, Math.floor(charge / (threshold / 4.0)));

                            if (isReady) {
                                tooltip.add(Component.translatable("tooltip.kogasa_addiction.tenshi.ready")
                                        .withStyle(ChatFormatting.AQUA));
                            } else {
                                tooltip.add(Component.translatable("tooltip.kogasa_addiction.tenshi.charge",
                                                KogasaNumberFormatHelper.formatTrimmed(charge, 1),
                                                KogasaNumberFormatHelper.formatTrimmed(threshold, 1),
                                                resistance)
                                        .withStyle(ChatFormatting.DARK_AQUA));
                            }
                        }
                    }
                }

                // 武器专属描述
                if (isWeapon) {
                    // 动态：显示玩家当前身上的正面 Buff
                    net.minecraft.client.player.LocalPlayer player = net.minecraft.client.Minecraft.getInstance().player;
                    if (player != null) {
                        List<Component> buffs = player.getActiveEffects().stream()
                                .filter(e -> e.getEffect().isBeneficial())
                                .<Component>map(e -> Component.translatable(e.getEffect().getDescriptionId())
                                        .append(" ")
                                        .append(Component.translatable("enchantment.level." + (e.getAmplifier() + 1))))
                                .toList();

                        if (!buffs.isEmpty()) {
                            MutableComponent buffLine = Component.literal("");
                            for (Component buff : buffs) {
                                buffLine.append(buff).append(" ");
                            }
                            tooltip.add(Component.translatable("tooltip.kogasa_addiction.tenshi.buffs", buffLine)
                                    .withStyle(ChatFormatting.AQUA));
                        }
                    }
                }
            }

            if (KogasaAbilityData.hasAmmo(stack)) {
                var ammoType = KogasaAbilityData.getAmmoType(stack);
                if (ammoType == KogasaAbilityData.AmmoType.POTION) {
                    ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(stack);
                    if (potionId != null) {
                        KogasaAbilityData.PotionKind kind = KogasaAbilityData.getAmmoPotionKind(stack);
                        Component potionName = KogasaDisplayHelper.getPotionDisplayName(potionId, kind);
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ammo.potion", potionName)
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                    var potion = ForgeRegistries.POTIONS.getValue(potionId);
                    if (potion != null) {
                        for (var effect : potion.getEffects()) {
                            tooltip.add(buildEffectLine(effect));
                        }
                    }
                } else if (ammoType == KogasaAbilityData.AmmoType.FIREWORK) {
                    tooltip.add(Component.translatable("tooltip.kogasa_addiction.ammo.firework")
                            .withStyle(ChatFormatting.GOLD));

                    // 解析烟花 NBT 并显示详情
                    ItemStack fireworkStack = KogasaAbilityData.getFireworkStack(stack);
                    if (!fireworkStack.isEmpty()) {
                        tooltip.addAll(getFireworkInfo(fireworkStack));
                    }
                }
            }

            // ===== 远程武器专属：穿透、多发、不消耗 =====
            if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) {
                int pierceLevel = KogasaEquipmentHelper.getBowPierceLevel(stack);
                int arrowCount = KogasaEquipmentHelper.getBowArrowCount(stack);

                if (pierceLevel > 0) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.ranged.pierce", pierceLevel)
                            .withStyle(ChatFormatting.AQUA));
                }
                if (arrowCount > 1) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.ranged.multishot", arrowCount)
                            .withStyle(ChatFormatting.AQUA));
                }

                // ===== 概率不消耗箭矢 =====
                double noConsumeChance = KogasaEquipmentHelper.getNoConsumeArrowChance(stack);
                if (noConsumeChance > 0.0) {
                    if (noConsumeChance >= 1.0) {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ranged.no_consume.max")
                                .withStyle(ChatFormatting.GOLD));
                    } else {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ranged.no_consume",
                                        KogasaNumberFormatHelper.formatPercentTrimmed(noConsumeChance, 0))
                                .withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    // 未解锁：显示还差多少成长值
                    double start = Config.NO_CONSUME_ARROW_START_GROWTH.get();
                    if (start > 0) {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ranged.no_consume.locked",
                                        KogasaNumberFormatHelper.formatPercentTrimmed(growth, 0),
                                        KogasaNumberFormatHelper.formatPercentTrimmed(start, 0))
                                .withStyle(ChatFormatting.DARK_GRAY));
                    }
                }
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.alt")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * 解析烟花火箭的 NBT，返回多行描述
     */
    private static List<Component> getFireworkInfo(ItemStack fireworkStack) {
        List<Component> lines = new ArrayList<>();
        CompoundTag nbt = fireworkStack.getTag();
        if (nbt == null || !nbt.contains("Fireworks", Tag.TAG_COMPOUND)) {
            return lines;
        }

        CompoundTag fireworks = nbt.getCompound("Fireworks");

        // 飞行时间
        if (fireworks.contains("Flight", Tag.TAG_BYTE)) {
            int flight = fireworks.getByte("Flight");
            lines.add(Component.translatable(
                            "tooltip.kogasa_addiction.ammo.firework.flight", flight)
                    .withStyle(ChatFormatting.GRAY));
        }

        // 爆炸列表
        ListTag explosions = fireworks.getList("Explosions", Tag.TAG_COMPOUND);
        if (explosions.isEmpty()) {
            lines.add(Component.translatable(
                            "tooltip.kogasa_addiction.ammo.firework.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        for (int i = 0; i < explosions.size(); i++) {
            CompoundTag exp = explosions.getCompound(i);
            lines.add(buildExplosionLine(exp, i + 1, explosions.size()));
        }

        return lines;
    }

    private static Component buildExplosionLine(CompoundTag exp, int index, int total) {
        // 形状
        int type = exp.getByte("Type");
        String shapeKey = switch (type) {
            case 0 -> "item.minecraft.firework_star.shape.small_ball";
            case 1 -> "item.minecraft.firework_star.shape.large_ball";
            case 2 -> "item.minecraft.firework_star.shape.star";
            case 3 -> "item.minecraft.firework_star.shape.creeper";
            case 4 -> "item.minecraft.firework_star.shape.burst";
            default -> "item.minecraft.firework_star.shape.small_ball";
        };

        MutableComponent line = Component.literal("  " + index + ". ")
                .append(Component.translatable(shapeKey));

        // 颜色（主色 + 渐变色）
        int[] colors = exp.getIntArray("Colors");
        int[] fadeColors = exp.getIntArray("FadeColors");

        if (colors.length > 0) {
            line.append(Component.literal(" "));
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) line.append(Component.literal("/"));
                line.append(colorComponent(colors[i]));
            }
        }
        if (fadeColors.length > 0) {
            line.append(Component.literal(" → "));
            for (int i = 0; i < fadeColors.length; i++) {
                if (i > 0) line.append(Component.literal("/"));
                line.append(colorComponent(fadeColors[i]));
            }
        }

        // 拖尾 / 闪烁
        List<Component> flags = new ArrayList<>();
        if (exp.getBoolean("Trail")) {
            flags.add(Component.translatable(
                    "tooltip.kogasa_addiction.ammo.firework.trail"));
        }
        if (exp.getBoolean("Flicker")) {
            flags.add(Component.translatable(
                    "tooltip.kogasa_addiction.ammo.firework.flicker"));
        }
        if (!flags.isEmpty()) {
            line.append(Component.literal(" ["));
            for (int i = 0; i < flags.size(); i++) {
                if (i > 0) line.append(Component.literal(", "));
                line.append(flags.get(i));
            }
            line.append(Component.literal("]"));
        }

        return line.withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    /**
     * int RGB → 颜色名 Component（优先匹配原版染料色）
     */
    private static Component colorComponent(int rgb) {
        for (DyeColor dye : DyeColor.values()) {
            if (dye.getFireworkColor() == rgb) {
                return Component.translatable(
                                "item.minecraft.firework_star." + dye.getName())
                        .withStyle(style -> style.withColor(dye.getTextColor()));
            }
        }
        // 非原版颜色：显示十六进制
        return Component.literal(String.format("#%06X", rgb))
                .withStyle(ChatFormatting.GRAY);
    }

    private static Component buildEffectLine(MobEffectInstance effect) {
        Component name = effect.getEffect().getDisplayName();
        int level = effect.getAmplifier() + 1;

        MutableComponent line = Component.literal("  ").append(name);

        if (level > 1) {
            line.append(Component.literal(" ")
                    .append(Component.translatable("enchantment.level." + level)));
        }

        return line.withStyle(ChatFormatting.GRAY);
    }
}