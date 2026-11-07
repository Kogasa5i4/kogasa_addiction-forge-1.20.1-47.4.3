package net.kogasachan.kogasa_addiction.ability;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.ForgeRegistry;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nullable;
import java.util.*;

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

    /** 能力数据模式版本（存于 KogasaAbilities 根内）。结构变更时 +1，并在 {@link #migrate} 里补步骤。 */
    public static final int SCHEMA_VERSION = 1;
    private static final String KEY_SCHEMA_VERSION = "SchemaVersion";

    private static CompoundTag getTag(ItemStack stack) {
        // 只读访问：没有根标签时返回空 compound，避免 Tooltip 渲染等只读路径创建空 NBT。
        CompoundTag tag = stack.getTag();
        if (tag == null) return new CompoundTag();

        CompoundTag root = tag.getCompound(ROOT);
        ensureMigrated(root);
        return root;
    }

    /**
     * 幂等迁移：把根标签从旧版本逐步升级到 {@link #SCHEMA_VERSION}。
     * 以后改数据结构时：SCHEMA_VERSION +1，并在 {@link #migrate} 里为旧版本号加一个 case。
     */
    private static void ensureMigrated(CompoundTag root) {
        int version = root.getInt(KEY_SCHEMA_VERSION);
        while (version < SCHEMA_VERSION) {
            version = migrate(root, version);
        }
        if (root.getInt(KEY_SCHEMA_VERSION) != SCHEMA_VERSION) {
            root.putInt(KEY_SCHEMA_VERSION, SCHEMA_VERSION);
        }
    }

    private static int migrate(CompoundTag root, int from) {
        return switch (from) {
            // v0 → v1：旧版没有版本字段，结构本身未变，仅盖章。
            // 注意：Aya 记录的"字符串列表 → 位图"迁移由 bitmapOf() 惰性完成，不在版本链里重复。
            case 0 -> 1;
            default -> SCHEMA_VERSION; // 未知版本号，保守跳到最新
        };
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
     * 一次解析出能力集合，供需要连续判断多个能力的调用方复用，
     * 避免在同一段渲染/结算逻辑里反复解析 NBT。
     */
    public static Set<KogasaAbility> getAbilitySet(ItemStack stack) {
        ListTag list = getTag(stack).getList(ABILITIES, Tag.TAG_STRING);
        Set<KogasaAbility> result = EnumSet.noneOf(KogasaAbility.class);
        for (int i = 0; i < list.size(); i++) {
            KogasaAbility ability = KogasaAbility.byId(list.getString(i));
            if (ability != null) result.add(ability);
        }
        return result;
    }

    /**
     * 是否拥有指定能力。
     * 直接扫描字符串列表，不创建任何临时集合 —— 该方法处在属性计算与 Tooltip 渲染的热路径上
     * （每次属性重算 / 每帧多次调用），因此刻意避免走 {@link #getAbilities(ItemStack)}。
     *
     * <p>封印中的装备（{@link KogasaEquipmentData#isLocked}）视为没有任何能力：
     * 能力已注入但尚未生效，成长达标解封后恢复。
     */
    public static boolean hasAbility(ItemStack stack, KogasaAbility ability) {
        if (ability == null) return false;
        if (KogasaEquipmentData.isLocked(stack)) return false;

        String id = ability.getId();
        ListTag list = getTag(stack).getList(ABILITIES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.getString(i))) return true;
        }
        return false;
    }

    /**
     * 是否拥有任意能力（封印中视为没有）。
     */
    public static boolean hasAnyAbility(ItemStack stack) {
        if (KogasaEquipmentData.isLocked(stack)) return false;
        return !getAbilitySet(stack).isEmpty();
    }

    /**
     * 添加能力
     */
    public static void addAbility(ItemStack stack, KogasaAbility ability) {
        List<KogasaAbility> current = getAbilities(stack);
        if (current.contains(ability)) return;
        if (current.size() >= getMaxAbilitySlots(stack)) return;

        CompoundTag tag = getTag(stack);
        ListTag list = tag.getList(ABILITIES, Tag.TAG_STRING);
        list.add(StringTag.valueOf(ability.getId()));
        tag.put(ABILITIES, list);
        saveTag(stack, tag);
    }

    /**
     * 战利品注入专用：无视能力槽限制直接写入一个能力（已存在则跳过）。
     * 配合 {@link KogasaEquipmentData#setLocked} 产出"封印装备"。
     */
    public static void forceAddAbility(ItemStack stack, KogasaAbility ability) {
        if (ability == null) return;

        String id = ability.getId();
        CompoundTag tag = getTag(stack);
        ListTag list = tag.getList(ABILITIES, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            if (id.equals(list.getString(i))) return;
        }
        list.add(StringTag.valueOf(id));
        tag.put(ABILITIES, list);
        saveTag(stack, tag);
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

    /**
     * 是否存有任何能力侧数据。
     */
    public static boolean hasData(ItemStack stack) {
        CompoundTag root = stack.getTag();
        return root != null && root.contains(ROOT);
    }

    /**
     * 清空该装备的全部能力侧数据（整个 {@link #ROOT} 复合标签：
     * 能力列表、新闻值与记录、弹药）。用于命令的整体重置。
     */
    public static void clearAllData(ItemStack stack) {
        CompoundTag root = stack.getTag();
        if (root == null) return;

        root.remove(ROOT);
        if (root.isEmpty()) stack.setTag(null);
    }

    // ==================== 能力槽位 ====================

    /**
     * 当前装备的能力槽位上限（等差数列解锁）。
     *
     * <p>第 N 个槽在 {@code abilitySlotInitialGrowth + (N-1) * abilitySlotGrowthIncrement}
     * 处解锁，最多 {@code abilitySlotMax} 个。
     * 默认 0 / 0.5 / 4：第 1 槽 0 成长即可用，之后每 +0.5 成长多一个槽。
     */
    public static int getMaxAbilitySlots(ItemStack stack) {
        double growth = net.kogasachan.kogasa_addiction.util.KogasaEquipmentData.getGrowth(stack);
        double initial = Config.ABILITY_SLOT_INITIAL_GROWTH.get();
        double increment = Config.ABILITY_SLOT_GROWTH_INCREMENT.get();
        int max = Config.ABILITY_SLOT_MAX.get();

        if (growth < initial) return 0;
        if (increment <= 0.0D) return max;

        long slots = (long) Math.floor((growth - initial) / increment) + 1L;
        return (int) Math.min(slots, max);
    }

    /**
     * 第 (unlockedSlots + 1) 个槽解锁所需的成长值（unlockedSlots 为当前已解锁数量）。
     * 用于 tooltip 显示"下一个槽还要多少成长"。
     */
    public static double getGrowthForNextSlot(int unlockedSlots) {
        double initial = Config.ABILITY_SLOT_INITIAL_GROWTH.get();
        double increment = Config.ABILITY_SLOT_GROWTH_INCREMENT.get();
        return initial + Math.max(0, unlockedSlots) * increment;
    }

    /**
     * 已占用的能力槽数量。特殊弹药不占用能力槽位。
     * 用集合去重统计，避免历史数据里出现重复 id 时槽位虚高。
     */
    public static int getUsedSlots(ItemStack stack) {
        return getAbilitySet(stack).size();
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

    // ==================== Aya 已记录集合（位图） ====================
    //
    // 旧实现把"记录过的生物/方块/伤害类型"存成字符串 ID 列表，每遇到一种新的就追加一条，
    // 总量随注册表无限增长（长期存档可上千条）。物品 NBT 会随容器同步下发给客户端，
    // 超过包上限后这件装备就无法同步了。
    //
    // 现在改成按**注册表数字 ID** 打位的 long[]，体积有硬上限：
    //   方块 ~1200 个 → 19 个 long → 152 字节
    //   生物 ~150 个  → 3 个 long → 24 字节
    //   伤害类型      → 1 个 long
    // 语义完全不变：仍是"这件装备记没记录过 X"，新闻值仍按件累计。
    //
    // 之所以没把记录搬到玩家身上：玩家的 persistentData 也是无上限增长的（只是不走物品同步），
    // 所以"确保不溢出"严格来说不成立；位图则是构造上就有界，且不改变"按件"的既有玩法。

    /** 伤害类型记录（字符串列表）的硬上限，防止数据包驱动的注册表把 NBT 撑爆 */
    private static final int MAX_DAMAGE_TYPE_RECORDS = 256;

    /**
     * 记录一种生物。返回 true 表示是新记录。
     */
    public static boolean tryAddKilledMob(ItemStack stack, EntityType<?> type) {
        return tryAddBit(stack, AYA_KILLED_MOBS, idOf(ForgeRegistries.ENTITY_TYPES, type));
    }

    /**
     * 记录一种方块。返回 true 表示是新记录。
     */
    public static boolean tryAddMinedBlock(ItemStack stack, Block block) {
        return tryAddBit(stack, AYA_MINED_BLOCKS, idOf(ForgeRegistries.BLOCKS, block));
    }

    /**
     * 记录一种伤害类型。返回 true 表示是新记录。
     *
     * <p>伤害类型是**数据包驱动的注册表**（{@code Registries.DAMAGE_TYPE}），
     * {@code BuiltInRegistries} / {@code ForgeRegistries} 都没有对应的静态字段，
     * 数据层拿不到它的数字 ID —— 所以这一类仍用字符串列表，
     * 但加了硬上限 {@link #MAX_DAMAGE_TYPE_RECORDS}。伤害类型总量本来就小（原版约 40 个），
     * 这样既保留"记过哪种伤害"的语义，又不会像原来那样无界增长。
     */
    public static boolean tryAddDamageTaken(ItemStack stack, ResourceLocation damageTypeId) {
        if (damageTypeId == null) return false;

        CompoundTag tag = getTag(stack);
        ListTag list = tag.getList(AYA_DAMAGES_TAKEN, Tag.TAG_STRING);
        String value = damageTypeId.toString();

        for (int i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(value)) return false;
        }
        if (list.size() >= MAX_DAMAGE_TYPE_RECORDS) return false;

        list.add(StringTag.valueOf(value));
        tag.put(AYA_DAMAGES_TAKEN, list);
        saveTag(stack, tag);
        return true;
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

    /**
     * 记录总数。位图（生物/方块）按置位数统计，字符串列表（伤害类型）按条目数统计。
     */
    public static int getRecordCount(ItemStack stack, String key) {
        Tag raw = getTag(stack).get(key);

        if (raw instanceof LongArrayTag longArray) {
            int count = 0;
            for (long word : longArray.getAsLongArray()) {
                count += Long.bitCount(word);
            }
            return count;
        }
        if (raw instanceof ListTag list) {
            return list.size();
        }
        return 0;
    }

    // ==================== 位图内部实现 ====================

    private static boolean tryAddBit(ItemStack stack, String key, int id) {
        if (id < 0) return false;

        // 这一步可能触发旧字符串列表 → 位图的迁移，并已写回
        long[] bits = bitmapOf(stack, key);

        int word = id >>> 6;
        long mask = 1L << (id & 63);

        if (word < bits.length && (bits[word] & mask) != 0L) {
            return false;
        }
        if (word >= bits.length) {
            bits = Arrays.copyOf(bits, word + 1);
        }
        bits[word] |= mask;

        CompoundTag tag = getTag(stack);
        tag.putLongArray(key, bits);
        saveTag(stack, tag);
        return true;
    }

    /**
     * 读取位图；遇到旧版本留下的字符串列表时，就地转换成位图并落盘。
     */
    private static long[] bitmapOf(ItemStack stack, String key) {
        CompoundTag tag = getTag(stack);
        Tag raw = tag.get(key);

        if (raw instanceof LongArrayTag longArray) {
            return longArray.getAsLongArray();
        }

        if (raw instanceof ListTag list) {
            long[] migrated = migrateLegacyList(key, list);
            tag.remove(key);
            if (migrated.length > 0) {
                tag.putLongArray(key, migrated);
            }
            saveTag(stack, tag);
            return migrated;
        }

        return new long[0];
    }

    /**
     * 把旧格式（字符串 ID 列表）转成位图；无法解析的条目直接丢弃。
     */
    private static long[] migrateLegacyList(String key, ListTag list) {
        IForgeRegistry<?> registry = registryForKey(key);
        if (registry == null) return new long[0];

        long[] bits = new long[0];
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id == null) continue;

            Object value = registry.getValue(id);
            if (value == null) continue;

            int index = idOf(registry, value);
            if (index < 0) continue;

            int word = index >>> 6;
            if (word >= bits.length) {
                bits = Arrays.copyOf(bits, word + 1);
            }
            bits[word] |= (1L << (index & 63));
        }
        return bits;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static int idOf(IForgeRegistry<?> registry, Object value) {
        // IForgeRegistry 接口不暴露数字 ID，实现类 ForgeRegistry 提供 getID
        return ((ForgeRegistry) registry).getID(value);
    }

    private static IForgeRegistry<?> registryForKey(String key) {
        return switch (key) {
            case AYA_KILLED_MOBS -> ForgeRegistries.ENTITY_TYPES;
            case AYA_MINED_BLOCKS -> ForgeRegistries.BLOCKS;
            default -> null;
        };
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
    private static final String AMMO_ITEM = "Item";

    public enum AmmoType {POTION, FIREWORK, YH_DANMAKU, YH_LASER, YH_SPELL}

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

    public static void setYhAmmo(ItemStack stack, ItemStack ammo, AmmoType type) {
        if (ammo.isEmpty()) return;
        CompoundTag tag = getTag(stack);
        CompoundTag data = new CompoundTag();
        data.putString(AMMO_TYPE, type.name().toLowerCase());
        data.put(AMMO_ITEM, ammo.copyWithCount(1).save(new CompoundTag()));
        tag.put(AMMO, data);
        saveTag(stack, tag);
    }

    public static ItemStack getYhAmmo(ItemStack stack) {
        CompoundTag tag = getTag(stack);
        if (!tag.contains(AMMO, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        CompoundTag data = tag.getCompound(AMMO);
        if (!data.contains(AMMO_ITEM, Tag.TAG_COMPOUND)) return ItemStack.EMPTY;
        return ItemStack.of(data.getCompound(AMMO_ITEM));
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
            case "yh_danmaku" -> AmmoType.YH_DANMAKU;
            case "yh_laser" -> AmmoType.YH_LASER;
            case "yh_spell" -> AmmoType.YH_SPELL;
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
