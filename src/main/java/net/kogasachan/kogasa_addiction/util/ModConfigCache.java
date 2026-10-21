package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModConfigCache {

    public static float kogasaIngotChance = 0.001f;
    public static int growthUnlockReinforcements = 3;
    public static double cirnoIngotDropChance = 0.09;
    public static int kogasaIngotSearchHeight = 8;
    public static float rangedMultishotSpread = 8.0f;

    @SubscribeEvent
    public static void onConfigLoad(ModConfigEvent.Loading event) {
        refresh();
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        refresh();
    }

    private static void refresh() {
        kogasaIngotChance = Config.KOGASA_INGOT_CHANCE.get().floatValue();
        growthUnlockReinforcements = Config.GROWTH_UNLOCK_REINFORCEMENTS.get();
        cirnoIngotDropChance = Config.CIRNO_INGOT_DROP_CHANCE.get();
        kogasaIngotSearchHeight = Config.KOGASA_INGOT_SEARCH_HEIGHT.get();
        rangedMultishotSpread = Config.RANGED_MULTISHOT_SPREAD.get().floatValue();
    }
}
