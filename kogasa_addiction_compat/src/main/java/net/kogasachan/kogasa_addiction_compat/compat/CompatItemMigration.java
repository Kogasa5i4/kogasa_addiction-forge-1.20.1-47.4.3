package net.kogasachan.kogasa_addiction_compat.compat;

import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.world.item.Item;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.MissingMappingsEvent;

/**
 * 旧存档兼容：附属曾经注册过独立的手册物品 {@code kogasa_addiction_compat:compat_manual}，
 * 现在手册并入主模组，旧物品映射到主模组手册 {@code kogasa_addiction:kogasa_manual}。
 */
@Mod.EventBusSubscriber(modid = KogasaCompatAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class CompatItemMigration {

    private CompatItemMigration() {
    }

    @SubscribeEvent
    public static void onMissingMappings(MissingMappingsEvent event) {
        for (MissingMappingsEvent.Mapping<Item> mapping : event.getMappings(ForgeRegistries.Keys.ITEMS, KogasaCompatAddon.MODID)) {
            if ("compat_manual".equals(mapping.getKey().getPath())) {
                mapping.remap(ModItems.KOGASA_MANUAL.get());
            }
        }
    }
}
