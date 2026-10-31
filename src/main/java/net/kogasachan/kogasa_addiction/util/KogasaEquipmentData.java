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
    /** 弓弩专用：累积吸收的"拉弓速度"（单位 tick，从原版拉满时间中扣减） */
    private static final String DRAW_SPEED_VALUE = "DrawSpeedValue";
    private static final String GROWTH_ENCHANTS_GRANTED = "GrowthEnchantsGranted";
    private static final String GROWTH_ENCHANT_FAIL_NOTIFIED = "GrowthEnchantFailNotified";

    // 新旧位置用的是同一个键名，只是父标签不同：
    //   新位置 → 物品 tag["KogasaEquipment"]["GrowthEnchantFailNotified"]
    //   旧位置 → 物品 tag["GrowthEnchantFailNotified"]（读取兼容，写入时迁移）

    public static boolean hasGrowthEnchantFailNotified(ItemStack stack) {
        if (getTag(stack).getBoolean(GROWTH_ENCHANT_FAIL_NOTIFIED)) return true;

        CompoundTag root = stack.getTag();
        return root != null && root.getBoolean(GROWTH_ENCHANT_FAIL_NOTIFIED);
    }

    public static void setGrowthEnchantFailNotified(ItemStack stack, boolean value) {
        CompoundTag root = stack.getTag();
        if (root != null) {
            root.remove(GROWTH_ENCHANT_FAIL_NOTIFIED);
        }

        CompoundTag tag = getTag(stack);
        if (value) {
            tag.putBoolean(GROWTH_ENCHANT_FAIL_NOTIFIED, true);
            saveTag(stack, tag);
            return;
        }

        if (tag.contains(GROWTH_ENCHANT_FAIL_NOTIFIED)) {
            tag.remove(GROWTH_ENCHANT_FAIL_NOTIFIED);
            if (tag.isEmpty()) {
                // 避免留下一个空的 KogasaEquipment 复合标签
                if (root != null) root.remove(ROOT);
            } else {
                saveTag(stack, tag);
            }
        }
    }

    /**
     * 是否存有任何强化/成长数据（含旧位置残留的标记）。
     */
    public static boolean hasData(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return false;
        if (root.contains(ROOT)) return true;
        return root.getBoolean(GROWTH_ENCHANT_FAIL_NOTIFIED);
    }

    /**
     * 清空该装备的全部强化/成长数据（整个 {@link #ROOT} 复合标签，含旧位置残留的标记）。
     */
    public static void clearAll(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return;

        root.remove(ROOT);
        root.remove(GROWTH_ENCHANT_FAIL_NOTIFIED);
        if (root.isEmpty()) stack.setTag(null);
    }

    public static double getAbsorbedValue(ItemStack stack) {
        return getTag(stack).getDouble(ABSORBED_VALUE);
    }

    /**
     * 已吸收的拉弓速度（tick）。弓弩强化时从材料身上吸收，用于缩短拉满所需时间。
     */
    public static double getDrawSpeedValue(ItemStack stack) {
        return getTag(stack).getDouble(DRAW_SPEED_VALUE);
    }

    public static void addDrawSpeedValue(ItemStack stack, double value) {
        if (value <= 0) return;
        CompoundTag tag = getTag(stack);
        tag.putDouble(DRAW_SPEED_VALUE, tag.getDouble(DRAW_SPEED_VALUE) + value);
        saveTag(stack, tag);
    }

    public static void setDrawSpeedValue(ItemStack stack, double value) {
        CompoundTag tag = getTag(stack);
        tag.putDouble(DRAW_SPEED_VALUE, value);
        saveTag(stack, tag);
    }

    /**
     * 清除强化相关数据（强化次数 / 已吸收材料 / 吸收值 / 拉弓速度），
     * 保留成长值与成长附魔计数。对应铁砧里的水桶清除。
     */
    public static void clearReinforcementData(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null || !root.contains(ROOT)) return;

        CompoundTag tag = root.getCompound(ROOT);
        tag.remove(REINFORCEMENTS);
        tag.remove(USED_MATERIALS);
        tag.remove(ABSORBED_VALUE);
        tag.remove(DRAW_SPEED_VALUE);

        if (tag.isEmpty()) {
            root.remove(ROOT);
            if (root.isEmpty()) stack.setTag(null);
        }
    }

    public static void addAbsorbedValue(ItemStack stack, double value) {
        CompoundTag tag = getTag(stack);
        double current = tag.getDouble(ABSORBED_VALUE);
        tag.putDouble(ABSORBED_VALUE, current + value);
        saveTag(stack, tag);
    }

    public static CompoundTag getTag(ItemStack stack) {
        // 只读访问：写入方会在修改后调用 saveTag。
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.getCompound(ROOT);
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
     * 综合数值倍率
     */
    public static double getMultiplier(ItemStack stack) {
        return KogasaEquipmentHelper.getTotalMultiplier(stack);
    }

    /**
     * 是否已解锁成长
     */
    public static boolean isGrowthUnlocked(ItemStack stack) {
        return getReinforcements(stack) >= ModConfigCache.growthUnlockReinforcements;
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
