package net.kogasachan.kogasa_addiction.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class KeyInit {

    public static final String KEY_CATEGORY = "key.categories.kogasa_addiction";

    public static final KeyMapping TIMESTOP_KEY = new KeyMapping(
            "key.kogasa_addiction.timestop",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_X,
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(TIMESTOP_KEY);
    }
}