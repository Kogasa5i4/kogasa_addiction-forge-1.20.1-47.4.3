package net.kogasachan.kogasa_addiction.ability;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class KogasaAbilityHelper {

    private KogasaAbilityHelper() {}

    /**
     * 任意护甲槽是否装备了带指定能力的物品
     */
    public static boolean hasAbilityInArmor(Player player, KogasaAbility ability) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (slot.getType() != EquipmentSlot.Type.ARMOR) continue;
            if (hasAbilityInSlot(player, slot, ability)) return true;
        }
        return false;
    }

    /**
     * 主手是否持有带指定能力的物品
     */
    public static boolean hasAbilityInMainHand(Player player, KogasaAbility ability) {
        return hasAbilityInSlot(player, EquipmentSlot.MAINHAND, ability);
    }

    /**
     * 指定槽位是否装备了带指定能力的物品
     */
    public static boolean hasAbilityInSlot(Player player, EquipmentSlot slot, KogasaAbility ability) {
        ItemStack stack = player.getItemBySlot(slot);
        return stack.getItem() instanceof IKogasaEquipment
                && KogasaAbilityData.hasAbility(stack, ability);
    }

    /**
     * 主手或副手中第一个 Kogasa 装备；都没有则返回 EMPTY
     */
    public static ItemStack getHeldKogasaEquipment(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IKogasaEquipment) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof IKogasaEquipment) return off;
        return ItemStack.EMPTY;
    }

    /**
     * 该物品是 Kogasa 装备且带有指定能力
     */
    public static boolean isEquipmentWithAbility(ItemStack stack, KogasaAbility ability) {
        return stack.getItem() instanceof IKogasaEquipment
                && KogasaAbilityData.hasAbility(stack, ability);
    }
}
