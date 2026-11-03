package net.kogasachan.danmaku.item;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public final class DanmakuItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KogasaDanmaku.MODID);

    /** 每种弹幕对应的手持物品（顺序与 {@link DanmakuType#values()} 一致） */
    public static final List<RegistryObject<Item>> BULLET_ITEMS = new ArrayList<>();

    static {
        for (DanmakuType type : DanmakuType.values()) {
            BULLET_ITEMS.add(ITEMS.register("danmaku_" + type.id(),
                    () -> new DanmakuBulletItem(type, new Item.Properties())));
        }
    }

    /** 弹幕发射器：测试用，可切换弹型/颜色 */
    public static final RegistryObject<Item> DANMAKU_WAND = ITEMS.register("danmaku_wand",
            () -> new DanmakuWandItem(new Item.Properties().stacksTo(1)));

    private DanmakuItems() {
    }
}
