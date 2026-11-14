package net.kogasachan.kogasa_addiction_compat.compat;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * 兼容层使用的物品标签。数据包作者可以随时通过 {@code /reload} 增删。
 */
public final class CompatTags {

    /**
     * 允许吃兼容机制的工具/武器/护甲；默认含原版工具武器与护甲标签
     */
    public static final TagKey<Item> COMPATIBLE = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(KogasaCompatAddon.MODID, "compatible"));

    private CompatTags() {
    }
}
