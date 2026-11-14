package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.loot.KogasaLockedEquipmentFunction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraftforge.event.village.VillagerTradesEvent;
import net.minecraftforge.event.village.WandererTradesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * 村民 / 流浪商人交易：锭 + 绿宝石 → 对应能力装备（职业限定装备类型），
 * 流浪商人另有"随机锭换随机封印装备"与"绿宝石换随机可酿造药水文文箭"。
 *
 * <p>大师级（5 级）村民交易使用能力锭作为副货币，把锭循环接进村民经济；
 * 绿宝石价格受 {@code villagerTrades.priceMultiplier} 全局倍率影响。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class VillagerTradeHandler {

    private VillagerTradeHandler() {
    }

    /*
      交易商品池只存 RegistryObject：若在静态初始化里调用 {@code get()}，
      模组构造阶段物品尚未注册会抛 "Registry Object not present"
      （datagen/启动都会先触发本类的订阅者扫描）。解析延迟到 getOffer。
     */
    /**
     * 盔甲匠：天子护甲四件
     */
    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] ARMOR_POOL = new RegistryObject[]{
            ModItems.TENSHI_HELMET,
            ModItems.TENSHI_CHESTPLATE,
            ModItems.TENSHI_LEGGINGS,
            ModItems.TENSHI_BOOTS
    };
    /**
     * 武器匠：剑、斧
     */
    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] SWORD_AXE_POOL = new RegistryObject[]{
            ModItems.KOGASA_SWORD,
            ModItems.KOGASA_AXE
    };
    /**
     * 工具匠：镐、斧、锹、锄
     */
    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] TOOL_POOL = new RegistryObject[]{
            ModItems.KOGASA_PICKAXE,
            ModItems.KOGASA_AXE,
            ModItems.KOGASA_SHOVEL,
            ModItems.KOGASA_HOE
    };
    /**
     * 制箭师：弓、弩
     */
    @SuppressWarnings("unchecked")
    private static final RegistryObject<Item>[] BOW_CROSSBOW_POOL = new RegistryObject[]{
            ModItems.AYA_BOW,
            ModItems.AYA_CROSSBOW
    };

    @SubscribeEvent
    public static void onVillagerTrades(VillagerTradesEvent event) {
        if (!Config.VILLAGER_TRADES_ENABLED.get()) return;

        if (event.getType() == VillagerProfession.ARMORER) {
            trades(event, 5).add(new IngotForAbilityEquipment(
                    ModItems.TENSHI_INGOT.get(), 12, ARMOR_POOL,
                    KogasaAbility.TENSHI, 3, 30));
        } else if (event.getType() == VillagerProfession.WEAPONSMITH) {
            trades(event, 5).add(new IngotForAbilityEquipment(
                    ModItems.KOGASA_INGOT.get(), 10, SWORD_AXE_POOL,
                    KogasaAbility.KOGASA, 3, 30));
        } else if (event.getType() == VillagerProfession.TOOLSMITH) {
            trades(event, 5).add(new IngotForAbilityEquipment(
                    ModItems.KOGASA_INGOT.get(), 8, TOOL_POOL,
                    KogasaAbility.KOGASA, 3, 30));
        } else if (event.getType() == VillagerProfession.FLETCHER) {
            trades(event, 5).add(new IngotForAbilityEquipment(
                    ModItems.AYA_INGOT.get(), 10, BOW_CROSSBOW_POOL,
                    KogasaAbility.AYA, 3, 30));
            // 文文箭：无能力，量大管饱
            trades(event, 2).add(new EmeraldForAyaArrows(18, 32, 12, 10));
        }
    }

    @SubscribeEvent
    public static void onWandererTrades(WandererTradesEvent event) {
        if (!Config.VILLAGER_TRADES_ENABLED.get()) return;

        // 少量绿宝石 + 随机锭 → 随机封印装备（能力随机，不一定对应）
        event.getRareTrades().add(new IngotAndEmeraldForSealedEquipment(4, 3, 20));
        // 绿宝石 → 随机可酿造原版药水文文箭（排除吸收/幸运等非酿造药水）
        event.getGenericTrades().add(new EmeraldForRandomTippedAyaArrow(1, 8, 4, 10));
    }

    private static List<VillagerTrades.ItemListing> trades(VillagerTradesEvent event, int level) {
        return event.getTrades().computeIfAbsent(level, k -> new ArrayList<>());
    }

    /**
     * 读取配置价格倍率并换算绿宝石数量（最少 1 个）。
     */
    private static int emeralds(int base) {
        return Math.max(1, (int) Math.round(base * Config.VILLAGER_TRADE_PRICE_MULTIPLIER.get()));
    }

    // ==================== 交易实现 ====================

    /**
     * 锭 + 中量绿宝石 → 随机装备（职业限定）+ 对应能力
     */
    private record IngotForAbilityEquipment(Item ingot, int baseEmeralds,
                                            RegistryObject<Item>[] pool,
                                            KogasaAbility ability, int maxUses, int xp)
            implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, RandomSource random) {
            ItemStack equipment = new ItemStack(pool[random.nextInt(pool.length)].get());
            KogasaAbilityData.forceAddAbility(equipment, ability);
            return new MerchantOffer(
                    new ItemStack(ingot),
                    new ItemStack(Items.EMERALD, emeralds(baseEmeralds)),
                    equipment, maxUses, xp, 0.05F);
        }
    }

    /**
     * 绿宝石 → 文文箭若干（无能力，量大）
     */
    private record EmeraldForAyaArrows(int baseEmeralds, int count, int maxUses, int xp)
            implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, @NotNull RandomSource random) {
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeralds(baseEmeralds)),
                    ItemStack.EMPTY,
                    new ItemStack(ModItems.AYA_ARROW.get(), count), maxUses, xp, 0.05F);
        }
    }

    /**
     * 少量绿宝石 + 随机锭 → 随机封印装备（能力随机，不一定对应）
     */
    private record IngotAndEmeraldForSealedEquipment(int baseEmeralds, int maxUses, int xp)
            implements VillagerTrades.ItemListing {

        @SuppressWarnings("unchecked")
        private static final RegistryObject<Item>[] INGOTS = new RegistryObject[]{
                ModItems.KOGASA_INGOT,
                ModItems.AYA_INGOT,
                ModItems.CIRNO_INGOT,
                ModItems.KOISHI_INGOT,
                ModItems.TENSHI_INGOT,
                ModItems.SAKUYA_INGOT
        };

        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, RandomSource random) {
            Item ingot = INGOTS[random.nextInt(INGOTS.length)].get();
            ItemStack result = KogasaLockedEquipmentFunction.createLockedEquipment(random);
            return new MerchantOffer(
                    new ItemStack(ingot),
                    new ItemStack(Items.EMERALD, emeralds(baseEmeralds)),
                    result, maxUses, xp, 0.05F);
        }
    }

    /**
     * 绿宝石 → 随机"可酿造"原版药水文文箭（排除吸收/幸运等非酿造药水与无效果药水）
     */
    private record EmeraldForRandomTippedAyaArrow(int baseEmeralds, int count, int maxUses, int xp)
            implements VillagerTrades.ItemListing {

        @Override
        public MerchantOffer getOffer(@NotNull Entity trader, @NotNull RandomSource random) {
            List<Potion> candidates = new ArrayList<>();
            for (Potion potion : ForgeRegistries.POTIONS) {
                ResourceLocation id = ForgeRegistries.POTIONS.getKey(potion);
                if (id == null || !"minecraft".equals(id.getNamespace())) continue;
                if (potion.getEffects().isEmpty()) continue; // 水/平凡的/浓稠的/粗制的等无效果
                candidates.add(potion);
            }

            ItemStack arrows = new ItemStack(ModItems.AYA_ARROW.get(), count);
            if (!candidates.isEmpty()) {
                PotionUtils.setPotion(arrows, candidates.get(random.nextInt(candidates.size())));
            }
            return new MerchantOffer(
                    new ItemStack(Items.EMERALD, emeralds(baseEmeralds)),
                    ItemStack.EMPTY,
                    arrows, maxUses, xp, 0.05F);
        }
    }
}
