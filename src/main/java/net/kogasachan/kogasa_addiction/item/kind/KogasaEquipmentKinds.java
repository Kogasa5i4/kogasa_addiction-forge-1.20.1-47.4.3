package net.kogasachan.kogasa_addiction.item.kind;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;

/**
 * 装备类型查表：按枚举声明顺序返回第一个命中的类型，未命中返回 {@code null}。
 *
 * <p>顺序即优先级，由 {@link KogasaEquipmentKind} 的声明顺序决定 —— 那里注释说明了
 * 为什么"镐早于斧""弩早于弓""Fumo/飞刀早于通用类型"不能调换。
 */
public final class KogasaEquipmentKinds {

    private static final KogasaEquipmentKind[] ORDER = KogasaEquipmentKind.values();

    private KogasaEquipmentKinds() {
    }

    @Nullable
    public static KogasaEquipmentKind of(ItemStack stack) {
        return stack.isEmpty() ? null : of(stack.getItem());
    }

    @Nullable
    public static KogasaEquipmentKind of(Item item) {
        if (item == null) return null;
        for (KogasaEquipmentKind kind : ORDER) {
            if (kind.matches(item)) return kind;
        }
        return null;
    }
}
