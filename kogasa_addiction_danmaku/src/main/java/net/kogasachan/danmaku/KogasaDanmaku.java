package net.kogasachan.danmaku;

import net.kogasachan.danmaku.entity.DanmakuEntities;
import net.kogasachan.danmaku.item.DanmakuItems;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Kogasa Addiction: Danmaku —— 以弹幕系统为基础的附属模组。
 *
 * <p>依赖主模组 {@code kogasa_addiction}（mods.toml 强制依赖）。
 * 当前内容：通用弹幕实体（无重力，30 种弹型 × 16 染料色）+ 测试物品 + 弹幕发射器。
 */
@Mod(KogasaDanmaku.MODID)
public class KogasaDanmaku {

    public static final String MODID = "kogasa_addiction_danmaku";
    public static final Logger LOGGER = LoggerFactory.getLogger("KogasaAddictionDanmaku");

    public KogasaDanmaku(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        DanmakuEntities.ENTITY_TYPES.register(modBus);
        DanmakuItems.ITEMS.register(modBus);

        LOGGER.info("Kogasa Addiction: Danmaku loaded");
    }
}
