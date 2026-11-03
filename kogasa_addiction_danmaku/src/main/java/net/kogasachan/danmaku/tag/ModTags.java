package net.kogasachan.danmaku.tag;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModTags {

    private ModTags() {
    }

    public static final class Items {

        /** 全部弹幕物品（29 种弹型 + 弹幕发射器） */
        public static final TagKey<Item> DANMAKU_ITEMS = create("danmaku_items");

        private Items() {
        }

        private static TagKey<Item> create(String name) {
            return TagKey.create(Registries.ITEM,
                    ResourceLocation.fromNamespaceAndPath(KogasaDanmaku.MODID, name));
        }
    }
}
