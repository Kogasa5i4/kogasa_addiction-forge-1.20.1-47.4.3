package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.ModConfigCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

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
                                String.format("%.1f", damage))
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
                            String.format("%.2f", growthMult))
                    .withStyle(ChatFormatting.YELLOW));

            // 已吸收装备
            if (used.isEmpty()) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.reinforcement.none")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (String id : used) {
                    Component itemName = getItemDisplayName(id);
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.reinforcement.entry", itemName)
                            .withStyle(ChatFormatting.GRAY));
                }
            }

            // 吸收值
            if (absorbed > 0) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.absorbed", String.format("%.1f", absorbed))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // ===== 成长信息（Ctrl，仅解锁后显示） =====
        if (re >= ModConfigCache.growthUnlockReinforcements) {
            if (Screen.hasControlDown()) {
                String percent = String.format("%.2f%%", growth * 100);
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.growth", percent)
                        .withStyle(ChatFormatting.GREEN));

                // 新增：成长附魔进度
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

            // 标题
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.abilities", abilities.size(), maxSlots)
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
            // 槽位满且未解锁第 2 槽 → 显示解锁提示（只在有能力时显示）
            if (abilities.size() >= maxSlots && maxSlots < 2) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.abilities.full_hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.AYA)) {
                double news = KogasaAbilityData.getNewsValue(stack);
                int killed = KogasaAbilityData.getKilledMobCount(stack);
                int mined = KogasaAbilityData.getMinedBlockCount(stack);
                int damage = KogasaAbilityData.getDamageTakenCount(stack);

                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.aya.news_value",
                                String.format("%.2f%%", news * 100),
                                String.format("×%.3f", 1.0 + news))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.aya.records",
                                killed, mined, damage)
                        .withStyle(ChatFormatting.GRAY));
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.CIRNO)) {
                double cost = Config.CIRNO_REVIVE_GROWTH_COST.get();
                boolean canRevive = growth >= cost;

                MutableComponent reviveLine = Component.translatable(
                        "tooltip.kogasa_addiction.cirno.revive_status",
                        String.format("%.2f", cost));
                reviveLine.withStyle(canRevive ? ChatFormatting.GREEN : ChatFormatting.RED);
                tooltip.add(reviveLine);
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.KOISHI)) {
                double chance = Config.KOISHI_CRIT_CHANCE.get() * 100;
                double mult = Config.KOISHI_CRIT_MULTIPLIER.get();
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.koishi.crit_info",
                                String.format("%.0f%%", chance),
                                String.format("%.1f", mult))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.KOGASA)) {
                double reMult = KogasaEquipmentHelper.getReinforcementMultiplier(stack);
                double totalMult = KogasaEquipmentHelper.getTotalMultiplier(stack);

                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.multiplier.reinforcement",
                                String.format("%.2f", reMult))
                        .withStyle(ChatFormatting.YELLOW));

                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.multiplier.total",
                                String.format("%.3f", totalMult))
                        .withStyle(ChatFormatting.GOLD));
            }

            if (KogasaAbilityData.hasAmmo(stack)) {
                var ammoType = KogasaAbilityData.getAmmoType(stack);
                if (ammoType == KogasaAbilityData.AmmoType.POTION) {
                    ResourceLocation potionId = KogasaAbilityData.getAmmoPotionId(stack);
                    if (potionId != null) {
                        KogasaAbilityData.PotionKind kind = KogasaAbilityData.getAmmoPotionKind(stack);
                        Component potionName = getPotionDisplayName(potionId, kind);
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

            // ===== 远程武器专属：穿透、多发 =====
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
                                        String.format("%.0f%%", noConsumeChance * 100))
                                .withStyle(ChatFormatting.GOLD));
                    }
                } else {
                    // 未解锁：显示还差多少成长值
                    double start = Config.NO_CONSUME_ARROW_START_GROWTH.get();
                    if (start > 0) {
                        tooltip.add(Component.translatable(
                                        "tooltip.kogasa_addiction.ranged.no_consume.locked",
                                        String.format("%.0f%%", growth * 100),   // ← 当前值
                                        String.format("%.0f%%", start * 100))    // ← 目标值
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
     * 把 "minecraft:iron_sword" 转成玩家当前语言下的物品显示名
     */
    private static Component getItemDisplayName(String id) {
        // 处理 "arrow:" 前缀：剥掉后按物品 ID 解析
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
            // 显示"16× 箭"而不是"箭"，提示消耗数量
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

    private static String formatDuration(int totalSeconds) {
        if (totalSeconds < 60) {
            return totalSeconds + "s";
        }
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}