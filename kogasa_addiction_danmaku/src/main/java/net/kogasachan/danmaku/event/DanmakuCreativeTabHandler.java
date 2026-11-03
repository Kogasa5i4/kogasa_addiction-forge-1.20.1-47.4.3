package net.kogasachan.danmaku.event;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.kogasachan.danmaku.item.DanmakuItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.RegistryObject;

/**
 * 弹幕物品挂到原版"战斗用品"创造分类（不新建分类，避免和主模组分类冲突）。
 */
@Mod.EventBusSubscriber(modid = KogasaDanmaku.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class DanmakuCreativeTabHandler {

    private DanmakuCreativeTabHandler() {
    }

    @SubscribeEvent
    public static void onBuildTabContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.COMBAT) {
            return;
        }
        for (RegistryObject<Item> bullet : DanmakuItems.BULLET_ITEMS) {
            event.accept(bullet.get());
        }
        event.accept(DanmakuItems.DANMAKU_WAND.get());
    }
}
