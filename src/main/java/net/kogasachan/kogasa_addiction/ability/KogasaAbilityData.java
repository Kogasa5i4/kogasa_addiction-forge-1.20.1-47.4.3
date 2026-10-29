package net.kogasachan.kogasa_addiction.ability;

import net.kogasachan.kogasa_addiction.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class KogasaAbilityData {

    private static final String ROOT = "KogasaAbilities";
    private static final String ABILITIES = "Abilities";
    private static final String NEWS_VALUE = "NewsValue";

    /**
     * 远程弹射物能力标记：发射时写入投掷物 persistentData，
     * 命中时据此判断能力，避免射手中途切换武器导致效果丢失。
     */
    public static final String PROJECTILE_KOISHI = "KogasaKoishiArrow";
    public static final String PROJECTILE_TENSHI = "KogasaTenshiArrow";
    private static final String AYA_KILLED_MOBS = "AyaKilledMobs";
    private static final String AYA_MINED_BLOCKS = "AyaMinedBlocks";
    private static final String AYA_DAMAGES_TAKEN = "AyaDamagesTaken";

    // ==================== 基础读写 ====================

    private static CompoundTag getTag(ItemStack stack) {
        // 只读访问：没有根标签时返回空 compound，避免 Tooltip 渲染等只读路径创建空 NBT。
        CompoundTag tag = stack.getTag();
        return tag == null ? new CompoundTag() : tag.getCompound(ROOT);
    }

    private static void saveTag(ItemStack stack, CompoundTag tag) {
        stack.getOrCreateTag().put(ROOT, tag);
    }

    // ==================== 能力列表 ====================

    /**
     * 获取已拥有的能力列表
     */
    public static List<KogasaAbility> getAbilities(ItemStack stack) {
        ListTag list = getTag(stack).getList(ABILITIES, Tag.TAG_STRING);
        List<KogasaAbility> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            KogasaAbility ability = KogasaAbility.byId(list.getString(i));
            if (ability != null) result.add(ability);
        }
        return result;
    }

    /**
     * 是否拥有指定能力
     */
    public static boolean hasAbility(ItemStack stack, KogasaAbility ability) {
        return getAbilities(stack).contains(ability);
    }

    /**
     * 是否拥有任意能力
     */
    public static boolean hasAnyAbility(ItemStack stack) {
        return !getAbilities(stack).isEmpty();
    }

    /**
     * 添加能力。返回 false 表示已拥有或槽位不足。
     */
    public static boolean addAbility(ItemStack stack, KogasaAbility ability) {
        List<KogasaAbility> current = getAbilities(stack);
        if (current.contains(ability)) return false;
        if (current.size() >= getMaxAbilitySlots(stack)) return false;

        CompoundTag tag = getTag(stack);
        ListTag list = tag.getList(ABILITIES, Tag.TAG_STRING);
        list.add(StringTag.valueOf(ability.getId()));
        tag.put(ABILITIES, list);
        saveTag(stack, tag);
        return true;
    }

    /**
     * 移除能力
     */
    public static boolean removeAbility(ItemStack stack, KogasaAbility ability) {
        List<KogasaAbility> current = getAbilities(stack);
        if (!current.contains(ability)) return false;

        CompoundTag tag = getTag(stack);
        ListTag list = new ListTag();
        for (KogasaAbility a : current) {
            if (a != ability) list.add(StringTag.valueOf(a.getId()));
        }
        tag.put(ABILITIES, list);
        saveTag(stack, tag);
        return true;
    }

    /**
     * 清空所有能力
     */
    public static void clearAbilities(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        tag.put(ABILITIES, new ListTag());
        saveTag(stack, tag);
    }

    // ==================== 能力槽位 ====================

    /**
     * 当前装备的能力槽位上限。
     * 基础 1 个，成长值达到阈值后解锁第 2 个。
     */
    public static int getMaxAbilitySlots(ItemStack stack) {
        double growth = net.kogasachan.kogasa_addiction.util.KogasaEquipmentData.getGrowth(stack);
        double threshold = Config.GROWTH_FOR_SECOND_ABILITY_SLOT.get();
        return growth >= threshold ? 2 : 1;
    }

    /**
     * 已占用的能力槽数量。特殊弹药不占用能力槽位。
     */
    public static int getUsedSlots(ItemStack stack) {
        return getAbilities(stack).size();
    }

    // ==================== Aya 新闻值 ====================

    public static double getNewsValue(ItemStack stack) {
        return getTag(stack).getDouble(NEWS_VALUE);
    }

    public static void addNewsValue(ItemStack stack, double amount) {
        CompoundTag tag = getTag(stack);
        double current = tag.getDouble(NEWS_VALUE);
        double max = Config.MAX_NEWS_VALUE.get();
        tag.putDouble(NEWS_VALUE, Math.min(current + amount, max));
        saveTag(stack, tag);
    }

    // ==================== Aya 已记录集合 ====================

    /**
     * 检查某类记录是否已存在，不存在则添加。返回 true 表示是新记录。
     */
    public static boolean tryAddRecord(ItemStack stack, String key, String value) {
        CompoundTag tag = getTag(stack);
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(value)) return false;
        }
        list.add(StringTag.valueOf(value));
        tag.put(key, list);
        saveTag(stack, tag);
        return true;
    }

    public static int getRecordCount(ItemStack stack, String key) {
        return getTag(stack).getList(key, Tag.TAG_STRING).size();
    }

    public static boolean tryAddKilledMob(ItemStack stack, String mobId) {
        return tryAddRecord(stack, AYA_KILLED_MOBS, mobId);
    }

    public static boolean tryAddMinedBlock(ItemStack stack, String blockId) {
        return tryAddRecord(stack, AYA_MINED_BLOCKS, blockId);
    }

    public static boolean tryAddDamageTaken(ItemStack stack, String damageTypeId) {
        return tryAddRecord(stack, AYA_DAMAGES_TAKEN, damageTypeId);
    }

    public static int getKilledMobCount(ItemStack stack) {
        return getRecordCount(stack, AYA_KILLED_MOBS);
    }

    public static int getMinedBlockCount(ItemStack stack) {
        return getRecordCount(stack, AYA_MINED_BLOCKS);
    }

    public static int getDamageTakenCount(ItemStack stack) {
        return getRecordCount(stack, AYA_DAMAGES_TAKEN);
    }

    public static void setNewsValue(ItemStack stack, double value) {
        CompoundTag tag = getTag(stack);
        tag.putDouble(NEWS_VALUE, value);
        saveTag(stack, tag);
    }

    // ==================== 弹药 ====================

    private static final String AMMO = "Ammo";
    private static final String AMMO_TYPE = "Type";
    private static final String AMMO_POTION = "Potion";
    private static final String AMMO_POTION_KIND = "PotionKind";
    private static final String AMMO_FIREWORK_NBT = "FireworkNbt";

    public enum AmmoType {POTION, FIREWORK}

    public enum PotionKind {REGULAR, SPLASH, LINGERING}

    public static boolean hasAmmo(ItemStack stack) {
        return getTag(stack).contains(AMMO, Tag.TAG_COMPOUND);
    }

    public static void setPotionAmmo(ItemStack stack, ResourceLocation potionId, PotionKind kind) {
        CompoundTag tag = getTag(stack);
        CompoundTag ammo = new CompoundTag();
        ammo.putString(AMMO_TYPE, "potion");
        ammo.putString(AMMO_POTION, potionId.toString());
        ammo.putString(AMMO_POTION_KIND, kind.name().toLowerCase());
        tag.put(AMMO, ammo);
        saveTag(stack, tag);
    }

    public static void setFireworkAmmo(ItemStack stack, CompoundTag fireworkNbt) {
        CompoundTag tag = getTag(stack);
        CompoundTag ammo = new CompoundTag();
        ammo.putString(AMMO_TYPE, "firework");
        ammo.put(AMMO_FIREWORK_NBT, fireworkNbt.copy());
        tag.put(AMMO, ammo);
        saveTag(stack, tag);
    }

    public static void clearAmmo(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        tag.remove(AMMO);
        saveTag(stack, tag);
    }

    @Nullable
    public static AmmoType getAmmoType(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(AMMO, Tag.TAG_COMPOUND)) return null;
        String type = tag.getCompound(AMMO).getString(AMMO_TYPE);
        return switch (type) {
            case "potion" -> AmmoType.POTION;
            case "firework" -> AmmoType.FIREWORK;
            default -> null;
        };
    }

    @Nullable
    public static ResourceLocation getAmmoPotionId(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(AMMO, Tag.TAG_COMPOUND)) return null;
        String id = tag.getCompound(AMMO).getString(AMMO_POTION);
        return id.isEmpty() ? null : ResourceLocation.tryParse(id);
    }

    public static PotionKind getAmmoPotionKind(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(AMMO, Tag.TAG_COMPOUND)) return PotionKind.REGULAR;
        String kind = tag.getCompound(AMMO).getString(AMMO_POTION_KIND);
        return switch (kind) {
            case "splash" -> PotionKind.SPLASH;
            case "lingering" -> PotionKind.LINGERING;
            default -> PotionKind.REGULAR;
        };
    }

    /**
     * 从 NBT 重建烟花 ItemStack
     */
    public static ItemStack getFireworkStack(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(AMMO, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        CompoundTag nbt = tag.getCompound(AMMO).getCompound(AMMO_FIREWORK_NBT);
        if (nbt.isEmpty()) return ItemStack.EMPTY;
        ItemStack firework = new ItemStack(net.minecraft.world.item.Items.FIREWORK_ROCKET);
        firework.setTag(nbt.copy());
        return firework;
    }
}
