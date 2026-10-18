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
        // 可选：在这里检查截图内容（event.getImage()），决定是否"有效"

        // 发送网络包到服务端，请求发放"文文锭"
        PacketHandler.INSTANCE.sendToServer(new ScreenshotTakenPacket());
    }
}
