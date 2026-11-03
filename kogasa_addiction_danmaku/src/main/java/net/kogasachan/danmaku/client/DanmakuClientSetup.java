package net.kogasachan.danmaku.client;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.kogasachan.danmaku.entity.DanmakuEntities;
import net.kogasachan.danmaku.item.DanmakuBulletItem;
import net.kogasachan.danmaku.item.DanmakuItems;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = KogasaDanmaku.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class DanmakuClientSetup {

    private DanmakuClientSetup() {
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(DanmakuEntities.DANMAKU_BULLET.get(), DanmakuBulletRenderer::new);
        event.registerEntityRenderer(DanmakuEntities.DANMAKU_BEAM.get(), DanmakuBeamRenderer::new);
    }

    /**
     * 29 种弹幕物品共用一张白色图标贴图，按各自当前颜色做 tint，
     * 这样不用为每种弹幕额外画图标。
     */
    @SubscribeEvent
    public static void onRegisterItemColors(RegisterColorHandlersEvent.Item event) {
        for (RegistryObject<Item> entry : DanmakuItems.BULLET_ITEMS) {
            Item item = entry.get();
            if (!(item instanceof DanmakuBulletItem)) {
                continue;
            }
            event.register((stack, tintIndex) ->
                    tintIndex == 0 ? DanmakuBulletItem.colorOf(stack).rgb() : -1, item);
        }
    }
}
