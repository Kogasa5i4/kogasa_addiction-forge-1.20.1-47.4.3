package net.kogasachan.kogasa_addiction.event.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.network.KeyStatePacket;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KeyStateClient {
    private static boolean lastCtrl, lastAlt;

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent e) {
        if (e.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            if (lastCtrl || lastAlt) {
                lastCtrl = lastAlt = false;
                PacketHandler.INSTANCE.sendToServer(new KeyStatePacket(false, false));
            }
            return;
        }
        long window = mc.getWindow().getWindow();
        boolean ctrl = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_CONTROL)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_CONTROL);
        boolean alt = InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT_ALT)
                || InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT_ALT);
        if (ctrl != lastCtrl || alt != lastAlt) {
            lastCtrl = ctrl;
            lastAlt = alt;
            PacketHandler.INSTANCE.sendToServer(new KeyStatePacket(ctrl, alt));
        }
    }
}
