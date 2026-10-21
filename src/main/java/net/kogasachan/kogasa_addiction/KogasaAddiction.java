package net.kogasachan.kogasa_addiction;

import com.mojang.logging.LogUtils;
import net.kogasachan.kogasa_addiction.entity.ModEntities;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.recipe.ModRecipes;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(KogasaAddiction.MODID)
public class KogasaAddiction {
    public static final String MODID = "kogasa_addiction";
    public static final String VERSION = "2.6";
    public static final Logger LOGGER = LogUtils.getLogger();

    public KogasaAddiction(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        MinecraftForge.EVENT_BUS.register(this);

        ModItems.ITEMS.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModRecipes.RECIPE_SERIALIZERS.register(modEventBus);

        context.registerConfig(
                ModConfig.Type.COMMON,
                Config.SPEC,
                "kogasa_addiction_" + VERSION + ".toml"
        );
    }
}
