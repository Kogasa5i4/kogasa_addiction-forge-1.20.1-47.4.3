package net.kogasachan.kogasa_addiction.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.UUID;

public class KogasaEquipmentHelper {

    private static final UUID WEAPON_DAMAGE_UUID =
            UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID WEAPON_SPEED_UUID =
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    private static final UUID[] ARMOR_UUIDS = {
            UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), // FEET
            UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), // LEGS
            UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), // CHEST
            UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")  // HEAD
    };

    /**
     * 统一入口：根据物品类型分派属性计算
     */
    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            Item item, EquipmentSlot slot, ItemStack stack) {

        if (item instanceof ArmorItem armor) {
            if (slot != armor.getEquipmentSlot()) {
                return armor.getDefaultAttributeModifiers(slot);
            }
            return buildArmorModifiers(stack, armor, slot);
        }

        if (slot != EquipmentSlot.MAINHAND) {
            return item.getDefaultAttributeModifiers(slot);
        }

        if (item instanceof SwordItem sword) {
            double baseDamage = 1.0 + sword.getDamage();
            double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, -2.4F);
        }

        if (item instanceof DiggerItem digger) {
            double baseDamage = 1.0 + digger.getAttackDamage();
            double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
            float baseSpeed = getDiggerAttackSpeed(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, baseSpeed);
        }

        if (item instanceof HoeItem hoe) {
            double baseDamage = 1.0 + hoe.getAttackDamage();
            double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, -3.0F);
        }

        // 其他类型的 Kogasa 装备（暂时没有）
        return item.getDefaultAttributeModifiers(slot);
    }

    /**
     * 武器/工具的攻击力与攻速
     */
    private static Multimap<Attribute, AttributeModifier> buildHandModifiers(
            ItemStack stack, double baseDamage, double absorbed, double baseSpeed) {
        double multiplier = KogasaEquipmentData.getMultiplier(stack);

        double totalDamage;
        if (Config.ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER.get()) {
            totalDamage = (baseDamage + absorbed) * multiplier;
        } else {
            totalDamage = baseDamage * multiplier + absorbed;
        }

        double totalSpeed = getAttackSpeedModifier(stack, baseSpeed);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                WEAPON_DAMAGE_UUID, "Kogasa modifier",
                totalDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                WEAPON_SPEED_UUID, "Kogasa modifier",
                totalSpeed, AttributeModifier.Operation.ADDITION));
        return builder.build();
    }

    /**
     * 护甲防御、韧性与击退抗性
     */
    private static Multimap<Attribute, AttributeModifier> buildArmorModifiers(
            ItemStack stack, ArmorItem armor, EquipmentSlot slot) {
        double multiplier = KogasaEquipmentData.getMultiplier(stack);
        double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
        double baseDefense = armor.getDefense();

        double totalDefense;
        if (Config.ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER.get()) {
            totalDefense = (baseDefense + absorbed) * multiplier;
        } else {
            totalDefense = baseDefense * multiplier + absorbed;
        }

        double totalToughness = armor.getToughness() * multiplier;
        double totalKnockback = getKnockbackResistance(stack);

        UUID uuid = switch (slot) {
            case LEGS -> ARMOR_UUIDS[1];
            case CHEST -> ARMOR_UUIDS[2];
            case HEAD -> ARMOR_UUIDS[3];
            default -> ARMOR_UUIDS[0];
        };

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();

        builder.put(Attributes.ARMOR, new AttributeModifier(
                uuid, "Kogasa armor",
                totalDefense, AttributeModifier.Operation.ADDITION));

        if (totalToughness > 0) {
            builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(
                    uuid, "Kogasa toughness",
                    totalToughness, AttributeModifier.Operation.ADDITION));
        }

        if (totalKnockback > 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(
                    uuid, "Kogasa knockback",
                    totalKnockback, AttributeModifier.Operation.ADDITION));
        }

        return builder.build();
    }

    /**
     * DiggerItem 的攻击速度按注册名后缀区分
     */
    private static float getDiggerAttackSpeed(ItemStack stack) {
        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return -2.8F;
        String path = id.getPath();
        if (path.endsWith("_axe")) return -3.2F;
        if (path.endsWith("_hoe")) return -3.0F;
        if (path.endsWith("_shovel")) return -3.0F;
        return -2.8F; // 镐
    }

    /**
     * 耐久上限
     */
    public static int getMaxDamage(ItemStack stack, int baseMaxDamage) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        int fixedPerRe = Config.DURABILITY_FIXED_PER_REINFORCEMENT.get();
        double ratioPerRe = Config.DURABILITY_RATIO_PER_REINFORCEMENT.get();
        double multiplier = 1.0 + re * ratioPerRe;
        return (int) ((baseMaxDamage * multiplier + re * fixedPerRe) * (1.0 + growth));
    }

    /**
     * 获取一件装备的“基础价值”，用于铁砧强化时累加。
     * - 剑/工具：Tier 加成 + 附加伤害
     * - 护甲：防御值
     * - 其他：0
     */
    public static double getEquipmentBaseValue(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof SwordItem sword) {
            return sword.getDamage();  // Tier 加成 + 附加伤害
        }
        if (item instanceof DiggerItem digger) {
            return digger.getAttackDamage();  // Tier 加成 + 附加伤害
        }
        if (item instanceof ArmorItem armor) {
            return armor.getDefense();
        }
        if (item instanceof HoeItem hoe) {
            return hoe.getTier().getAttackDamageBonus();
        }
        return 0;
    }

    /**
     * 挖掘速度
     */
    public static float getDestroySpeed(ItemStack stack, float baseSpeed) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        double ratio = Config.MINING_SPEED_RATIO_PER_REINFORCEMENT.get();
        double multiplier = (1.0 + re * ratio) * (1.0 + growth);
        return (float) (baseSpeed * multiplier);
    }

    /**
     * 攻速成长：修饰符从 baseSpeed 逼近 0.0，达到后封顶。
     * progress = min(growth × ratio, 1.0)
     */
    public static double getAttackSpeedModifier(ItemStack stack, double baseSpeed) {
        double growth = KogasaEquipmentData.getGrowth(stack);
        double ratio = Config.ATTACK_SPEED_GROWTH_RATIO.get();
        double progress = Math.min(growth * ratio, 1.0);
        return baseSpeed * (1.0 - progress);
    }

    /**
     * 用于 Tooltip 显示
     */
    public static double getDisplayAttackDamage(ItemStack stack) {
        Item item = stack.getItem();
        double baseDamage = 0;
        if (item instanceof SwordItem sword) {
            baseDamage = 1.0 + sword.getDamage();
        } else if (item instanceof DiggerItem digger) {
            baseDamage = 1.0 + digger.getAttackDamage();
        }
        double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
        if (Config.ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER.get()) {
            return (baseDamage + absorbed) * KogasaEquipmentData.getMultiplier(stack);
        } else {
            return baseDamage * KogasaEquipmentData.getMultiplier(stack) + absorbed;
        }
    }

    public static double getDisplayAttackSpeed(ItemStack stack) {
        double baseSpeed = getBaseAttackSpeed(stack);
        double cap = Config.ATTACK_SPEED_CAP.get();
        return cap + getAttackSpeedModifier(stack, baseSpeed);
    }

    /**
     * 按注册名返回物品的基础攻速修饰符
     */
    public static double getBaseAttackSpeed(ItemStack stack) {
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return -2.8;
        String path = id.getPath();
        if (path.endsWith("_sword")) return -2.4;
        if (path.endsWith("_axe")) return -3.2;
        if (path.endsWith("_hoe")) return -3.0;
        if (path.endsWith("_shovel")) return -3.0;
        return -2.8; // 镐
    }

    /**
     * 攻速是否已封顶
     */
    public static boolean isAttackSpeedCapped(ItemStack stack) {
        double growth = KogasaEquipmentData.getGrowth(stack);
        double ratio = Config.ATTACK_SPEED_GROWTH_RATIO.get();
        return growth * ratio >= 1.0;
    }

    /**
     * 击退抗性
     */
    public static double getKnockbackResistance(ItemStack stack) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        double base = Config.KNOCKBACK_BASE_RESISTANCE.get();
        double max = Config.KNOCKBACK_MAX_PER_PIECE.get();
        // 融合增加"最大上限"，成长逼近该上限
        double effectiveMax = Math.min(max * (1.0 + re * 0.5), 1.0);
        return effectiveMax - (effectiveMax - base) / (growth + 1.0);
    }

    /**
     * 强化倍率：1.0 + 强化次数 × 0.1
     */
    public static double getReinforcementMultiplier(ItemStack stack) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        double perLevel = Config.REINFORCEMENT_MULTIPLIER_PER_LEVEL.get();
        return 1.0 + re * perLevel;
    }

    /**
     * 成长倍率：1.0 + 成长值
     */
    public static double getGrowthMultiplier(ItemStack stack) {
        return 1.0 + KogasaEquipmentData.getGrowth(stack);
    }

    /**
     * 综合倍率：强化 × 成长
     */
    public static double getTotalMultiplier(ItemStack stack) {
        return getReinforcementMultiplier(stack) * getGrowthMultiplier(stack);
    }

    /**
     * 计算该装备实际可用的强化上限：
     * 基础上限 + 成长解锁的额外槽位
     */
    public static int getEffectiveMaxReinforcements(ItemStack stack) {
        int base = Config.MAX_REINFORCEMENTS.get();
        if (!Config.ENABLE_EXTRA_SLOTS.get()) return base;

        double growth = KogasaEquipmentData.getGrowth(stack);
        double perSlot = Config.GROWTH_PER_EXTRA_SLOT.get();
        int maxExtra = Config.MAX_EXTRA_SLOTS.get();

        int extra = (int) Math.floor(growth / perSlot);
        extra = Math.min(extra, maxExtra);

        return base + extra;
    }

    /**
     * 根据左侧 Kogasa 物品返回对应的匹配标签
     */
    public static TagKey<Item> getMatchingTag(ItemStack stack) {
        Item item = stack.getItem();
        // 用注册名判断，避免为每种类型创建单独的类判断
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return null;
        String path = id.getPath();

        if (path.endsWith("_sword")) return ItemTags.SWORDS;
        if (path.endsWith("_pickaxe")) return ItemTags.PICKAXES;
        if (path.endsWith("_axe")) return ItemTags.AXES;
        if (path.endsWith("_shovel")) return ItemTags.SHOVELS;
        if (path.endsWith("_hoe")) return ItemTags.HOES;
        if (path.endsWith("_helmet")) return Tags.Items.ARMORS_HELMETS;
        if (path.endsWith("_chestplate")) return Tags.Items.ARMORS_CHESTPLATES;
        if (path.endsWith("_leggings")) return Tags.Items.ARMORS_LEGGINGS;
        if (path.endsWith("_boots")) return Tags.Items.ARMORS_BOOTS;
        return null;
    }

    /**
     * 根据材料类型返回吸收值比例
     */
    public static double getAbsorbedRatio(ItemStack material) {
        if (material.getItem() instanceof ArmorItem) {
            return Config.ARMOR_ABSORBED_RATIO.get();
        }
        return Config.WEAPON_ABSORBED_RATIO.get();
    }

    /**
     * 动态挖掘等级：max(Tier基础等级, 强化次数)
     */
    public static int getDynamicHarvestLevel(ItemStack stack, int baseLevel) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        return Math.max(baseLevel, re);
    }
}