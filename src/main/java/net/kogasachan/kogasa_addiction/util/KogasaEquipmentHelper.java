package net.kogasachan.kogasa_addiction.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.datagen.ModItemTagsProvider;
import net.kogasachan.kogasa_addiction.item.FumoItem;
import net.kogasachan.kogasa_addiction.item.SakuyaKnifeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
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
            float baseSpeed = (float) getBaseAttackSpeed(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, baseSpeed);
        }

        if (item instanceof TridentItem trident) {
            double baseDamage = 1.0 + 8.0;
            double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, -2.9F);
        }

        // 其他未单独处理的 Kogasa 装备（如弓弩）使用默认属性
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
     * 耐久上限
     */
    public static int getMaxDamage(ItemStack stack, int baseMaxDamage) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        int fixedPerRe = Config.DURABILITY_FIXED_PER_REINFORCEMENT.get();
        double ratioPerRe = Config.DURABILITY_RATIO_PER_REINFORCEMENT.get();
        double multiplier = (1.0 + re * ratioPerRe) * (1.0 + growth) * getNewsMultiplier(stack);
        return (int) (baseMaxDamage * multiplier + re * fixedPerRe);
    }

    /**
     * 获取一件装备的“基础价值”，用于铁砧强化时累加。
     * - 剑/工具：物品自身的攻击伤害
     * - 护甲：防御值
     * - 三叉戟：固定 8.0
     * - 其他：0
     */
    public static double getEquipmentBaseValue(ItemStack stack) {
        Item item = stack.getItem();

        if (item instanceof SwordItem sword) {
            return sword.getDamage();
        }
        if (item instanceof DiggerItem digger) {
            return digger.getAttackDamage();
        }
        if (item instanceof ArmorItem armor) {
            return armor.getDefense();
        }
        if (item instanceof TridentItem) {
            return 8.0;
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
        double multiplier = (1.0 + re * ratio) * (1.0 + growth) * getNewsMultiplier(stack);
        return (float) (baseSpeed * multiplier);
    }

    /**
     * 攻击速度
     */
    public static double getAttackSpeedModifier(ItemStack stack, double baseSpeed) {
        double growth = KogasaEquipmentData.getGrowth(stack);
        double news = getAyaNews(stack);
        double ratio = Config.ATTACK_SPEED_GROWTH_RATIO.get();

        // progress 加上 news 的贡献
        double progress = Math.min((growth + news) * ratio, 1.0);
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
        } else if (item instanceof TridentItem) {
            baseDamage = 1.0 + 8.0;
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
        if (stack.getItem() instanceof FumoItem fumo) return -2.0;
        if (stack.getItem() instanceof SakuyaKnifeItem sakuyaKnifeItem) return -2.9;
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id == null) return -2.8;
        String path = id.getPath();
        if (path.endsWith("_sword")) return -2.4;
        if (path.endsWith("_axe")) return -3.2;
        if (path.endsWith("_hoe")) return -3.0;
        if (path.endsWith("_shovel")) return -3.0;
        return -2.8; // 未识别的工具/武器（含镐）
    }

    /**
     * 攻速是否已封顶
     */
    public static boolean isAttackSpeedCapped(ItemStack stack) {
        double growth = KogasaEquipmentData.getGrowth(stack);
        double news = getAyaNews(stack);
        double ratio = Config.ATTACK_SPEED_GROWTH_RATIO.get();
        return (growth + news) * ratio >= 1.0;
    }

    /**
     * 击退抗性
     */
    public static double getKnockbackResistance(ItemStack stack) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        double news = getAyaNews(stack);
        double base = Config.KNOCKBACK_BASE_RESISTANCE.get();
        double max = Config.KNOCKBACK_MAX_PER_PIECE.get();

        // 有效成长值 = growth + news，加速逼近上限
        double effectiveGrowth = growth + news;

        double effectiveMax = Math.min(max * (1.0 + re * 0.5), 1.0);
        return effectiveMax - (effectiveMax - base) / (effectiveGrowth + 1.0);
    }

    /**
     * 强化倍率（仅在拥有小伞能力时生效）
     */
    public static double getReinforcementMultiplier(ItemStack stack) {
        if (!KogasaAbilityData.hasAbility(stack, KogasaAbility.KOGASA)) {
            return 1.0;
        }
        int re = KogasaEquipmentData.getReinforcements(stack);
        double perLevel = Config.KOGASA_REINFORCEMENT_MULTIPLIER_PER_LEVEL.get();
        return 1.0 + re * perLevel;
    }

    /**
     * 成长倍率：1.0 + 成长值
     */
    public static double getGrowthMultiplier(ItemStack stack) {
        return 1.0 + KogasaEquipmentData.getGrowth(stack);
    }

    /**
     * 综合倍率：强化 × 成长 × AYA 新闻值倍率
     */
    public static double getTotalMultiplier(ItemStack stack) {
        return getReinforcementMultiplier(stack)
                * getGrowthMultiplier(stack)
                * getNewsMultiplier(stack);
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
        if (item instanceof FumoItem) return Tags.Items.TOOLS;
        if (item instanceof SakuyaKnifeItem) return Tags.Items.TOOLS_TRIDENTS;
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
     * 返回装备类型的后缀标识（不含下划线），例如 "sword"、"pickaxe"。
     * Fumo 返回 null；咲夜飞刀返回 "knife"。
     */
    public static String getEquipmentSuffix(ItemStack stack) {
        Item item = stack.getItem();

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (id == null) return null;
        String path = id.getPath();

        if (path.endsWith("sword")) return "sword";
        if (path.endsWith("pickaxe")) return "pickaxe";
        if (path.endsWith("axe")) return "axe";
        if (path.endsWith("shovel")) return "shovel";
        if (path.endsWith("hoe")) return "hoe";
        if (path.endsWith("helmet")) return "helmet";
        if (path.endsWith("chestplate")) return "chestplate";
        if (path.endsWith("leggings")) return "leggings";
        if (path.endsWith("boots")) return "boots";
        if (path.endsWith("knife")) return "knife";
        return null;
    }

    /**
     * 判断 right 是否是 left 的有效强化材料。
     * 先走 tag 匹配；tag 不匹配时退回注册名后缀匹配。
     * 三叉戟类特例：接受剑、三叉戟或飞刀。
     */
    public static boolean matchesReinforcementMaterial(ItemStack left, ItemStack right) {
        TagKey<Item> tag = getMatchingTag(left);
        if (tag != null && right.is(tag)) return true;

        if (left.getItem() instanceof TridentItem) {
            if (right.is(ItemTags.SWORDS) || right.is(Tags.Items.TOOLS_TRIDENTS) || right.is(ModItemTagsProvider.KNIVES))
                return true;
            ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
            if (rightId == null) return false;
            String path = rightId.getPath();
            return path.endsWith("sword") || path.endsWith("trident") || path.endsWith("knife");
        }

        String suffix = getEquipmentSuffix(left);
        if (suffix == null) return false;

        ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
        if (rightId == null) return false;
        return rightId.getPath().endsWith("_" + suffix);
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

    /**
     * AYA 新闻值倍率。没有 AYA 能力时返回 1.0。
     */
    public static double getNewsMultiplier(ItemStack stack) {
        if (!KogasaAbilityData.hasAbility(stack, KogasaAbility.AYA)) {
            return 1.0;
        }
        return 1.0 + KogasaAbilityData.getNewsValue(stack);
    }

    /**
     * 弓弩穿透等级：成长值达到阈值时返回 1
     */
    public static int getBowPierceLevel(ItemStack stack) {
        double growth = KogasaEquipmentData.getGrowth(stack);
        return growth >= Config.BOW_PIERCE_GROWTH.get() ? 1 : 0;
    }

    /**
     * 弓弩多发数量
     */
    public static int getBowArrowCount(ItemStack stack) {
        double growth = KogasaEquipmentData.getGrowth(stack);
        if (growth >= Config.BOW_MULTISHOT_3_GROWTH.get()) return 3;
        if (growth >= Config.BOW_MULTISHOT_2_GROWTH.get()) return 2;
        return 1;
    }

    /**
     * 弩是否解锁自动装填
     */
    public static boolean isCrossbowAutoReload(ItemStack stack) {
        double threshold = Config.CROSSBOW_AUTO_RELOAD_GROWTH.get();
        if (threshold <= 0.0) return false;
        return KogasaEquipmentData.getGrowth(stack) >= threshold;
    }

    /**
     * 对药水效果应用 Cirno 增强（默认等级 +2、时长 ×1.5）：
     * - 缓慢特殊处理：至少 V 级（amplifier = 4）
     */
    public static List<MobEffectInstance> applyCirnoPotionBoost(
            List<MobEffectInstance> original) {
        List<MobEffectInstance> boosted = new ArrayList<>();
        int levelBonus = Config.CIRNO_POTION_LEVEL_BONUS.get();
        float durationMultiplier = Config.CIRNO_POTION_DURATION_MULTIPLIER.get().floatValue();

        for (MobEffectInstance effect : original) {
            int newAmplifier = effect.getAmplifier() + levelBonus;
            int newDuration = (int) (effect.getDuration() * durationMultiplier);

            if (effect.getEffect() == MobEffects.MOVEMENT_SLOWDOWN) {
                newAmplifier = Math.max(newAmplifier, 4);  // 缓慢至少 V
            }

            boosted.add(new MobEffectInstance(
                    effect.getEffect(), newDuration, newAmplifier,
                    effect.isAmbient(), effect.isVisible(), effect.showIcon()));
        }
        return boosted;
    }

    /**
     * 不消耗箭矢的概率。
     * growth < start       → 0.0
     * growth >= full       → 1.0
     * 其余                 → 线性插值
     */
    public static double getNoConsumeArrowChance(ItemStack stack) {
        double growth = KogasaEquipmentData.getGrowth(stack);
        double start = Config.NO_CONSUME_ARROW_START_GROWTH.get();
        double full = Config.NO_CONSUME_ARROW_FULL_GROWTH.get();

        if (growth < start) return 0.0;
        if (full <= start) return 1.0;
        if (growth >= full) return 1.0;
        return (growth - start) / (full - start);
    }

    /**
     * 有 AYA 能力时返回新闻值，否则 0.0
     */
    private static double getAyaNews(ItemStack stack) {
        return KogasaAbilityData.hasAbility(stack, KogasaAbility.AYA)
                ? KogasaAbilityData.getNewsValue(stack) : 0.0;
    }
}