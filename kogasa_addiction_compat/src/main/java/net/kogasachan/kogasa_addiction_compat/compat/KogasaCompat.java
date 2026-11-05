package net.kogasachan.kogasa_addiction_compat.compat;

import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/**
 * 兼容层判定入口。
 *
 * <p>兼容物品的三种来源：
 * <ol>
 *   <li>本模组装备（{@link IKogasaEquipment}）；</li>
 *   <li>被彩虹水晶“结合”过、带标记 NBT 的物品；</li>
 *   <li>数据包里 {@code #kogasa_addiction_compat:compatible} 标签内的物品。</li>
 * </ol>
 * 并且要求配置总开关开启（默认关闭）。
 */
public final class KogasaCompat {

    /** 物品根标签上的兼容标记（boolean） */
    public static final String MARKER_KEY = "KogasaCompat";

    private KogasaCompat() {
    }

    /**
     * 兼容总开关。配置还没加载时（游戏启动早期、物品注册阶段）安全地返回 false，
     * 避免在 {@code ItemStack#getMaxDamage} 之类的早期调用里炸掉。
     */
    public static boolean isEnabled() {
        try {
            return CompatConfig.SPEC.isLoaded() && CompatConfig.ENABLE_COMPAT.get();
        } catch (RuntimeException ex) {
            return false;
        }
    }

    /** 是否应用本模组机制（本模组装备也返回 true，但总开关关闭时一律 false） */
    public static boolean isCompatible(ItemStack stack) {
        if (stack.isEmpty() || !isEnabled()) {
            return false;
        }
        if (stack.getItem() instanceof IKogasaEquipment) {
            return true;
        }
        return hasMarker(stack) || stack.is(CompatTags.COMPATIBLE);
    }

    /**
     * 是否需要“通用兼容”处理（即：兼容但不是 {@link IKogasaEquipment}）。
     * 属性/耐久事件只处理这一批，避免和本模组物品自身的重写重复。
     */
    public static boolean isCompatOnly(ItemStack stack) {
        return isCompatible(stack) && !(stack.getItem() instanceof IKogasaEquipment);
    }

    public static boolean hasMarker(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(MARKER_KEY);
    }

    /** 给物品打上兼容标记（彩虹水晶结合时调用） */
    public static void markCompatible(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        stack.getOrCreateTag().putBoolean(MARKER_KEY, true);
    }

    /** 这件非本模组物品上是否有本模组添加的数据（标记/强化/成长/能力/弹药）。 */
    public static boolean hasKogasaData(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        return hasMarker(stack)
                || KogasaEquipmentData.hasData(stack)
                || KogasaAbilityData.hasData(stack);
    }

    /** 删除本模组给非本模组物品添加的全部 NBT（恢复原始状态）。 */
    public static void clearAddedData(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            tag.remove(MARKER_KEY);
            if (tag.isEmpty()) {
                stack.setTag(null);
            }
        }
        KogasaEquipmentData.clearAll(stack);
        KogasaAbilityData.clearAllData(stack);
    }
}
