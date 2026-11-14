package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.util.KogasaIdListMatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 夜雀食堂 / 琪露诺冰屋宝箱加入妖怪们的归家物品。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class YoukaiHomecomingLootHandler {

    private static final String YH = "youkaishomecoming";
    private static final String MYSTIA_POOL = "kogasa_yh_food_bonus";
    private static final String CIRNO_POOL = "kogasa_yh_ice_bonus";
    private static final Set<String> CIRNO_ITEMS = Set.of(
            "fairy_ice_crystal",
            "frozen_frog_cold",
            "frozen_frog_warm",
            "frozen_frog_temperate",
            "cirno_hairband",
            "cirno_wings"
    );

    private YoukaiHomecomingLootHandler() {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event) {
        if (!Config.YH_LOOT_ENABLED.get() || !ModList.get().isLoaded(YH)) return;
        ResourceLocation id = event.getName();
        if (id == null || !KogasaAddiction.MODID.equals(id.getNamespace())) return;

        String path = id.getPath();
        boolean mystia = path.equals("chests/mystia_izakaya_barrel_1");
        boolean cirno = path.equals("chests/cirno_barrel_2");
        if (!mystia && !cirno) return;

        String poolName = mystia ? MYSTIA_POOL : CIRNO_POOL;
        LootTable table = event.getTable();
        if (table.getPool(poolName) != null) return;

        if (mystia) {
            LootPool.Builder pool = LootPool.lootPool()
                    .name(poolName)
                    .setRolls(ConstantValue.exactly(1.0F))
                    .when(LootItemRandomChanceCondition.randomChance(Config.YH_LOOT_CHANCE.get().floatValue()));
            for (Item item : foodItems()) {
                pool.add(LootItem.lootTableItem(item));
            }
            table.addPool(pool.build());
        } else {
            List<WeightedItem> items = cirnoItems();
            if (items.isEmpty()) return;
            LootPool.Builder pool = LootPool.lootPool()
                    .name(poolName)
                    .setRolls(ConstantValue.exactly(1.0F))
                    .when(LootItemRandomChanceCondition.randomChance(Config.YH_LOOT_CHANCE.get().floatValue()));
            for (WeightedItem entry : items) {
                pool.add(LootItem.lootTableItem(entry.item()).setWeight(entry.weight()));
            }
            table.addPool(pool.build());
        }
    }

    private static List<Item> foodItems() {
        List<Item> result = new ArrayList<>();
        for (Item item : ForgeRegistries.ITEMS.getValues()) {
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
            if (id == null || !YH.equals(id.getNamespace())) continue;
            if (KogasaIdListMatcher.matches(id, Config.YH_LOOT_BLACKLIST.get())) continue;
            if (!item.isEdible()) continue;
            result.add(item);
        }
        return result;
    }

    private record WeightedItem(Item item, int weight) {
    }

    private static List<WeightedItem> cirnoItems() {
        List<WeightedItem> result = new ArrayList<>();
        for (String path : CIRNO_ITEMS) {
            ResourceLocation id = ResourceLocation.tryParse(YH + ":" + path);
            if (id == null) continue;
            if (KogasaIdListMatcher.matches(id, Config.YH_LOOT_BLACKLIST.get())) continue;
            Item item = ForgeRegistries.ITEMS.getValue(id);
            if (item == null) continue;
            // hairband / wings 的权重是其他物品的 1/100。
            int weight = path.equals("cirno_hairband") || path.equals("cirno_wings") ? 1 : 100;
            result.add(new WeightedItem(item, weight));
        }
        return result;
    }
}
