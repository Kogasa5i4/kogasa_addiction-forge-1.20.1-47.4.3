package net.kogasachan.kogasa_addiction.util;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKind;
import net.kogasachan.kogasa_addiction.item.kind.KogasaEquipmentKinds;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
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

    /**
     * 附属注册的“强制实际数值”判定；优先于动态列表。
     */
    private static final List<Predicate<ItemStack>> FORCE_ACTUAL_STAT_PREDICATES = new ArrayList<>();
    /**
     * 附属注册的变体身份解析（例如拔刀剑 bladeState）；用于材料去重与实际物品匹配。
     */
    private static final List<Function<ItemStack, String>> STAT_VARIANT_RESOLVERS = new ArrayList<>();
    /**
     * 附属注册的 JEI 材料展开器（例如一条 slashblade 注册名展开成多把带 NBT 的刀）。
     */
    private static final List<Function<ItemStack, List<ItemStack>>> MATERIAL_STACK_EXPANDERS = new ArrayList<>();
    /**
     * 附属注册的额外强化材料匹配（例如 TaCZ 枪械对枪械）。
     */
    private static final List<java.util.function.BiPredicate<ItemStack, ItemStack>> REINFORCEMENT_MATCHERS =
            new ArrayList<>();
    /**
     * 附属注册的吸收后回调（result 已复制左侧装备，right 是材料）。
     */
    private static final List<java.util.function.BiConsumer<ItemStack, ItemStack>> ABSORPTION_LISTENERS =
            new ArrayList<>();
    private static final List<java.util.function.BiConsumer<ItemStack, net.minecraft.world.entity.player.Player>> ABSORPTION_COMPLETE_LISTENERS =
            new ArrayList<>();

    /**
     * 原版三叉戟基准攻击伤害的惰性缓存（首次属性计算时读取注册表，之后复用）。
     */
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
        appendAbsorbedAttributes(absorbedOnly, stack, EquipmentSlot.MAINHAND, java.util.Set.of(), java.util.Set.of());
        return absorbedOnly.build();
    }

    /**
     * 武器/工具的攻击力与攻速
     */
    private static Multimap<Attribute, AttributeModifier> buildHandModifiers(
            ItemStack stack, double baseDamage, double absorbed, double baseSpeed) {
        double multiplier = KogasaEquipmentData.getMultiplier(stack);
        // baseDamage 是给玩家看的“实际总伤害”（含空手 +1）；
        // ATTACK_DAMAGE 修饰符只需要物品自身贡献，玩家基础 1 点由属性系统自带，
        // 否则实际伤害会比 tooltip 再高 1 点（飞刀尤其明显）。
        double itemDamage = baseDamage - emptyHandBonus(stack);

        double totalDamage;
        if (Config.ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER.get()) {
            totalDamage = (itemDamage + absorbed) * multiplier;
        } else {
            totalDamage = itemDamage * multiplier + absorbed;
        }

        double totalSpeed = getAttackSpeedModifier(stack, baseSpeed);

        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        builder.put(Attributes.ATTACK_DAMAGE, new AttributeModifier(
                WEAPON_DAMAGE_UUID, "Kogasa modifier",
                totalDamage, AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_SPEED, new AttributeModifier(
                WEAPON_SPEED_UUID, "Kogasa modifier",
                totalSpeed, AttributeModifier.Operation.ADDITION));
        appendAbsorbedAttributes(builder, stack, EquipmentSlot.MAINHAND,
                java.util.Set.of(
                        ResourceLocation.withDefaultNamespace("generic.attack_damage"),
                        ResourceLocation.withDefaultNamespace("generic.attack_speed")),
                java.util.Set.of(
                        ResourceLocation.withDefaultNamespace("generic.attack_damage")));
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
        double totalKnockback = armor.getMaterial().getKnockbackResistance() * multiplier;

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

        appendAbsorbedAttributes(builder, stack, slot,
                java.util.Set.of(
                        ResourceLocation.withDefaultNamespace("generic.armor"),
                        ResourceLocation.withDefaultNamespace("generic.armor_toughness"),
                        ResourceLocation.withDefaultNamespace("generic.knockback_resistance")),
                java.util.Set.of(
                        ResourceLocation.withDefaultNamespace("generic.armor")));
        return builder.build();
    }

    /**
     * 把 AbsorbedAttributes 中除攻击/护甲外的属性追加到 modifier 表。
     */
    @SuppressWarnings("deprecation")
    private static void appendAbsorbedAttributes(
            ImmutableMultimap.Builder<Attribute, AttributeModifier> builder,
            ItemStack stack, EquipmentSlot slot,
            java.util.Set<ResourceLocation> baseLegacy,
            java.util.Set<ResourceLocation> absorbedLegacy) {

        // 物品本来就有的额外属性也参与成长；攻击/护甲/攻速仍由上面的专用公式处理。
        Map<ResourceLocation, Double> baseAdditive = new java.util.LinkedHashMap<>();
        for (var entry : stack.getItem().getDefaultAttributeModifiers(slot).entries()) {
            if (entry.getValue().getOperation() != AttributeModifier.Operation.ADDITION) continue;
            ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(entry.getKey());
            if (id == null || baseLegacy.contains(id)) continue;
            baseAdditive.merge(id, entry.getValue().getAmount(), Double::sum);
        }

        Map<ResourceLocation, Double> absorbedAdditive = KogasaEquipmentData.getAbsorbedAttributes(stack);
        java.util.List<KogasaEquipmentData.AbsorbedMultiplier> absorbedMultipliers =
                KogasaEquipmentData.getAbsorbedMultipliers(stack);

        java.util.Set<ResourceLocation> all = new java.util.LinkedHashSet<>(baseAdditive.keySet());
        all.addAll(absorbedAdditive.keySet());
        for (var entry : absorbedMultipliers) all.add(entry.attribute());

        for (ResourceLocation id : all) {
            if (absorbedLegacy.contains(id)) continue;

            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
            if (attribute == null) continue;

            double base = baseAdditive.getOrDefault(id, 0.0D);
            double additive = absorbedAdditive.getOrDefault(id, 0.0D);
            double multiplier = KogasaAttributeGrowth.multiplier(stack, id);

            // 加法吸收并入基础值，最后统一乘一次综合倍率。
            double value = KogasaAttributeGrowth.applyCap(id, (base + additive) * multiplier);
            if (value <= 0.0D) continue;

            UUID uuid = UUID.nameUUIDFromBytes(("kogasa_absorbed:" + id).getBytes(StandardCharsets.UTF_8));
            builder.put(attribute, new AttributeModifier(
                    uuid, "Kogasa growth " + id, value,
                    AttributeModifier.Operation.ADDITION));
        }

        // 乘法吸收保留来源装备的 UUID 与 operation。
        for (KogasaEquipmentData.AbsorbedMultiplier entry : absorbedMultipliers) {
            if (absorbedLegacy.contains(entry.attribute())) continue;
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(entry.attribute());
            if (attribute == null || entry.amount() <= 0.0D) continue;
            builder.put(attribute, new AttributeModifier(
                    entry.uuid(), "Kogasa absorbed multiplier " + entry.attribute(),
                    entry.amount(), entry.operation()));
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

    private static final List<Predicate<ItemStack>> RANGED_PREDICATES = new ArrayList<>();
    private static final List<Function<ItemStack, Double>> RANGED_POWER_PROVIDERS = new ArrayList<>();
    private static final List<Function<ItemStack, Double>> RANGED_DRAW_PROVIDERS = new ArrayList<>();

    /**
     * 附属注册“按远程处理”的装备（例如 TaCZ 枪械）；不会改变物品类型。
     */
    public static void registerRangedPredicate(Predicate<ItemStack> predicate) {
        if (predicate != null) RANGED_PREDICATES.add(predicate);
    }

    public static void registerRangedPowerProvider(Function<ItemStack, Double> provider) {
        if (provider != null) RANGED_POWER_PROVIDERS.add(provider);
    }

    public static void registerRangedDrawProvider(Function<ItemStack, Double> provider) {
        if (provider != null) RANGED_DRAW_PROVIDERS.add(provider);
    }

    public static boolean isRanged(ItemStack stack) {
        if (stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem) return true;
        for (Predicate<ItemStack> predicate : RANGED_PREDICATES) {
            try {
                if (predicate.test(stack)) return true;
            } catch (RuntimeException ignored) {
            }
        }
        return false;
    }

    /**
     * 弓弩的"基础发射威力"。
     *
     * <p>强化材料侧的口径：**任何箭矢都视作原版弓弩的等价复制品**，属性完全一致，
     * 因此这里只看左侧这件装备自己是弓还是弩，与材料是什么无关。
     */
    public static double getRangedBasePower(ItemStack rangedStack) {
        for (Function<ItemStack, Double> provider : RANGED_POWER_PROVIDERS) {
            try {
                double value = provider.apply(rangedStack);
                if (value > 0.0D) return value;
            } catch (RuntimeException ignored) {
            }
        }
        if (rangedStack.getItem() instanceof CrossbowItem) return VANILLA_CROSSBOW_POWER;
        if (rangedStack.getItem() instanceof BowItem) return VANILLA_BOW_POWER;
        return 0.0D;
    }

    /**
     * 弓弩的"基础拉弓时间"（tick），口径同上。
     */
    public static double getRangedBaseDrawTicks(ItemStack rangedStack) {
        for (Function<ItemStack, Double> provider : RANGED_DRAW_PROVIDERS) {
            try {
                double value = provider.apply(rangedStack);
                if (value > 0.0D) return value;
            } catch (RuntimeException ignored) {
            }
        }
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

    /**
     * 原版三叉戟蓄满所需 tick（ThrownTrident 满力蓄力时间）
     */
    private static final double VANILLA_TRIDENT_DRAW_TICKS = 10.0D;

    /**
     * 原版三叉戟投掷初速
     */
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
     * ISS/Cataclysm 等外部装备：按包名识别，供吸收与强化材料兼容使用。
     */
    public static boolean isExternalWeapon(ItemStack stack) {
        if (stack.isEmpty() || isExternalArmor(stack)) return false;
        Item item = stack.getItem();
        if (!isExternalEquipment(item)) return false;
        if (item instanceof BowItem || item instanceof CrossbowItem) return false;
        if (item instanceof SwordItem || item instanceof DiggerItem || item instanceof TridentItem) return true;
        if (hasSuperclass(item, "Cataclysm_Weapon_Item")
                || hasSuperclass(item, "StaffItem")
                || hasSuperclass(item, "ExtendedSwordItem")) return true;
        return getAttributeAttackDamageBase(stack) > 0.0D;
    }

    public static boolean isExternalArmor(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return isExternalEquipment(item)
                && (item instanceof ArmorItem || getArmorEquipmentSlot(stack) != null);
    }

    private static boolean isExternalEquipment(Item item) {
        String name = item.getClass().getName();
        return name.startsWith("io.redspace.ironsspellbooks.api.item.weapons.")
                || name.startsWith("io.redspace.ironsspellbooks.item.weapons.")
                || name.startsWith("io.redspace.ironsspellbooks.item.armor.")
                || name.startsWith("com.github.L_Ender.cataclysm.items.")
                || name.startsWith("net.unusual.block_factorys_bosses.item.");
    }

    private static boolean hasSuperclass(Item item, String simpleName) {
        for (Class<?> type = item.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getSimpleName().equals(simpleName)) return true;
        }
        return false;
    }

    /**
     * 获取一件装备用于吸收的“基础价值”。
     *
     * <p>攻击力规则（A+A）：
     * <ul>
     *   <li>护甲仍按原有护甲值逻辑；</li>
     *   <li>dynamicStatClasses / dynamicStatItems 命中：使用稳定的 legacy 值，不读实时 NBT；</li>
     *   <li>forceActualStatClasses / forceActualStatItems 命中：强制使用默认 ATTACK_DAMAGE；</li>
     *   <li>非动态装备：actual 与 legacy 不一致时以 actual 为准（通常说明类重写了属性表），
     *       相同时取两者最大值兜底。</li>
     * </ul>
     */
    public static double getEquipmentBaseValue(ItemStack stack) {
        Item item = stack.getItem();

        // 护甲优先：外部自定义护甲通常不是标准 ArmorItem。
        if (isExternalArmor(stack)) {
            double armor = getAttributeArmorValue(stack);
            if (armor > 0.0D) return armor;
        }
        if (item instanceof ArmorItem armor) {
            return armor.getDefense();
        }
        if (getArmorEquipmentSlot(stack) != null) {
            double armor = getAttributeArmorValue(stack);
            if (armor > 0.0D) return armor;
        }

        double legacy = getLegacyAttackDamage(stack);

        if (isForceActualStatEquipment(stack)) {
            // 强制实际值必须读带 ItemStack 的完整属性表（拔刀剑的 bladeState 在属性事件里），
            // 不能只读物品默认属性。
            double live = getAttributeAttackDamage(stack);
            // 兜底：某些附属刀/默认刀没有把 bladeState 正确接进属性事件，
            // 此时用 bladeState.baseAttackModifier - 1（SlashBlade 的 item 贡献）。
            CompoundTag root = stack.getTag();
            if (root != null && root.contains("bladeState", Tag.TAG_COMPOUND)) {
                double stateBase = root.getCompound("bladeState").getFloat("baseAttackModifier");
                if (stateBase > 1.0D) live = Math.max(live, stateBase - 1.0D);
            }
            if (live > 0.0D) return live;
        }

        double actual = getDefaultAttackDamage(stack);
        if (isDynamicStatEquipment(stack)) return legacy;
        if (actual > 0.0D && actual != legacy) return actual;
        return Math.max(actual, legacy);
    }

    /**
     * 旧版稳定攻击力读取：SwordItem/DiggerItem/原版三叉戟字段。
     */
    private static double getLegacyAttackDamage(ItemStack stack) {
        Item item = stack.getItem();
        if (item instanceof SwordItem sword) return sword.getDamage();
        if (item instanceof DiggerItem digger) return digger.getAttackDamage();
        if (item instanceof TridentItem) return getVanillaTridentDamage();
        return 0.0D;
    }

    /**
     * 物品自己的默认主手 ATTACK_DAMAGE（不触发 Forge 事件、不含附魔）。
     */
    @SuppressWarnings("deprecation")
    public static double getDefaultAttackDamage(ItemStack stack) {
        double sum = 0.0D;
        if (stack.isEmpty()) return sum;
        for (var entry : stack.getItem()
                .getDefaultAttributeModifiers(EquipmentSlot.MAINHAND).entries()) {
            if (entry.getKey() == Attributes.ATTACK_DAMAGE
                    && entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                sum += entry.getValue().getAmount();
            }
        }
        return sum;
    }

    /**
     * 该类/物品是否被视为动态属性装备（使用稳定 legacy 基准，不读实时属性）。
     */
    public static boolean isDynamicStatEquipment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Item item = stack.getItem();
        // 附属模组通过 registerEquipmentPredicate 标记的兼容装备也自动按动态处理，
        // 不需要再手动写进 dynamicStatClasses / dynamicStatItems。
        if (isKogasaEquipment(stack)) return true;

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (KogasaIdListMatcher.matches(id, Config.DYNAMIC_STAT_ITEMS.get())) return true;
        return KogasaIdListMatcher.matchesString(item.getClass().getName(), Config.DYNAMIC_STAT_CLASSES.get());
    }

    /**
     * @deprecated 使用 {@link #isDynamicStatEquipment(ItemStack)}。
     */
    @Deprecated
    public static boolean isDynamicStatMaterial(ItemStack stack) {
        return isDynamicStatEquipment(stack);
    }

    /**
     * 该类/物品是否强制使用实际 ATTACK_DAMAGE（优先级高于动态列表）。
     */
    public static boolean isForceActualStatEquipment(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        for (Predicate<ItemStack> predicate : FORCE_ACTUAL_STAT_PREDICATES) {
            try {
                if (predicate.test(stack)) return true;
            } catch (RuntimeException ignored) {
                // 可选模组未加载/物品状态异常时不影响主逻辑
            }
        }
        // SlashBlade / 拔刀剑变体：bladeState 里的攻击力是 stack 级数据，
        // 不能走 legacy 字段。即使附属尚未注册 predicate，也按实际属性读取。
        CompoundTag root = stack.getTag();
        if (root != null && root.contains("bladeState", Tag.TAG_COMPOUND)) return true;

        Item item = stack.getItem();
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        if (KogasaIdListMatcher.matches(id, Config.FORCE_ACTUAL_STAT_ITEMS.get())) return true;
        return KogasaIdListMatcher.matchesString(item.getClass().getName(), Config.FORCE_ACTUAL_STAT_CLASSES.get());
    }

    /**
     * 附属注册“该物品强制按实际数值吸收”的判定。
     */
    public static void registerForceActualStatPredicate(Predicate<ItemStack> predicate) {
        if (predicate != null) FORCE_ACTUAL_STAT_PREDICATES.add(predicate);
    }

    /**
     * 附属注册变体身份解析；返回 null 表示该物品没有变体概念。
     */
    public static void registerStatVariantResolver(Function<ItemStack, String> resolver) {
        if (resolver != null) STAT_VARIANT_RESOLVERS.add(resolver);
    }

    private static final List<java.util.function.BiConsumer<ItemStack, List<String>>> SPECIAL_EFFECT_APPLIERS =
            new ArrayList<>();

    private static final List<java.util.function.BiConsumer<ItemStack, CompoundTag>> BLADE_STATE_APPLIERS =
            new ArrayList<>();

    /**
     * 附属注册完整的 bladeState → capability 同步（拔刀剑）。
     */
    public static void registerBladeStateApplier(
            java.util.function.BiConsumer<ItemStack, CompoundTag> applier) {
        if (applier != null) BLADE_STATE_APPLIERS.add(applier);
    }

    public static void applyBladeState(ItemStack stack, CompoundTag bladeState) {
        for (var applier : BLADE_STATE_APPLIERS) {
            try {
                applier.accept(stack, bladeState);
            } catch (RuntimeException ignored) {
                // 可选模组状态异常不影响 NBT
            }
        }
    }

    /**
     * 附属注册“SE 写入 ItemStack 后同步到能力状态”的回调（拔刀剑需要）。
     */
    public static void registerSpecialEffectApplier(
            java.util.function.BiConsumer<ItemStack, List<String>> applier) {
        if (applier != null) SPECIAL_EFFECT_APPLIERS.add(applier);
    }

    public static void applySpecialEffects(ItemStack stack, List<String> effects) {
        for (var applier : SPECIAL_EFFECT_APPLIERS) {
            try {
                applier.accept(stack, effects);
            } catch (RuntimeException ignored) {
                // 可选模组状态异常不影响 NBT
            }
        }
    }

    /**
     * 附属注册 JEI 材料展开器；返回空列表时使用原始 ItemStack。
     */
    public static void registerReinforcementMatcher(
            java.util.function.BiPredicate<ItemStack, ItemStack> matcher) {
        if (matcher != null) REINFORCEMENT_MATCHERS.add(matcher);
    }

    public static void registerAbsorptionListener(
            java.util.function.BiConsumer<ItemStack, ItemStack> listener) {
        if (listener != null) ABSORPTION_LISTENERS.add(listener);
    }

    /**
     * 吸收完成后的 result 回调（此时 result NBT 已写完，可用于刷新外部模组缓存）。
     */
    public static void registerAbsorptionCompleteListener(
            java.util.function.BiConsumer<ItemStack, net.minecraft.world.entity.player.Player> listener) {
        if (listener != null) ABSORPTION_COMPLETE_LISTENERS.add(listener);
    }

    public static void notifyAbsorptionComplete(ItemStack result,
                                                net.minecraft.world.entity.player.Player player) {
        for (var listener : ABSORPTION_COMPLETE_LISTENERS) {
            try {
                listener.accept(result, player);
            } catch (RuntimeException ignored) {
            }
        }
    }

    public static void notifyAbsorption(ItemStack result, ItemStack right) {
        for (var listener : ABSORPTION_LISTENERS) {
            try {
                listener.accept(result, right);
            } catch (RuntimeException ignored) {
                // 可选模组吸收异常不影响主流程
            }
        }
    }

    public static void registerMaterialStackExpander(Function<ItemStack, List<ItemStack>> expander) {
        if (expander != null) MATERIAL_STACK_EXPANDERS.add(expander);
    }

    /**
     * 物品的稳定变体身份（例如拔刀剑的 bladeState 键）；无变体返回 null。
     */
    public static String getStatVariantKey(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (Function<ItemStack, String> resolver : STAT_VARIANT_RESOLVERS) {
            try {
                String key = resolver.apply(stack);
                if (key != null && !key.isBlank()) return key;
            } catch (RuntimeException ignored) {
                // 忽略可选模组异常
            }
        }
        return null;
    }

    /**
     * 物品 ID + 变体身份，作为“已使用材料”的去重键。
     */
    public static String getMaterialId(ItemStack stack, String baseId) {
        String variant = getStatVariantKey(stack);
        return variant == null ? baseId : baseId + "#" + variant;
    }

    /**
     * 把单个注册物品展开成多个带 NBT 的 JEI 材料；空列表表示不展开。
     */
    public static List<ItemStack> expandMaterialStacks(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return List.of();
        java.util.LinkedHashMap<String, ItemStack> result = new java.util.LinkedHashMap<>();
        for (Function<ItemStack, List<ItemStack>> expander : MATERIAL_STACK_EXPANDERS) {
            try {
                List<ItemStack> expanded = expander.apply(stack);
                if (expanded == null) continue;
                for (ItemStack variant : expanded) {
                    if (variant == null || variant.isEmpty()) continue;
                    ItemStack copy = variant.copy();
                    result.putIfAbsent(copy.getItem() + "|" + copy.getTag(), copy);
                }
            } catch (RuntimeException ignored) {
                // 忽略可选模组异常
            }
        }
        return new ArrayList<>(result.values());
    }

    /**
     * 该材料的基础攻击力是否来自武器攻击属性；是的话吸收时要排除 attack_damage，
     * 防止“基础值 + 属性”重复计算。
     */
    public static boolean usesAttackDamageBase(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        if (stack.getItem() instanceof ArmorItem || getArmorEquipmentSlot(stack) != null) return false;
        return getDefaultAttackDamage(stack) > 0.0D || getLegacyAttackDamage(stack) > 0.0D;
    }

    /**
     * 主手攻击伤害的 ADDITION 修饰符之和（调用物品自己的属性表）。
     */
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
        double ratio = Config.ATTACK_SPEED_GROWTH_RATIO.get();
        // 文文新闻值是攻速进度乘数：加快接近上限，但总攻速仍封顶 4。
        double progress = Math.min(growth * ratio * getNewsMultiplier(stack), 1.0D);
        if (progress <= 0.0D) return baseSpeed;

        // 总攻速 = 默认 4 + modifier；成长值为基础，新闻值作为乘数加快逼近上限。
        double defaultSpeed = Attributes.ATTACK_SPEED.getDefaultValue();
        double targetModifier = Config.ATTACK_SPEED_CAP.get() - defaultSpeed;
        return baseSpeed + (targetModifier - baseSpeed) * progress;
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
        return net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED.getDefaultValue()
                + getAttackSpeedModifier(stack, baseSpeed);
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
        // 附属自定义材料对（例如 TaCZ 枪械对枪械）。
        for (var matcher : REINFORCEMENT_MATCHERS) {
            try {
                if (matcher.test(left, right)) return true;
            } catch (RuntimeException ignored) {
            }
        }
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
        if (kind != null) {
            // ISS / Cataclysm 的武器类名不一定是 SwordItem，护甲也可能走自定义基类。
            if (isExternalWeapon(right) && isMeleeKind(kind)) return true;
            if (isExternalArmor(right) && isArmorKind(kind)) return true;
            if (kind.acceptsMaterial(right)) return true;
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

    private static boolean isMeleeKind(KogasaEquipmentKind kind) {
        return kind == KogasaEquipmentKind.SWORD
                || kind == KogasaEquipmentKind.AXE
                || kind == KogasaEquipmentKind.KNIFE;
    }

    private static boolean isArmorKind(KogasaEquipmentKind kind) {
        return kind == KogasaEquipmentKind.HELMET
                || kind == KogasaEquipmentKind.CHESTPLATE
                || kind == KogasaEquipmentKind.LEGGINGS
                || kind == KogasaEquipmentKind.BOOTS;
    }

    /**
     * 是否走原版武器/工具/护甲/弓弩基类的安全属性路径。
     */
    public static boolean isVanillaEquipmentClass(Item item) {
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TridentItem
                || item instanceof ArmorItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }

    /**
     * 普通 Item 通用装备的强化材料：已识别装备、本模组装备、动态材料、有攻击属性或可穿护甲槽。
     */
    public static boolean isGenericReinforcementMaterial(ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof IKogasaEquipment) return true;
        if (KogasaEquipmentKinds.of(stack) != null) return true;
        if (isDynamicStatEquipment(stack)) return true;
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

    /**
     * 默认属性表中的 ARMOR 值（供非 ArmorItem 的可穿戴物品使用）。
     */
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

    /**
     * 从物品自己的默认属性表读主手攻击伤害（不触发 ItemAttributeModifierEvent）。
     */
    public static double getAttributeAttackDamageBase(ItemStack stack) {
        return sumDefaultAddition(stack, Attributes.ATTACK_DAMAGE);
    }

    /**
     * 从物品自己的默认属性表读主手攻速（不触发 ItemAttributeModifierEvent）。
     */
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

    /**
     * 原版 Tier → 旧数字等级（木/金 0，石 1，铁 2，钻 3，下界合金 4）；非原版材质视为 0。
     */
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
