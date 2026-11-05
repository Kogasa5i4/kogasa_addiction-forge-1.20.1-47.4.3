package net.kogasachan.kogasa_addiction_compat.compat;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

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
                } else {
                    Multimap<Attribute, AttributeModifier> modifiers =
                            KogasaEquipmentHelper.getAttributeModifiers(stack.getItem(), slot, stack);

                    // 安全类物品：用主模组公式整体替换，保证与本模组装备完全一致
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
            event.addModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(GENERIC_DAMAGE_UUID, "Kogasa compat damage",
                            damageDelta, AttributeModifier.Operation.ADDITION));
        }

        double targetSpeed = KogasaEquipmentHelper.getAttackSpeedModifier(stack, baseSpeed);
        double speedDelta = targetSpeed - baseSpeed;
        if (Math.abs(speedDelta) > 1.0E-6) {
            event.addModifier(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED,
                    new AttributeModifier(GENERIC_SPEED_UUID, "Kogasa compat speed",
                            speedDelta, AttributeModifier.Operation.ADDITION));
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

        Multimap<Attribute, AttributeModifier> current = event.getModifiers();
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
        double targetKnockback = net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper
                .getKnockbackResistance(stack);

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
    private static boolean isSafeItem(Item item) {
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TridentItem
                || item instanceof ArmorItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }
}
