package net.kogasachan.kogasa_addiction.migration;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.MissingMappingsEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Maps the old Kogasa armor registry names to their new Tenshi names.
 * Forge applies these remaps when loading saved data containing the old registry IDs,
 * allowing ItemStacks (including their NBT, damage, enchantments and growth data) to survive.
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ItemIdMigration {
    private ItemIdMigration() {
    }

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
