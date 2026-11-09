package net.kogasachan.kogasa_addiction.compat;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

/** Curios 是可选依赖；真正注册放在另一个类里，避免未安装时加载 Curios 类型。 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class KogasaCuriosCompat {

    private KogasaCuriosCompat() {
    }

    @SubscribeEvent
    public static void commonSetup(FMLCommonSetupEvent event) {
        if (!ModList.get().isLoaded("curios")) return;
        try {
            if (!Config.ENABLE_CURIOS_SLOTS.get()) return;
        } catch (RuntimeException ignored) {
            // 配置尚未加载时保持默认开启
        }
        event.enqueueWork(KogasaCuriosRegistration::register);
    }
}
