package net.kogasachan.kogasa_addiction_compat.compat;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaAttributeGrowth;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 兼容层的数值入口：把成长/强化/封印带来的属性应用到非本模组物品上。
 *
 * <p>实现方式：Forge 的 {@link ItemAttributeModifierEvent}（{@code ItemStack#getAttributeModifiers} 内部触发）。
 * 只对"安全类"物品接管：{@link SwordItem} / {@link DiggerItem} / {@link TridentItem} /
 * {@link ArmorItem} / {@link BowItem} / {@link CrossbowItem}——这些类走原版属性结构，
 * 用 {@code clearModifiers()} + 主模组同一套 {@link KogasaEquipmentHelper} 计算，
 * 数值与 tooltip 都和本模组物品保持一致。
 *
 * <p>其他模组物品（自定义属性管线）暂不接管，避免清掉它们的修饰符；等后续做"叠加增量"模式。
 */
@Mod.EventBusSubscriber(modid = KogasaCompatAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompatAttributeHandler {

    /**
     * 防递归：主模组在计算属性时可能读取“原版示例物品”的属性，
     * 如果那次读取又触发本事件，会导致无限递归。计算期间直接跳过。
     */
    private static final ThreadLocal<Boolean> COMPUTING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private static final java.util.UUID GENERIC_DAMAGE_UUID =
            java.util.UUID.fromString("6f9d5f41-2c9a-4d5e-9a8b-0d3a6e7c1f20");
    private static final java.util.UUID GENERIC_SPEED_UUID =
            java.util.UUID.fromString("0b7c3e22-8a4f-4c1d-b6e9-5a2d9f7c3b11");

    private CompatAttributeHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        if (COMPUTING.get()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!KogasaCompat.isCompatOnly(stack)) {
            return;
        }
        boolean safe = isSafeItem(stack.getItem());
        if (!safe && !CompatGenericEquipment.isGeneric(stack, event.getOriginalModifiers())) {
            return;
        }

        COMPUTING.set(Boolean.TRUE);
        try {
            EquipmentSlot slot = event.getSlotType();
            if (safe) {
                if (stack.getItem() instanceof ArmorItem) {
                    // 兼容护甲不能 clear/replace：灾变自己的 ItemAttributeModifierEvent
                    // 会再替换一次护甲属性，两边叠加就是“两套数值”。
                    // 这里只叠加成长/强化带来的增量。
                    applyArmorOverlay(event, stack);
                } else if (!isPlainVanillaItem(stack.getItem()) || hasNonVanillaAttributes(event)) {
                    // ISS/拔刀剑等虽然继承 SwordItem，但自带数据驱动属性或自定义属性；
                    // 整体替换会把原生属性清掉，改走叠加式。
                    applyGenericOverlay(event, stack);
                } else {
                    Multimap<Attribute, AttributeModifier> modifiers =
                            KogasaEquipmentHelper.getAttributeModifiers(stack.getItem(), slot, stack);

                    // 原版属性结构：用主模组公式整体替换，保证与本模组装备完全一致
                    event.clearModifiers();
                    modifiers.forEach(event::addModifier);
                }
            } else {
                applyGenericOverlay(event, stack);
            }
        } finally {
            COMPUTING.set(Boolean.FALSE);
        }
    }

    /**
     * 普通 Item 的通用武器：保留它自己的属性表（灾变配置数值），只叠加成长/强化带来的增量。
     *
     * <p>注意：这里刻意不清空 original modifiers；否则灾变这类把数值写在属性表里的武器
     * 会被主模组公式覆盖成 0。只有原版基类（safe）才走整体替换分支。
     */
    private static void applyGenericOverlay(ItemAttributeModifierEvent event, ItemStack stack) {
        if (event.getSlotType() != EquipmentSlot.MAINHAND) {
            return;
        }

        Multimap<Attribute, AttributeModifier> original = event.getOriginalModifiers();
        double baseDamage = sumAddition(original, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        double baseSpeed = sumAddition(original, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
        if (baseDamage == 0.0D && baseSpeed == 0.0D) {
            return;
        }

        double absorbed = net.kogasachan.kogasa_addiction.util.KogasaEquipmentData.getAbsorbedValue(stack);
        double multiplier = net.kogasachan.kogasa_addiction.util.KogasaEquipmentData.getMultiplier(stack);
        double targetDamage = net.kogasachan.kogasa_addiction.Config.ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER.get()
                ? (baseDamage + absorbed) * multiplier
                : baseDamage * multiplier + absorbed;
        double damageDelta = targetDamage - baseDamage;
        if (Math.abs(damageDelta) > 1.0E-6) {
            // 不新增第二条 attack_damage modifier；把原生的第一条 ADDITION 改成目标值，
            // 否则铁砧输出会出现两条“攻击伤害”行。
            adjustFirstAddition(event, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    damageDelta, GENERIC_DAMAGE_UUID, "Kogasa compat damage");
        }

        double targetSpeed = KogasaEquipmentHelper.getAttackSpeedModifier(stack, baseSpeed);
        double speedDelta = targetSpeed - baseSpeed;
        if (Math.abs(speedDelta) > 1.0E-6) {
            adjustFirstAddition(event, net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    speedDelta, GENERIC_SPEED_UUID, "Kogasa compat speed");
        }

        applyAbsorbedExtras(event, stack, java.util.Set.of(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.attack_damage")));
        applyAbsorbedMultipliers(event, stack, java.util.Set.of(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.attack_damage")));
    }

    /** 调整原生第一条 ADDITION，而不是新增一条同属性 modifier。 */
    private static void adjustFirstAddition(ItemAttributeModifierEvent event, Attribute attribute,
                                             double delta, java.util.UUID fallbackId, String fallbackName) {
        for (AttributeModifier modifier : new java.util.ArrayList<>(event.getModifiers().get(attribute))) {
            if (modifier.getOperation() != AttributeModifier.Operation.ADDITION) continue;
            event.removeModifier(attribute, modifier);
            event.addModifier(attribute, new AttributeModifier(
                    modifier.getId(), modifier.getName(),
                    modifier.getAmount() + delta,
                    AttributeModifier.Operation.ADDITION));
            return;
        }
        event.addModifier(attribute, new AttributeModifier(
                fallbackId, fallbackName, delta, AttributeModifier.Operation.ADDITION));
    }

    /** 物品原有属性 + 吸收属性统一成长；同一属性合并成单条，避免同名双行。 */
    private static void applyAbsorbedExtras(ItemAttributeModifierEvent event, ItemStack stack, java.util.Set<net.minecraft.resources.ResourceLocation> legacy) {
        Map<net.minecraft.resources.ResourceLocation, Double> additive =
                KogasaEquipmentData.getAbsorbedAttributes(stack);

        java.util.Set<net.minecraft.resources.ResourceLocation> ids = new java.util.LinkedHashSet<>();
        for (Attribute attribute : event.getModifiers().keySet()) {
            net.minecraft.resources.ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (id != null) ids.add(id);
        }
        ids.addAll(additive.keySet());



        for (net.minecraft.resources.ResourceLocation id : ids) {
            if (legacy.contains(id)) continue;
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(id);
            if (attribute == null) continue;

            double base = sumAddition(event.getModifiers(), attribute);
            double add = additive.getOrDefault(id, 0.0D);
            double growth = KogasaAttributeGrowth.multiplier(stack, id);
            double value = KogasaAttributeGrowth.applyCap(id, (base + add) * growth);
            if (value <= 0.0D) continue;

            event.removeAttribute(attribute);
            event.addModifier(attribute, new AttributeModifier(
                    java.util.UUID.nameUUIDFromBytes(("kogasa_absorbed:" + id).getBytes(StandardCharsets.UTF_8)),
                    "Kogasa growth " + id, value, AttributeModifier.Operation.ADDITION));
        }
    }

    /**
     * 兼容护甲：保留 Cataclysm 等模组自己的护甲属性，只追加成长/强化增量。
     * 不 clearModifiers，避免与对方自己的 ItemAttributeModifierEvent 叠加成两套。
     */
    private static void applyArmorOverlay(ItemAttributeModifierEvent event, ItemStack stack) {
        EquipmentSlot slot = event.getSlotType();
        if (slot != EquipmentSlot.HEAD
                && slot != EquipmentSlot.CHEST
                && slot != EquipmentSlot.LEGS
                && slot != EquipmentSlot.FEET) {
            return;
        }

        // 只用物品原始属性作为兼容 overlay 的基准：不能用 event.getModifiers()，
        // 否则其它 handler 临时塞进来的数值会被当成“原装备基础值”再复制一遍。
        Multimap<Attribute, AttributeModifier> current = event.getOriginalModifiers();
        double multiplier = net.kogasachan.kogasa_addiction.util.KogasaEquipmentData.getMultiplier(stack);
        double absorbed = net.kogasachan.kogasa_addiction.util.KogasaEquipmentData.getAbsorbedValue(stack);

        double baseArmor = sumAddition(current, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        double baseToughness = sumAddition(current, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        double baseKnockback = sumAddition(current, net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);

        // 护甲槽不匹配时原属性为空：这时加“吸收值”会把 +442 之类错误地塞进
        // 脚/腿/头三个空槽，tooltip 就会出现多套错误数值。只处理真正有护甲值的槽。
        if (baseArmor <= 0.0D) {
            return;
        }

        double targetArmor = net.kogasachan.kogasa_addiction.Config.ABSORBED_VALUE_INCLUDED_IN_MULTIPLIER.get()
                ? (baseArmor + absorbed) * multiplier
                : baseArmor * multiplier + absorbed;
        double targetToughness = baseToughness * multiplier;
        double targetKnockback = baseKnockback * multiplier;

        // LOWEST 优先级：在灾变自己的 ItemAttributeModifier 之后执行。
        // 先移除同一属性上的基础护甲修饰符，再加一个“总数”修饰符；
        // 否则基础值 + 增量会各占一行，tooltip 看起来像两套数值。
        event.removeAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR);
        event.removeAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS);
        event.removeAttribute(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE);

        addTotal(event, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR,
                targetArmor, "Kogasa compat armor");
        addTotal(event, net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                targetToughness, "Kogasa compat toughness");
        addTotal(event, net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                targetKnockback, "Kogasa compat knockback");

        applyAbsorbedExtras(event, stack, java.util.Set.of(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.armor"),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.armor_toughness"),
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.knockback_resistance")));

        // 护甲韧性：基础韧性已走上面的 total，这里单独追加吸收部分。
        var toughnessId = net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.armor_toughness");
        double absorbedToughness = KogasaEquipmentData.getAbsorbedAttributes(stack)
                .getOrDefault(toughnessId, 0.0D);
        double toughnessMultiplier = KogasaEquipmentData.getAbsorbedAttributeMultipliers(stack)
                .getOrDefault(toughnessId, 0.0D);
        double toughnessGrowth = KogasaAttributeGrowth.multiplier(stack, toughnessId);
        double toughnessValue = KogasaAttributeGrowth.applyCap(toughnessId,
                absorbedToughness * toughnessGrowth * (1.0D + toughnessMultiplier));
        if (toughnessValue > 0.0D) {
            event.addModifier(net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS,
                    new AttributeModifier(
                            java.util.UUID.nameUUIDFromBytes(("kogasa_absorbed:" + toughnessId).getBytes(StandardCharsets.UTF_8)),
                            "Kogasa absorbed toughness", toughnessValue,
                            AttributeModifier.Operation.ADDITION));
        }

        // 击退抗性：基础值已走上面的 total，这里单独追加吸收部分。
        var knockbackId = net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.knockback_resistance");
        double absorbedKnockback = KogasaEquipmentData.getAbsorbedAttributes(stack)
                .getOrDefault(knockbackId, 0.0D);
        double knockbackMultiplier = KogasaEquipmentData.getAbsorbedAttributeMultipliers(stack)
                .getOrDefault(knockbackId, 0.0D);
        double knockbackGrowth = KogasaAttributeGrowth.multiplier(stack, knockbackId);
        double knockbackValue = KogasaAttributeGrowth.applyCap(knockbackId,
                absorbedKnockback * knockbackGrowth * (1.0D + knockbackMultiplier));
        if (knockbackValue > 0.0D) {
            event.addModifier(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE,
                    new AttributeModifier(
                            java.util.UUID.nameUUIDFromBytes(("kogasa_absorbed:" + knockbackId).getBytes(StandardCharsets.UTF_8)),
                            "Kogasa absorbed knockback", knockbackValue,
                            AttributeModifier.Operation.ADDITION));
        }
        applyAbsorbedMultipliers(event, stack, java.util.Set.of(
                net.minecraft.resources.ResourceLocation.withDefaultNamespace("generic.armor")));
    }

    private static void addTotal(ItemAttributeModifierEvent event, Attribute attribute,
                                 double total, String name) {
        if (total <= 1.0E-6) return;
        event.addModifier(attribute, new AttributeModifier(
                ARMOR_OVERLAY_UUID, name, total,
                AttributeModifier.Operation.ADDITION));
    }

    private static final java.util.UUID ARMOR_OVERLAY_UUID =
            java.util.UUID.fromString("3c2b4d5e-6f70-4a8b-9c1d-2e3f4a5b6c7d");

    private static double sumAddition(Multimap<Attribute, AttributeModifier> modifiers,
                                      Attribute attribute) {
        double sum = 0.0D;
        for (var entry : modifiers.entries()) {
            if (entry.getKey() == attribute
                    && entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                sum += entry.getValue().getAmount();
            }
        }
        return sum;
    }

    /**
     * 只接管原版属性结构的物品类。
     *
     * <p>兼容护甲走 additive overlay：不 clear 原属性，避免和灾变等模组
     * 自己的 {@code ItemAttributeModifierEvent} 叠加成两套数值。
     */
    private static boolean hasNonVanillaAttributes(ItemAttributeModifierEvent event) {
        for (Attribute attribute : event.getOriginalModifiers().keySet()) {
            if (attribute != net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE
                    && attribute != net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED
                    && attribute != net.minecraft.world.entity.ai.attributes.Attributes.ARMOR
                    && attribute != net.minecraft.world.entity.ai.attributes.Attributes.ARMOR_TOUGHNESS
                    && attribute != net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) {
                return true;
            }
        }
        return false;
    }

    private static void applyAbsorbedMultipliers(ItemAttributeModifierEvent event, ItemStack stack,
                                                java.util.Set<net.minecraft.resources.ResourceLocation> skip) {
        for (KogasaEquipmentData.AbsorbedMultiplier entry : KogasaEquipmentData.getAbsorbedMultipliers(stack)) {
            if (skip.contains(entry.attribute()) || entry.amount() <= 0.0D) continue;
            Attribute attribute = ForgeRegistries.ATTRIBUTES.getValue(entry.attribute());
            if (attribute == null) continue;
            event.addModifier(attribute, new AttributeModifier(
                    entry.uuid(), "Kogasa absorbed multiplier " + entry.attribute(),
                    entry.amount(), entry.operation()));
        }
    }

    /** 只有 registry 命名空间为 minecraft 的真正原版装备才允许整体替换属性表。 */
    private static boolean isPlainVanillaItem(Item item) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && "minecraft".equals(id.getNamespace()) && isSafeItem(item);
    }

    private static boolean isSafeItem(Item item) {
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TridentItem
                || item instanceof ArmorItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }
}
