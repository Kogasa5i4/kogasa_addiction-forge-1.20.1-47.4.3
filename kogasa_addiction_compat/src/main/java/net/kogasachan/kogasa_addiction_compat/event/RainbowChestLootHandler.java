package net.kogasachan.kogasa_addiction_compat.event;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.kogasachan.kogasa_addiction_compat.compat.CompatConfig;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.CompositeEntryBase;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.lang.reflect.Field;
import java.util.List;

/** 给所有含钻石的原版宝箱表追加一次 50% 彩虹水晶奖励。 */
@Mod.EventBusSubscriber(modid = KogasaCompatAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RainbowChestLootHandler {

    private static final String POOL_NAME = "kogasa_rainbow_crystal_bonus";
    private static Field poolsField;
    private static Field entriesField;
    private static Field itemField;
    private static Field childrenField;

    private RainbowChestLootHandler() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!CompatConfig.RAINBOW_CHEST_LOOT_ENABLED.get()) return;
        ResourceLocation id = event.getName();
        if (id == null || !"minecraft".equals(id.getNamespace()) || !id.getPath().startsWith("chests/")) {
            return;
        }

        LootTable table = event.getTable();
        if (table.getPool(POOL_NAME) != null || !containsDiamond(table)) return;

        float chance = CompatConfig.RAINBOW_CHEST_LOOT_CHANCE.get().floatValue();
        if (chance <= 0.0F) return;

        table.addPool(LootPool.lootPool()
                .name(POOL_NAME)
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(LootItem.lootTableItem(CompatRegistry.RAINBOW_CRYSTAL.get()))
                .build());
    }

    private static boolean containsDiamond(LootTable table) {
        try {
            if (poolsField == null) {
                poolsField = LootTable.class.getDeclaredField("pools");
                poolsField.setAccessible(true);
            }
            List<?> pools = (List<?>) poolsField.get(table);
            for (Object pool : pools) {
                if (containsDiamondInPool(pool, 0)) return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // 反射失败时宁可不改表，也不要把所有宝箱都塞满。
        }
        return false;
    }

    private static boolean containsDiamondInPool(Object pool, int depth) {
        if (pool == null || depth > 8) return false;
        try {
            if (entriesField == null) {
                entriesField = pool.getClass().getDeclaredField("entries");
                entriesField.setAccessible(true);
            }
            Object value = entriesField.get(pool);
            if (!(value instanceof Object[] entries)) return false;
            for (Object entry : entries) {
                if (containsDiamondInEntry(entry, depth + 1)) return true;
            }
        } catch (ReflectiveOperationException | RuntimeException ignored) {
            // ignore
        }
        return false;
    }

    private static boolean containsDiamondInEntry(Object entry, int depth) {
        if (entry == null || depth > 8) return false;
        if (entry instanceof LootItem) {
            try {
                if (itemField == null) {
                    itemField = LootItem.class.getDeclaredField("item");
                    itemField.setAccessible(true);
                }
                return itemField.get(entry) == Items.DIAMOND;
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                return false;
            }
        }
        if (entry instanceof CompositeEntryBase) {
            try {
                if (childrenField == null) {
                    childrenField = CompositeEntryBase.class.getDeclaredField("children");
                    childrenField.setAccessible(true);
                }
                Object value = childrenField.get(entry);
                if (value instanceof LootPoolEntryContainer[] children) {
                    for (LootPoolEntryContainer child : children) {
                        if (containsDiamondInEntry(child, depth + 1)) return true;
                    }
                }
            } catch (ReflectiveOperationException | RuntimeException ignored) {
                // ignore
            }
        }
        return false;
    }
}
