package net.kogasachan.kogasa_addiction.event.server;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class CreativeTabHandler {

    @SubscribeEvent
    public static void onBuildTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.KOGASA_INGOT);
            event.accept(ModItems.AYA_INGOT);
            event.accept(ModItems.CIRNO_INGOT);
            event.accept(ModItems.KOISHI_INGOT);
        }
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(ModItems.KOGASA_SWORD);
            event.accept(ModItems.KOGASA_AXE);
            event.accept(ModItems.KOGASA_HELMET);
            event.accept(ModItems.KOGASA_CHESTPLATE);
            event.accept(ModItems.KOGASA_LEGGINGS);
            event.accept(ModItems.KOGASA_BOOTS);
        }
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModItems.KOGASA_PICKAXE);
            event.accept(ModItems.KOGASA_AXE);
            event.accept(ModItems.KOGASA_SHOVEL);
            event.accept(ModItems.KOGASA_HOE);
        }
    }
}