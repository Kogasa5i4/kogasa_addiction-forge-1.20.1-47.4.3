package net.kogasachan.kogasa_addiction.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKind;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKinds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

public class KogasaEquipmentHelper {

    private static final UUID WEAPON_DAMAGE_UUID =
            UUID.fromString("CB3F55D3-645C-4F38-A497-9C13A33DB5CF");
    private static final UUID WEAPON_SPEED_UUID =
            UUID.fromString("FA233E1C-4180-4865-B01B-BCCE9785ACA3");

    /**
     * 兜底分支的默认属性。本模组未单独处理属性的装备（弓弩）与"槽位不匹配"的分支
     * （副手持剑、护甲错槽）在原版内建属性恒为空，直接返回共享空表即可；
     * 也顺带绕开已弃用的 {@code Item#getDefaultAttributeModifiers}。
     */
    private static final Multimap<Attribute, AttributeModifier> EMPTY_MODIFIERS =
            ImmutableMultimap.of();

    /** 已警告过的动态数值材料 id，避免每次铁砧都刷日志。 */
    private static final Set<String> WARNED_DYNAMIC_MATERIALS = ConcurrentHashMap.newKeySet();

    /** 原版三叉戟基准攻击伤害的惰性缓存（首次属性计算时读取注册表，之后复用）。 */
    private static Double vanillaTridentDamage = null;

    /**
     * 原版三叉戟的基准攻击伤害：动态读取注册表中 Items.TRIDENT 的内建属性
     * （ATTACK_DAMAGE 的 ADDITION 修饰符之和），代替硬编码 8.0。
     *
     * <p><b>不能用 {@code ItemStack#getAttributeModifiers}</b>：那会触发 Forge 的
     * {@code ItemAttributeModifierEvent}，兼容附属会把“原版三叉戟”也当成兼容物品再算一遍属性，
     * 于是递归到栈溢出（客户端 tooltip 直接崩）。这里直接用物品的内建属性，不经过事件。
     */
    @SuppressWarnings("deprecation")
    private static double getVanillaTridentDamage() {
        if (vanillaTridentDamage == null) {
            double damage = 0.0D;
            for (AttributeModifier modifier : Items.TRIDENT
                    .getDefaultAttributeModifiers(EquipmentSlot.MAINHAND)
                    .get(Attributes.ATTACK_DAMAGE)) {
                if (modifier.getOperation() == AttributeModifier.Operation.ADDITION) {
                    damage += modifier.getAmount();
                }
            }
            vanillaTridentDamage = damage;
        }
        return vanillaTridentDamage;
    }

    private static final UUID[] ARMOR_UUIDS = {
            UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), // FEET
            UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), // LEGS
            UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), // CHEST
            UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")  // HEAD
    };

    /**
     * 空手基础的 +1 只给本模组装备。
     *
     * <p>兼容物品（非 {@link IKogasaEquipment}）走原版数值：攻击伤害修饰符不再额外 +1，
     * 否则实际伤害和 tooltip 都会比原版高 1 点。
     */
    private static double emptyHandBonus(ItemStack stack) {
        return stack.getItem() instanceof IKogasaEquipment ? 1.0 : 0.0;
    }

    /**
     * 统一入口：根据物品类型分派属性计算
     */
    public static Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            Item item, EquipmentSlot slot, ItemStack stack) {

        if (item instanceof ArmorItem armor) {
            if (slot != armor.getEquipmentSlot()) {
                // 护甲错槽：原版内建属性为空
                return EMPTY_MODIFIERS;
            }
            return buildArmorModifiers(stack, armor, slot);
        }

        if (slot != EquipmentSlot.MAINHAND) {
            // 副手持握武器/工具：原版内建属性为空
            return EMPTY_MODIFIERS;
        }

        if (item instanceof SwordItem sword) {
            double baseDamage = emptyHandBonus(stack) + sword.getDamage();
            double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, -2.4F);
        }

        if (item instanceof DiggerItem digger) {
            double baseDamage = emptyHandBonus(stack) + digger.getAttackDamage();
            double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
            float baseSpeed = (float) getBaseAttackSpeed(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, baseSpeed);
        }

        if (item instanceof TridentItem trident) {
            double baseDamage = emptyHandBonus(stack) + getVanillaTridentDamage();
            double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
            return buildHandModifiers(stack, baseDamage, absorbed, -2.9F);
        }

        // 其他未单独处理的 Kogasa 装备（如弓弩）：只带吸收的额外属性
        ImmutableMultimap.Builder<Attribute, AttributeModifier> absorbedOnly = ImmutableMultimap.builder();
        appendAbsorbedAttributes(absorbedOnly, stack, EquipmentSlot.MAINHAND, true);
        return absorbedOnly.build();
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
        appendAbsorbedAttributes(builder, stack, EquipmentSlot.MAINHAND, false);
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

        appendAbsorbedAttributes(builder, stack, slot, false);
        return builder.build();
    }

    /** 把 AbsorbedAttributes 中除攻击/护甲外的属性追加到 modifier 表。 */
    @SuppressWarnings("deprecation")
    private static void appendAbsorbedAttributes(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack, EquipmentSlot slot, boolean includeLegacyAttributes) {
        java.util.Set<ResourceLocation> legacy = java.util.Set.of(
                ResourceLocation.withDefaultNamespace("generic.attack_damage"),
                ResourceLocation.withDefaultNamespace("generic.attack_speed"),
                ResourceLocation.withDefaultNamespace("generic.armor"),
                ResourceLocation.withDefaultNamespace("generic.armor_toughness"),
                ResourceLocation.withDefaultNamespace("generic.knockback_resistance"));

        // 物品本来就有的额外属性也参与成长；攻击/护甲/攻速仍由上面的专用公式处理。
        Map<ResourceLocation, Double> baseAdditive = new java.util.LinkedHashMap<>();
        for (var entry : stack.getItem().getDefaultAttributeModifiers(slot).entries()) {
            if (entry.getValue().getOperation() != AttributeModifier.Operation.ADDITION) continue;
            ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (id == null || (!includeLegacyAttributes && legacy.contains(id))) continue;
            baseAdditive.merge(id, entry.getValue().getAmount(), Double::sum);
        }

        Map<ResourceLocation, Double> absorbedAdditive = KogasaEquipmentData.getAbsorbedAttributes(stack);
        Map<ResourceLocation, Double> absorbedMultiplier = KogasaEquipmentData.getAbsorbedAttributeMultipliers(stack);

        java.util.Set<ResourceLocation> all = new java.util.LinkedHashSet<>(baseAdditive.keySet());
        all.addAll(absorbedAdditive.keySet());
        all.addAll(absorbedMultiplier.keySet());

        for (ResourceLocation id : all) {
            if (!includeLegacyAttributes && legacy.contains(id)) continue;

            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
            if (attribute == null) continue;

            double base = baseAdditive.getOrDefault(id, 0.0D);
            double additive = absorbedAdditive.getOrDefault(id, 0.0D);
            double multiplicative = absorbedMultiplier.getOrDefault(id, 0.0D);
            double multiplier = KogasaAttributeGrowth.multiplier(stack, id);

            // 加法吸收并入基础值，最后统一乘一次综合倍率；乘法吸收单独成乘数池。
            double value = (base + additive) * multiplier * (1.0D + multiplicative);
            value = KogasaAttributeGrowth.applyCap(id, value);
            if (value <= 0.0D) continue;

            UUID uuid = UUID.nameUUIDFromBytes(("kogasa_absorbed:" + id).getBytes(StandardCharsets.UTF_8));
            builder.put(attribute, new AttributeModifier(
                    uuid, "Kogasa growth " + id, value,
                    AttributeModifier.Operation.ADDITION));
        }
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
     * 原版弓的箭矢初速基数（BowItem.releaseUsing 里 power * 3.0F）
     */
    private static final double VANILLA_BOW_POWER = 3.0D;

    /**
     * 原版弩的箭矢初速基数（CrossbowItem.ARROW_POWER）
     */
    private static final double VANILLA_CROSSBOW_POWER = 3.15D;

    /**
     * 原版弓拉满所需 tick（BowItem.MAX_DRAW_DURATION）
     */
    private static final double VANILLA_BOW_DRAW_TICKS = 20.0D;

    /**
     * 原版弩装填所需 tick（CrossbowItem.MAX_CHARGE_DURATION）
     */
    private static final double VANILLA_CROSSBOW_DRAW_TICKS = 25.0D;

    public static boolean isRanged(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    /**
     * 弓弩的"基础发射威力"。
     *
     * <p>强化材料侧的口径：**任何箭矢都视作原版弓弩的等价复制品**，属性完全一致，
     * 因此这里只看左侧这件装备自己是弓还是弩，与材料是什么无关。
     */
    public static double getRangedBasePower(ItemStack rangedStack) {
        if (rangedStack.getItem() instanceof CrossbowItem) return VANILLA_CROSSBOW_POWER;
        if (rangedStack.getItem() instanceof BowItem) return VANILLA_BOW_POWER;
        return 0.0D;
    }

    /**
     * 弓弩的"基础拉弓时间"（tick），口径同上。
     */
    public static double getRangedBaseDrawTicks(ItemStack rangedStack) {
        if (rangedStack.getItem() instanceof CrossbowItem) return VANILLA_CROSSBOW_DRAW_TICKS;
        if (rangedStack.getItem() instanceof BowItem) return VANILLA_BOW_DRAW_TICKS;
        return 0.0D;
    }

    /**
     * 发射威力倍率 =（原版威力 + 已吸收威力）/ 原版威力。
     *
     * <p>与近战保持同一口径：吸收值是"基础值的增量"，所以第 1 次强化在默认
     * {@code weaponAbsorbedRatio=0.5} 下是 +50% 初速，而不是直接把初速翻倍。
     */
    public static double getRangedPowerMultiplier(ItemStack stack) {
        double base = getRangedBasePower(stack);
        if (base <= 0.0D) return 1.0D;

        double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);
        if (absorbed <= 0.0D) return 1.0D;

        return 1.0D + absorbed / base;
    }

    /**
     * 实际拉满所需 tick：原版拉满时间 - 已吸收的拉弓速度，至少保留 1 tick。
     */
    public static int getEffectiveDrawTicks(ItemStack stack, int vanillaTicks) {
        double absorbed = KogasaEquipmentData.getDrawSpeedValue(stack);
        if (absorbed <= 0.0D) return vanillaTicks;

        long reduced = Math.round(vanillaTicks - absorbed);
        return (int) Math.max(1L, reduced);
    }

    /** 原版三叉戟蓄满所需 tick（ThrownTrident 满力蓄力时间） */
    private static final double VANILLA_TRIDENT_DRAW_TICKS = 10.0D;

    /** 原版三叉戟投掷初速 */
    private static final double VANILLA_TRIDENT_POWER = 2.5D;

    /**
     * 当前实际拉满所需 tick：弓 20 / 弩 25 / 三叉戟 10，
     * 先按总倍率开方缩短，再扣掉强化吸收的拉弓速度。
     * 非弓弩三叉戟返回 0。
     */
    public static int getEffectiveChargeTicks(ItemStack stack) {
        double base = getRangedBaseDrawTicks(stack);
        if (base <= 0.0D && stack.getItem() instanceof TridentItem) {
            base = VANILLA_TRIDENT_DRAW_TICKS;
        }
        if (base <= 0.0D) return 0;

        double multiplier = KogasaEquipmentData.getMultiplier(stack);
        int shortened = (int) Math.round(base / Math.sqrt(multiplier));
        return getEffectiveDrawTicks(stack, Math.max(1, shortened));
    }

    /**
     * 显示用的弹射物速度：基础弹速（弓 3.0 / 弩 3.15 / 三叉戟 2.5）
     * × 总倍率开方 × 强化吸收的发射威力倍率。非弓弩三叉戟返回 0。
     */
    public static double getDisplayProjectileVelocity(ItemStack stack) {
        double base = getRangedBasePower(stack);
        if (base <= 0.0D && stack.getItem() instanceof TridentItem) {
            base = VANILLA_TRIDENT_POWER;
        }
        if (base <= 0.0D) return 0.0D;

        double multiplier = KogasaEquipmentData.getMultiplier(stack);
        return base * Math.sqrt(multiplier) * getRangedPowerMultiplier(stack);
    }

    // ==================== 物品"实质相同"比较（成长不打断攻击/挖掘） ====================
    //
    // 背景：Player.tick 与 MultiPlayerGameMode#sameDestroyTarget 都会比较"主手物品
    // 是否与上一刻相同"（ItemStack.matches / isSameItemSameTags）。成长每次攻击/挖掘
    // 都写一次 KogasaEquipment.Growth，物品 NBT 随之变化，于是攻击冷却被 reset、
    // 挖掘进度被清零。这里提供"忽略本模组高频数据根"的比较，供两个 mixin 调用。

    /**
     * 实质相同比较：把双方物品的 KogasaEquipment / KogasaAbilities 根剥掉后再走
     * 原版 isSameItemSameTags。非本模组物品不做任何剥除（走原版语义）。
     */
    public static boolean areStacksEffectivelySame(ItemStack a, ItemStack b) {
        if (ItemStack.isSameItemSameTags(a, b)) return true;

        if (!isKogasaEquipment(a)
                && !isKogasaEquipment(b)) {
            return false;
        }

        ItemStack aCopy = a.copy();
        ItemStack bCopy = b.copy();
        KogasaEquipmentData.clearAll(aCopy);
        KogasaEquipmentData.clearAll(bCopy);
        KogasaAbilityData.clearAllData(aCopy);
        KogasaAbilityData.clearAllData(bCopy);
        return ItemStack.isSameItemSameTags(aCopy, bCopy);
    }

    /**
     * Player.tick 用 ItemStack.matches 判定"主手是否变化"以决定是否重置攻击冷却。
     * 原版 matches = 数量相等 && isSameItemSameTags。这里保持数量语义，
     * 只把标签比较换成 {@link #areStacksEffectivelySame}。
     */
    public static boolean matchesIgnoringGrowth(ItemStack a, ItemStack b) {
        if (a == b) return true;
        if (a.getCount() != b.getCount()) return false;
        return areStacksEffectivelySame(a, b);
    }

    /**
     * 与 Forge 默认 shouldCauseBlockBreakReset 同语义（耐久不参与比较），
     * 额外把本模组高频数据根剥掉：成长等写入不再导致挖掘进度重置。
     */
    public static boolean shouldCauseBlockBreakResetIgnoringGrowth(ItemStack oldStack, ItemStack newStack) {
        if (!newStack.is(oldStack.getItem())) return true;

        if (!newStack.isDamageableItem() || !oldStack.isDamageableItem()) {
            return !areStacksEffectivelySame(newStack, oldStack);
        }

        // 损伤物品：与原版一致，耐久不计入比较
        ItemStack newCopy = newStack.copy();
        ItemStack oldCopy = oldStack.copy();
        CompoundTag newTag = newCopy.getTag();
        CompoundTag oldTag = oldCopy.getTag();
        if (newTag != null) newTag.remove(ItemStack.TAG_DAMAGE);
        if (oldTag != null) oldTag.remove(ItemStack.TAG_DAMAGE);
        KogasaEquipmentData.clearAll(newCopy);
        KogasaEquipmentData.clearAll(oldCopy);
        KogasaAbilityData.clearAllData(newCopy);
        KogasaAbilityData.clearAllData(oldCopy);
        return !ItemStack.isSameItemSameTags(newCopy, oldCopy);
    }

    /**
     * 获取一件装备的“基础价值”，用于铁砧强化时累加。
     *
     * <p>判定顺序固定为：
     * <ol>
     *   <li>配置的 dynamicStatMaterials：读当前主手攻击伤害属性；</li>
     *   <li>原版剑/工具/护甲/三叉戟：读各自稳定数值；</li>
     *   <li>可穿戴到护甲槽、但不是 {@link ArmorItem} 的普通 Item：读其默认 ARMOR 属性；</li>
     *   <li>其他：0。</li>
     * </ol>
     */
    public static double getEquipmentBaseValue(ItemStack stack) {
        // 1) 动态数值材料：按配置列表，用当前主手攻击伤害属性表作为吸收基准
        if (isDynamicStatMaterial(stack)) {
            double dynamicDamage = getAttributeAttackDamage(stack);
            if (dynamicDamage > 0.0D) {
                warnDynamicMaterial(stack);
                return dynamicDamage;
            }
        }

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
            return getVanillaTridentDamage();
        }

        // 3) 非 ArmorItem、但能进护甲槽的普通 Item：按 ARMOR 属性算护甲价值
        if (getArmorEquipmentSlot(stack) != null) {
            double armor = getAttributeArmorValue(stack);
            if (armor > 0.0D) {
                return armor;
            }
        }
        return 0;
    }

    /** 该物品是否在 dynamicStatMaterials 列表中，强化时读取动态属性表。 */
    public static boolean isDynamicStatMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return KogasaIdListMatcher.matches(id, Config.DYNAMIC_STAT_MATERIALS.get());
    }

    /** 主手攻击伤害的 ADDITION 修饰符之和（调用物品自己的属性表）。 */
    public static double getAttributeAttackDamage(ItemStack stack) {
        double sum = 0.0D;
        for (var entry : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
            if (entry.getKey() == Attributes.ATTACK_DAMAGE
                    && entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                sum += entry.getValue().getAmount();
            }
        }
        return sum;
    }

    private static void warnDynamicMaterial(ItemStack stack) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        if (id != null && WARNED_DYNAMIC_MATERIALS.add(id.toString())) {
            KogasaAddiction.LOGGER.warn(
                    "Dynamic-stat material {} uses its current ATTACK_DAMAGE attribute as reinforcement base; items with NBT/growth-driven stats can produce extremely high values.",
                    id);
        }
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
            baseDamage = emptyHandBonus(stack) + sword.getDamage();
        } else if (item instanceof DiggerItem digger) {
            baseDamage = emptyHandBonus(stack) + digger.getAttackDamage();
        } else if (item instanceof TridentItem) {
            baseDamage = emptyHandBonus(stack) + getVanillaTridentDamage();
        } else if (isKogasaEquipment(stack)) {
            baseDamage = getAttributeAttackDamageBase(stack);
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
     * 物品的基础攻速修饰符，由装备类型表统一给出。
     */
    public static double getBaseAttackSpeed(ItemStack stack) {
        Item item = stack.getItem();
        KogasaEquipmentKind kind = KogasaEquipmentKinds.of(stack);

        // 原版武器/工具仍按类型表；普通 Item 的通用装备从它自己的属性表读基础攻速
        boolean vanillaClass = item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TridentItem
                || item instanceof ArmorItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
        if (!vanillaClass) {
            double attributeSpeed = getAttributeAttackSpeedBase(stack);
            if (attributeSpeed != 0.0D) {
                return attributeSpeed;
            }
        }
        return kind == null ? -2.8D : kind.baseAttackSpeed();
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
     * 综合倍率：强化 × 成长 × AYA 新闻值倍率；
     * 封印装备（战利品）再减半（属性 -50%），解封后恢复。
     */
    public static double getTotalMultiplier(ItemStack stack) {
        double total = getReinforcementMultiplier(stack)
                * getGrowthMultiplier(stack)
                * getNewsMultiplier(stack);

        if (KogasaEquipmentData.isLocked(stack)) {
            total *= 0.5D;
        }
        return total;
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
     * 左侧装备对应的强化材料 tag（由宽容类型判定得出，未识别返回 null）。
     */
    public static TagKey<Item> getMatchingTag(ItemStack stack) {
        KogasaEquipmentKind kind = KogasaEquipmentKinds.of(stack);
        return kind == null ? null : kind.materialTag();
    }

    /**
     * 返回装备类型的后缀标识（不含下划线），例如 "sword"、"pickaxe"；未识别返回 null。
     */
    public static String getEquipmentSuffix(ItemStack stack) {
        KogasaEquipmentKind kind = KogasaEquipmentKinds.of(stack);
        return kind == null ? null : kind.suffix();
    }

    /**
     * 判断 right 是否是 left 的有效强化材料。
     *
     * <p>判定顺序（顺序不要随意调换）：
     * <ol>
     *   <li><b>通用装备</b>：left 已兼容、且不是原版武器/工具/护甲/弓弩基类 →
     *       接受“任意装备类材料”；</li>
     *   <li><b>类型匹配</b>：由 {@link KogasaEquipmentKinds#of(ItemStack)} 得出 left 的类型，
     *       再按该类型的 tag / 后缀 / 类身份规则判断；</li>
     *   <li><b>类家族兜底</b>：left 的 item 类（匿名类先上溯到非匿名父类）作为基类，
     *       right 是它的实例即可。过宽根类（Item/BlockItem/Object）直接拒绝。</li>
     * </ol>
     */
    public static boolean matchesReinforcementMaterial(ItemStack left, ItemStack right) {
        // Curios 装备（法术书/戒指）没有原版装备类层次，跨模组同槽位视为同类材料。
        if (KogasaCuriosHelper.sharesSlot(left, right)) return true;
        // 两边都已标记为兼容装备时，不再要求 Java 类相同。
        if (isKogasaEquipment(left) && isKogasaEquipment(right)) return true;
        // 护甲对护甲：自定义 ArmorItem / 非 ArmorItem 可穿戴物品都可能被原版类型判定漏掉。
        if (getArmorEquipmentSlot(left) != null && isGenericReinforcementMaterial(right)) return true;
        // 1) 通用装备（如灾变直接继承 Item 的武器）：类不是原版基类时，先看通用材料
        if (isKogasaEquipment(left) && !isVanillaEquipmentClass(left.getItem())) {
            if (isGenericReinforcementMaterial(right)) return true;
            return matchesClassFamilyMaterial(left, right);
        }

        // 2) 原版类型匹配
        KogasaEquipmentKind kind = KogasaEquipmentKinds.of(left);
        if (kind != null && kind.acceptsMaterial(right)) {
            return true;
        }

        // 3) 类家族兜底（只对已兼容左件启用，且拒绝过宽根类）
        return isKogasaEquipment(left) && matchesClassFamilyMaterial(left, right);
    }

    /**
     * 类家族兜底：取 left 的物品类，匿名类一路找非匿名父类；right 是该类实例即可。
     *
     * <p>只作为最后兜底：父类若是 {@link Item}/{@link BlockItem}/Object 这种所有物品共享的根类，
     * 会退化成“万物皆材料”，因此直接拒绝。
     */
    public static boolean matchesClassFamilyMaterial(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return false;

        Class<?> clazz = left.getItem().getClass();
        int guard = 0;
        while (clazz != null
                && (clazz.isAnonymousClass() || clazz.isSynthetic())
                && guard++ < 8) {
            clazz = clazz.getSuperclass();
        }

        if (clazz == null || clazz == Object.class
                || clazz == Item.class || clazz == BlockItem.class) {
            return false;
        }
        return clazz.isInstance(right.getItem());
    }

    /** 是否走原版武器/工具/护甲/弓弩基类的安全属性路径。 */
    public static boolean isVanillaEquipmentClass(Item item) {
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TridentItem
                || item instanceof ArmorItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }

    /** 普通 Item 通用装备的强化材料：已识别装备、本模组装备、动态材料、有攻击属性或可穿护甲槽。 */
    public static boolean isGenericReinforcementMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof IKogasaEquipment) return true;
        if (KogasaEquipmentKinds.of(stack) != null) return true;
        if (isDynamicStatMaterial(stack)) return true;
        if (getArmorEquipmentSlot(stack) != null) return true;
        if (KogasaCuriosHelper.isCurio(stack)) return true;
        // 自定义 Item 类常常不走 SwordItem/ArmorItem，也没有默认攻击属性；
        // 有耐久即可视为装备材料（ISS/启示录/灾变等）。
        if (stack.getMaxDamage() > 0) return true;
        return getAttributeAttackDamageBase(stack) > 0.0D
                || getAttributeAttackSpeedBase(stack) > 0.0D;
    }

    /**
     * 是否是“能进护甲槽”的对应槽位装备。
     *
     * <p>只看 HEAD/CHEST/LEGS/FEET；MAINHAND/OFFHAND 几乎任何物品都能拿，不能作为装备判据。
     * 不包含 Curios 槽位（需要 Curios API）。
     */
    @javax.annotation.Nullable
    public static EquipmentSlot getArmorEquipmentSlot(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        EquipmentSlot slot = stack.getEquipmentSlot();
        if (slot == EquipmentSlot.HEAD
                || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS
                || slot == EquipmentSlot.FEET) {
            return slot;
        }
        return null;
    }

    /** 默认属性表中的 ARMOR 值（供非 ArmorItem 的可穿戴物品使用）。 */
    public static double getAttributeArmorValue(ItemStack stack) {
        EquipmentSlot armorSlot = getArmorEquipmentSlot(stack);
        if (armorSlot == null) return 0.0D;

        double sum = 0.0D;
        for (var entry : stack.getItem()
                .getDefaultAttributeModifiers(armorSlot).entries()) {
            if (entry.getKey() == Attributes.ARMOR
                    && entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                sum += entry.getValue().getAmount();
            }
        }
        return sum;
    }

    /** 从物品自己的默认属性表读主手攻击伤害（不触发 ItemAttributeModifierEvent）。 */
    public static double getAttributeAttackDamageBase(ItemStack stack) {
        return sumDefaultAddition(stack, Attributes.ATTACK_DAMAGE);
    }

    /** 从物品自己的默认属性表读主手攻速（不触发 ItemAttributeModifierEvent）。 */
    public static double getAttributeAttackSpeedBase(ItemStack stack) {
        return sumDefaultAddition(stack, Attributes.ATTACK_SPEED);
    }

    private static double sumDefaultAddition(ItemStack stack, Attribute attribute) {
        double sum = 0.0D;
        for (var entry : stack.getItem()
                .getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
            if (entry.getKey() == attribute
                    && entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                sum += entry.getValue().getAmount();
            }
        }
        return sum;
    }

    /**
     * 根据材料类型返回吸收值比例
     */
    public static double getAbsorbedRatio(ItemStack material) {
        if (material.getItem() instanceof ArmorItem || getArmorEquipmentSlot(material) != null) {
            return Config.ARMOR_ABSORBED_RATIO.get();
        }
        return Config.WEAPON_ABSORBED_RATIO.get();
    }

    /**
     * 有效挖掘等级：基础材质与强化次数取高者，映射为原版 {@link Tiers} 常量。
     * 不再使用已弃用的 {@code Tier#getLevel()}——基础等级用原版 Tier 身份比较，
     * 强化等级按 0~4 阶梯映射（下界合金封顶）。
     */
    public static Tier getDynamicHarvestTier(ItemStack stack, Tier baseTier) {
        int re = KogasaEquipmentData.getReinforcements(stack);
        int level = Math.max(indexOfVanillaTier(baseTier), re);

        if (level >= 4) return Tiers.NETHERITE;
        if (level == 3) return Tiers.DIAMOND;
        if (level == 2) return Tiers.IRON;
        if (level == 1) return Tiers.STONE;
        return Tiers.WOOD;
    }

    /** 原版 Tier → 旧数字等级（木/金 0，石 1，铁 2，钻 3，下界合金 4）；非原版材质视为 0。 */
    private static int indexOfVanillaTier(Tier tier) {
        if (tier == Tiers.NETHERITE) return 4;
        if (tier == Tiers.DIAMOND) return 3;
        if (tier == Tiers.IRON) return 2;
        if (tier == Tiers.STONE) return 1;
        return 0; // 木 / 金 / 本模组自定义材质
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
    // ===== 外部（附属模组）装备判定 =====
    // 兼容附属会把"标签 / 标记过的物品"注册进来，让它们和本模组装备走同一套逻辑。
    // 判定异常不能影响主逻辑，所以逐个 try/catch。

    private static final List<Predicate<ItemStack>> EXTERNAL_EQUIPMENT_PREDICATES = new ArrayList<>();

    public static void registerEquipmentPredicate(Predicate<ItemStack> predicate) {
        EXTERNAL_EQUIPMENT_PREDICATES.add(predicate);
    }

    /**
     * 统一的"是否按本模组装备处理"判定。
     *
     * <p>本模组物品恒为 true；外部判定（兼容附属）注册后生效。
     * 这里刻意不读配置：是否启用由注册方（兼容附属）自己判断。
     */
    public static boolean isKogasaEquipment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof IKogasaEquipment) {
            return true;
        }
        for (Predicate<ItemStack> predicate : EXTERNAL_EQUIPMENT_PREDICATES) {
            try {
                if (predicate.test(stack)) {
                    return true;
                }
            } catch (RuntimeException ignored) {
                // 外部判定出错时忽略，不影响主逻辑
            }
        }
        return false;
    }
}
