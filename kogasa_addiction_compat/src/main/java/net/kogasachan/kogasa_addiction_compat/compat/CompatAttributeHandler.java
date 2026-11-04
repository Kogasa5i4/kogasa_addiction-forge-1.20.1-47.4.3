package net.kogasachan.kogasa_addiction_compat.compat;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.event.ItemAttributeModifierEvent;
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

    private CompatAttributeHandler() {
    }

    @SubscribeEvent
    public static void onAttributeModifiers(ItemAttributeModifierEvent event) {
        if (COMPUTING.get()) {
            return;
        }

        ItemStack stack = event.getItemStack();
        if (!KogasaCompat.isCompatOnly(stack)) {
            return;
        }
        if (!isSafeItem(stack.getItem())) {
            return;
        }

        COMPUTING.set(Boolean.TRUE);
        try {
            EquipmentSlot slot = event.getSlotType();
            Multimap<Attribute, AttributeModifier> modifiers =
                    KogasaEquipmentHelper.getAttributeModifiers(stack.getItem(), slot, stack);

            // 安全类物品：用主模组公式整体替换，保证与本模组装备完全一致
            event.clearModifiers();
            modifiers.forEach(event::addModifier);
        } finally {
            COMPUTING.set(Boolean.FALSE);
        }
    }

    /** 只接管原版属性结构的物品类 */
    private static boolean isSafeItem(Item item) {
        return item instanceof SwordItem
                || item instanceof DiggerItem
                || item instanceof TridentItem
                || item instanceof ArmorItem
                || item instanceof BowItem
                || item instanceof CrossbowItem;
    }
}
