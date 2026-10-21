package net.kogasachan.kogasa_addiction.datagen;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> KNIVES = TagKey.create(Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath("forge", "tools/knives"));
}
