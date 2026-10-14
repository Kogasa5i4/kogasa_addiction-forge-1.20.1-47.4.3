package net.kogasachan.kogasa_addiction;

import com.mojang.logging.LogUtils;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(net.kogasachan.kogasa_addiction.KogasaAddiction.MODID)
public class KogasaAddiction {
    // 模组主类
    // modid 注册
    public static final String MODID = "kogasa_addiction";

    public static final Logger LOGGER = LogUtils.getLogger();

    // 模组构造函数
    public KogasaAddiction(FMLJavaModLoadingContext context) {
        // 获取模组事件总线
        IEventBus modEventBus = context.getModEventBus();

        // 主类本身的注册
        MinecraftForge.EVENT_BUS.register(this);

        ModItems.ITEMS.register(modEventBus);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    // 位于模组加载的公共初始化阶段，可用于注册酿造配方、网络包//复杂配方序列化器等
    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(PacketHandler::register);
    }

    // 在创造模式物品栏构建时触发，可用于将物品添加到已有的创造模式标签页
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
    }

    // 在服务器启动时触发，可用于加载服务器专属数据或执行服务器初始化
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    // 客户端专用的事件订阅类
    // 在客户端初始化阶段执行，常用于注册渲染器（如物品模型，特殊方块渲染器）或加载客户端资源
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
        }
    }
}
