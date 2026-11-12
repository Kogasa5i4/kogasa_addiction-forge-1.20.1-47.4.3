package net.kogasachan.kogasa_addiction.util;

import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

import java.util.*;

public class KogasaEquipmentData {

    private static final String ROOT = "KogasaEquipment";
    private static final String REINFORCEMENTS = "Reinforcements";
    private static final String USED_MATERIALS = "UsedMaterials";
    private static final String GROWTH = "Growth";
    private static final String ABSORBED_VALUE = "AbsorbedValue";
    private static final String ABSORBED_ATTRIBUTES = "AbsorbedAttributes";
    /** addAbsorbedValue 已同时写入 scalar 与 AbsorbedAttributes 的标记。 */
    private static final String ABSORBED_SYNCED = "AbsorbedValueSynced";
    private static final String ABSORBED_ATTRIBUTE_MULTIPLIERS = "AbsorbedAttributeMultipliers";
    private static final String ABSORBED_MULTIPLIER_ENTRIES = "AbsorbedMultiplierEntries";
    /** 弓弩专用：累积吸收的"拉弓速度"（单位 tick，从原版拉满时间中扣减） */
    private static final String DRAW_SPEED_VALUE = "DrawSpeedValue";
    private static final String GROWTH_ENCHANTS_GRANTED = "GrowthEnchantsGranted";
    private static final String GROWTH_ENCHANT_FAIL_NOTIFIED = "GrowthEnchantFailNotified";
    /** 战利品开出的"封印装备"：能力已注入但不生效，成长达到阈值后自动解封 */
    private static final String LOCKED = "Locked";

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
            if (isEmptyIgnoringVersion(tag)) {
                // 避免留下一个空的 KogasaEquipment 复合标签
                if (root != null) root.remove(ROOT);
            } else {
                saveTag(stack, tag);
            }
        }
    }

    /**
     * 去掉版本号字段后是否为空：单独存在的 SchemaVersion 不算有效数据，
     * 清空类操作应当把这种"空壳根"一并删掉。
     */
    private static boolean isEmptyIgnoringVersion(CompoundTag tag) {
        if (tag.isEmpty()) return true;
        return tag.size() == 1 && tag.contains(KEY_SCHEMA_VERSION);
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
        CompoundTag tag = getTag(stack);
        CompoundTag attributes = tag.getCompound(ABSORBED_ATTRIBUTES);
        if (attributes.contains("minecraft:generic.attack_damage")) {
            return attributes.getDouble("minecraft:generic.attack_damage");
        }
        if (attributes.contains("minecraft:generic.armor")) {
            return attributes.getDouble("minecraft:generic.armor");
        }
        return tag.getDouble(ABSORBED_VALUE);
    }

    /** 指定属性的吸收值；旧装备的 scalar 只作为 attack/armor 的兼容回退。 */
    public static double getAbsorbedValue(ItemStack stack, ResourceLocation attributeId) {
        CompoundTag tag = getTag(stack);
        CompoundTag attributes = tag.getCompound(ABSORBED_ATTRIBUTES);
        if (attributes.contains(attributeId.toString())) {
            return attributes.getDouble(attributeId.toString());
        }
        double legacy = tag.getDouble(ABSORBED_VALUE);
        if (legacy <= 0.0D) return 0.0D;
        if (attributeId.equals(ResourceLocation.withDefaultNamespace("generic.attack_damage"))
                && !(stack.getItem() instanceof ArmorItem)) {
            return legacy;
        }
        if (attributeId.equals(ResourceLocation.withDefaultNamespace("generic.armor"))
                && stack.getItem() instanceof ArmorItem) {
            return legacy;
        }
        return 0.0D;
    }

    /** 已吸收的额外属性（attribute id → value），不包含旧 scalar。 */
    public static Map<ResourceLocation, Double> getAbsorbedAttributes(ItemStack stack) {
        CompoundTag attributes = getTag(stack).getCompound(ABSORBED_ATTRIBUTES);
        Map<ResourceLocation, Double> result = new LinkedHashMap<>();
        for (String key : attributes.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                result.put(id, attributes.getDouble(key));
            }
        }
        return result;
    }

    public static void addAbsorbedAttribute(ItemStack stack, ResourceLocation attributeId, double value) {
        if (value <= 0.0D) return;
        CompoundTag tag = getTag(stack);
        CompoundTag attributes = tag.getCompound(ABSORBED_ATTRIBUTES);
        String key = attributeId.toString();
        attributes.putDouble(key, attributes.getDouble(key) + value);
        tag.put(ABSORBED_ATTRIBUTES, attributes);
        tag.putBoolean(ABSORBED_SYNCED, true);
        saveTag(stack, tag);
    }

    public static void setAbsorbedAttribute(ItemStack stack, ResourceLocation attributeId, double value) {
        CompoundTag tag = getTag(stack);
        CompoundTag attributes = tag.getCompound(ABSORBED_ATTRIBUTES);
        String key = attributeId.toString();
        if (value <= 0.0D) {
            attributes.remove(key);
        } else {
            attributes.putDouble(key, value);
        }
        if (attributes.isEmpty()) {
            tag.remove(ABSORBED_ATTRIBUTES);
        } else {
            tag.put(ABSORBED_ATTRIBUTES, attributes);
        }
        saveTag(stack, tag);
    }

    public static boolean hasAbsorbedAttributes(ItemStack stack) {
        return !getTag(stack).getCompound(ABSORBED_ATTRIBUTES).isEmpty();
    }

    public static Map<ResourceLocation, Double> getAbsorbedAttributeMultipliers(ItemStack stack) {
        CompoundTag attributes = getTag(stack).getCompound(ABSORBED_ATTRIBUTE_MULTIPLIERS);
        Map<ResourceLocation, Double> result = new LinkedHashMap<>();
        for (String key : attributes.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id != null) {
                result.put(id, attributes.getDouble(key));
            }
        }
        return result;
    }

    public record AbsorbedMultiplier(UUID uuid, ResourceLocation attribute, double amount,
                                     net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
    }

    /** 保留来源装备 UUID / operation 的乘法吸收条目。 */
    public static List<AbsorbedMultiplier> getAbsorbedMultipliers(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        List<AbsorbedMultiplier> result = new java.util.ArrayList<>();
        ListTag list = tag.getList(ABSORBED_MULTIPLIER_ENTRIES, Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompound(i);
            ResourceLocation id = ResourceLocation.tryParse(entry.getString("Attribute"));
            UUID uuid = null;
            if (entry.contains("Uuid")) {
                Tag pTag = entry.get("Uuid");
                if (pTag != null) {
                    uuid = NbtUtils.loadUUID(pTag);
                }
            }
            if (id == null || uuid == null) continue;
            var op = parseOperation(entry.getString("Operation"));
            result.add(new AbsorbedMultiplier(uuid, id, entry.getDouble("Amount"), op));
        }
        if (!result.isEmpty()) return result;

        // 旧数据：AbsorbedAttributeMultipliers 是按属性合并的 map，没有来源 UUID。
        CompoundTag legacy = tag.getCompound(ABSORBED_ATTRIBUTE_MULTIPLIERS);
        for (String key : legacy.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(key);
            if (id == null) continue;
            UUID uuid = UUID.nameUUIDFromBytes(("kogasa_legacy_multiplier:" + key).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            result.add(new AbsorbedMultiplier(uuid, id, legacy.getDouble(key),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE));
        }
        return result;
    }

    private static net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation parseOperation(String name) {
        try {
            return net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.valueOf(name);
        } catch (RuntimeException ignored) {
            return net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.MULTIPLY_BASE;
        }
    }

    public static void addAbsorbedMultiplier(ItemStack stack, UUID uuid, ResourceLocation attributeId,
                                             double value,
                                             net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation operation) {
        if (value <= 0.0D || uuid == null) return;
        CompoundTag tag = getTag(stack);
        ListTag list = tag.getList(ABSORBED_MULTIPLIER_ENTRIES, 10).copy();
        CompoundTag entry = new CompoundTag();
        entry.put("Uuid", net.minecraft.nbt.NbtUtils.createUUID(uuid));
        entry.putString("Attribute", attributeId.toString());
        entry.putDouble("Amount", value);
        entry.putString("Operation", operation.name());
        list.add(entry);
        tag.put(ABSORBED_MULTIPLIER_ENTRIES, list);
        saveTag(stack, tag);
    }

    public static void addAbsorbedAttributeMultiplier(ItemStack stack, ResourceLocation attributeId, double value) {
        if (value <= 0.0D) return;
        CompoundTag tag = getTag(stack);
        CompoundTag attributes = tag.getCompound(ABSORBED_ATTRIBUTE_MULTIPLIERS);
        String key = attributeId.toString();
        attributes.putDouble(key, attributes.getDouble(key) + value);
        tag.put(ABSORBED_ATTRIBUTE_MULTIPLIERS, attributes);
        saveTag(stack, tag);
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
        tag.remove(ABSORBED_ATTRIBUTES);
        tag.remove(ABSORBED_SYNCED);
        tag.remove(ABSORBED_ATTRIBUTE_MULTIPLIERS);
        tag.remove(ABSORBED_MULTIPLIER_ENTRIES);
        tag.remove(DRAW_SPEED_VALUE);

        if (isEmptyIgnoringVersion(tag)) {
            root.remove(ROOT);
            if (root.isEmpty()) stack.setTag(null);
        }
    }

    public static void addAbsorbedValue(ItemStack stack, double value) {
        if (value <= 0.0D) return;
        CompoundTag tag = getTag(stack);
        double current = tag.getDouble(ABSORBED_VALUE);
        tag.putDouble(ABSORBED_VALUE, current + value);

        // v3 之后 getAbsorbedValue 优先读 AbsorbedAttributes；只写 scalar 会让
        // 后续吸收看起来不增长（迁移过的装备尤其明显）。主属性必须同步累加。
        String key = stack.getItem() instanceof net.minecraft.world.item.ArmorItem
                ? "minecraft:generic.armor"
                : "minecraft:generic.attack_damage";
        CompoundTag attributes = tag.getCompound(ABSORBED_ATTRIBUTES);
        attributes.putDouble(key, attributes.getDouble(key) + value);
        tag.put(ABSORBED_ATTRIBUTES, attributes);
        saveTag(stack, tag);
    }

    /** 装备数据模式版本（存于 KogasaEquipment 根内）。结构变更时 +1，并在 {@link #migrate} 里补步骤。 */
    public static final int SCHEMA_VERSION = 4;
    private static final String KEY_SCHEMA_VERSION = "SchemaVersion";

    public static CompoundTag getTag(ItemStack stack) {
        // 只读访问：写入方会在修改后调用 saveTag。
        CompoundTag tag = stack.getTag();
        if (tag == null) return new CompoundTag();

        CompoundTag root = tag.getCompound(ROOT);
        if (root.isEmpty()) return root;
        if (ensureMigrated(stack, root)) {
            // 迁移结果必须写回 ItemStack，否则只改了内存里的 CompoundTag，
            // 下次从磁盘读取仍是旧 SchemaVersion。
            saveTag(stack, root);
        }
        return root;
    }

    /**
     * 幂等迁移：把根标签从旧版本逐步升级到 {@link #SCHEMA_VERSION}。
     * 以后改数据结构时：SCHEMA_VERSION +1，并在 {@link #migrate} 里为旧版本号加一个 case。
     */
    private static boolean ensureMigrated(ItemStack stack, CompoundTag root) {
        int initialVersion = root.getInt(KEY_SCHEMA_VERSION);
        int version = initialVersion;
        while (version < SCHEMA_VERSION) {
            version = migrate(stack, root, version);
        }
        boolean changed = version != initialVersion
                || root.getInt(KEY_SCHEMA_VERSION) != SCHEMA_VERSION;
        if (root.getInt(KEY_SCHEMA_VERSION) != SCHEMA_VERSION) {
            root.putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
            changed = true;
        }
        return changed;
    }

    private static int migrate(ItemStack stack, CompoundTag root, int from) {
        return switch (from) {
            // v0 → v1：旧版没有版本字段，结构本身未变，仅盖章。
            // 顶层遗留的 GrowthEnchantFailNotified 由 hasGrowthEnchantFailNotified /
            // setGrowthEnchantFailNotified 的兼容读写在访问时自然迁移。
            case 0 -> 1;
            // v1 → v2：新增 Locked 键（无旧数据需要搬运，仅盖章）。
            case 1 -> 2;
            // v2 → v3：旧的单个 AbsorbedValue 迁移到 AbsorbedAttributes。
            case 2 -> {
                migrateLegacyAbsorbedValue(stack, root);
                yield 3;
            }
            // v3 → v4：修复 scalar 与 AbsorbedAttributes 可能不同步的历史数据。
            case 3 -> {
                migrateAbsorbedValueConsistency(stack, root);
                yield 4;
            }
            default -> SCHEMA_VERSION; // 未知版本号，保守跳到最新
        };
    }

    private static void migrateLegacyAbsorbedValue(ItemStack stack, CompoundTag root) {
        double legacy = root.getDouble(ABSORBED_VALUE);
        if (legacy <= 0.0D) return;

        CompoundTag attributes = root.getCompound(ABSORBED_ATTRIBUTES);
        String key = stack.getItem() instanceof ArmorItem
                ? "minecraft:generic.armor"
                : "minecraft:generic.attack_damage";
        if (!attributes.contains(key)) {
            attributes.putDouble(key, legacy);
        }
        if (attributes.isEmpty()) {
            root.remove(ABSORBED_ATTRIBUTES);
        } else {
            root.put(ABSORBED_ATTRIBUTES, attributes);
        }
        root.remove(ABSORBED_VALUE);
    }

    /**
     * v3 期间 addAbsorbedValue 只写 scalar，而 getAbsorbedValue 优先读
     * AbsorbedAttributes，导致旧物品后续吸收没有反映到主属性。
     *
     * <p>迁移策略：
     * <ul>
     *   <li>已有同步标记：两者视为同一份总值，取较大值，避免重复；</li>
     *   <li>没有同步标记且两者都有值：视为旧版“迁移基数 + 后续 scalar 增量”，相加；</li>
     *   <li>只有一边有值：直接使用该值。</li>
     * </ul>
     * 最终把 scalar 和主属性都写成同一个总值，并打上同步标记。
     */
    private static void migrateAbsorbedValueConsistency(ItemStack stack, CompoundTag root) {
        String key = stack.getItem() instanceof ArmorItem
                ? "minecraft:generic.armor"
                : "minecraft:generic.attack_damage";
        CompoundTag attributes = root.getCompound(ABSORBED_ATTRIBUTES);
        double scalar = root.getDouble(ABSORBED_VALUE);
        double attribute = attributes.getDouble(key);
        boolean synced = root.getBoolean(ABSORBED_SYNCED);

        double total;
        if (scalar <= 0.0D) {
            total = attribute;
        } else if (attribute <= 0.0D) {
            total = scalar;
        } else if (synced || Math.abs(scalar - attribute) < 1.0E-6D) {
            total = Math.max(scalar, attribute);
        } else {
            // 旧 v3 数据：attribute 是迁移前的总值，scalar 是迁移后追加的增量。
            total = scalar + attribute;
        }

        if (total > 0.0D) {
            attributes.putDouble(key, total);
            root.put(ABSORBED_ATTRIBUTES, attributes);
            root.putDouble(ABSORBED_VALUE, total);
            root.putBoolean(ABSORBED_SYNCED, true);
        } else {
            root.remove(ABSORBED_VALUE);
            root.remove(ABSORBED_SYNCED);
        }
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
     * 是否处于封印状态（战利品开出的装备）：能力已注入但不生效。
     */
    public static boolean isLocked(ItemStack stack) {
        return getTag(stack).getBoolean(LOCKED);
    }

    /**
     * 设置/解除封印。解除时若根标签只剩版本号，连空壳根一并删除。
     */
    public static void setLocked(ItemStack stack, boolean locked) {
        CompoundTag tag = getTag(stack);
        if (locked) {
            tag.putBoolean(LOCKED, true);
            saveTag(stack, tag);
            return;
        }

        if (tag.contains(LOCKED)) {
            tag.remove(LOCKED);
            if (isEmptyIgnoringVersion(tag)) {
                CompoundTag root = stack.getTag();
                if (root != null) {
                    root.remove(ROOT);
                    if (root.isEmpty()) stack.setTag(null);
                }
            } else {
                saveTag(stack, tag);
            }
        }
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
