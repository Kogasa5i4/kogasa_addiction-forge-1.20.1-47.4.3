package net.kogasachan.kogasa_addiction.migration;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * 将旧的 Kogasa 护甲注册名迁移到新的天子护甲注册名。
 * Forge 在加载包含旧注册 ID 的存档数据时会应用这些重映射，
 * 从而保留 ItemStack 的 NBT、耐久、附魔与成长数据。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemIdMigration {
    private ItemIdMigration() {
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, KogasaAddiction.MODID)) {
            ResourceLocation oldId = mapping.getKey();
            if (!KogasaAddiction.MODID.equals(oldId.getNamespace())) {
                continue;
            }

            switch (oldId.getPath()) {
                case "kogasa_helmet" -> mapping.remap(ModItems.TENSHI_HELMET.get());
                case "kogasa_chestplate" -> mapping.remap(ModItems.TENSHI_CHESTPLATE.get());
                case "kogasa_leggings" -> mapping.remap(ModItems.TENSHI_LEGGINGS.get());
                case "kogasa_boots" -> mapping.remap(ModItems.TENSHI_BOOTS.get());
                default -> {
                    // No migration registered for this ID.
                }
            }
        }
    }
}
