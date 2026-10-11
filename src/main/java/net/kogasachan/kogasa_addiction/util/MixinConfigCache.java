package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class MixinConfigCache {

    public static boolean enableBlastFurnaceIngot = true;
    public static float blastFurnaceIngotChance = 0.005f;
    public static int growthUnlockReinforcements = 3;

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        refresh();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        refresh();
    }

    private static void refresh() {
        enableBlastFurnaceIngot = Config.ENABLE_BLAST_FURNACE_INGOT.get();
        blastFurnaceIngotChance = Config.BLAST_FURNACE_INGOT_CHANCE.get().floatValue();
        growthUnlockReinforcements = Config.GROWTH_UNLOCK_REINFORCEMENTS.get();
    }
}
