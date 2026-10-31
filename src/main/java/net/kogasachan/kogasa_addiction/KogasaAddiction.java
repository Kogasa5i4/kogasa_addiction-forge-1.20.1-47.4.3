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

    /**
     * 配置文件版本号，**与模组版本无关**：它只决定配置文件名（kogasa_addiction_2.6.toml）。
     * 改动它会让所有玩家生成一份新的配置文件（旧配置里的调整看起来"被重置"），
     * 因此只有在配置项发生不兼容变更时才应该 +1。
     */
    public static final String CONFIG_VERSION = "2.6";

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
                "kogasa_addiction_" + CONFIG_VERSION + ".toml"
        );
    }
}
