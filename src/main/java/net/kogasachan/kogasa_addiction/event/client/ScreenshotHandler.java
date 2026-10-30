package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.kogasachan.kogasa_addiction.network.ScreenshotTakenPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ScreenshotEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ScreenshotHandler {

    @SubscribeEvent
    public static void onScreenshot(ScreenshotEvent event) {
        // 截图内容不做额外校验，由服务端按共享冷却与概率决定是否发放文文锭
        PacketHandler.INSTANCE.sendToServer(new ScreenshotTakenPacket());
    }
}
