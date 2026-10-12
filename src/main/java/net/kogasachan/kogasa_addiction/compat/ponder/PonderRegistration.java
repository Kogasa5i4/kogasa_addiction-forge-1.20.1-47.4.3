package net.kogasachan.kogasa_addiction.compat.ponder;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class PonderRegistration {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (!ModList.get().isLoaded("ponder")) return;
        event.enqueueWork(() -> {
            try {
                // 反射加载，不直接引用 Ponder 类
                Class<?> clazz = Class.forName(
                        "net.kogasachan.kogasa_addiction.compat.ponder.KogasaPonderRegistrar");
                clazz.getMethod("register").invoke(null);
            } catch (Throwable t) {
                KogasaAddiction.LOGGER.error("Failed to register Ponder plugin", t);
            }
        });
    }
}
