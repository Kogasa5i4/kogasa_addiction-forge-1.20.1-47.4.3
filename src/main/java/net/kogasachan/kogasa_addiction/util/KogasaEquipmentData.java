package net.kogasachan.kogasa_addiction.util;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class KogasaEquipmentData {

    private static final String ROOT = "KogasaEquipment";
    private static final String REINFORCEMENTS = "Reinforcements";
    private static final String USED_MATERIALS = "UsedMaterials";
    private static final String GROWTH = "Growth";
    private static final String ABSORBED_VALUE = "AbsorbedValue";
    private static final String GROWTH_ENCHANTS_GRANTED = "GrowthEnchantsGranted";

    public static double getAbsorbedValue(ItemStack stack) {
        return getTag(stack).getDouble(ABSORBED_VALUE);
    }

    public static void addAbsorbedValue(ItemStack stack, double value) {
        CompoundTag tag = getTag(stack);
        double current = tag.getDouble(ABSORBED_VALUE);
        tag.putDouble(ABSORBED_VALUE, current + value);
        saveTag(stack, tag);
    }

    public static CompoundTag getTag(ItemStack stack) {
        return stack.getOrCreateTag().getCompound(ROOT);
    }

    public static void saveTag(ItemStack stack, CompoundTag tag) {
        stack.getOrCreateTag().put(ROOT, tag);
    }

    public static int getReinforcements(ItemStack stack) {
        return getTag(stack).getInt(REINFORCEMENTS);
    }

    public static void setReinforcements(ItemStack stack, int value) {
        CompoundTag tag = getTag(stack);
        tag.putInt(REINFORCEMENTS, value);
        saveTag(stack, tag);
    }

    public static boolean hasUsedMaterial(ItemStack stack, String materialId) {
        ListTag list = getTag(stack).getList(USED_MATERIALS, 8);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(materialId)) return true;
        }
        return false;
    }

    public static void addUsedMaterial(ItemStack stack, String materialId) {
        CompoundTag tag = getTag(stack);
        ListTag list = tag.getList(USED_MATERIALS, 8);
        list.add(StringTag.valueOf(materialId));
        tag.put(USED_MATERIALS, list);
        saveTag(stack, tag);
    }

    public static double getGrowth(ItemStack stack) {
        return getTag(stack).getDouble(GROWTH);
    }

    public static void setGrowth(ItemStack stack, double value) {
        CompoundTag tag = getTag(stack);
        tag.putDouble(GROWTH, value);
        saveTag(stack, tag);
    }

    public static int getGrowthEnchantsGranted(ItemStack stack) {
        return getTag(stack).getInt(GROWTH_ENCHANTS_GRANTED);
    }

    public static void setGrowthEnchantsGranted(ItemStack stack, int value) {
        CompoundTag tag = getTag(stack);
        tag.putInt(GROWTH_ENCHANTS_GRANTED, value);
        saveTag(stack, tag);
    }

    /**
     * 综合数值倍率：强化 + 成长
     */
    public static double getMultiplier(ItemStack stack) {
        int re = getReinforcements(stack);
        double growth = getGrowth(stack);
        return (1.0 + re * 0.1) * (1.0 + growth);
    }

    /**
     * 是否已解锁成长
     */
    public static boolean isGrowthUnlocked(ItemStack stack) {
        return getReinforcements(stack) >= MixinConfigCache.growthUnlockReinforcements;
    }

    /**
     * 获取强化用过的道具
     */
    public static List<String> getUsedMaterials(ItemStack stack) {
        ListTag list = getTag(stack).getList(USED_MATERIALS, 8);
        List<String> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            result.add(list.getString(i));
        }
        return result;
    }
}
