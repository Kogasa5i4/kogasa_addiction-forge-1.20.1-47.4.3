package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * 额外属性的成长倍率、黑名单与上限。
 */
public final class KogasaAttributeGrowth {

    private KogasaAttributeGrowth() {
    }

    public static double multiplier(ItemStack stack, ResourceLocation attributeId) {
        if (KogasaIdListMatcher.matches(attributeId, Config.ATTRIBUTE_GROWTH_BLACKLIST.get())) {
            return 1.0D;
        }
        return KogasaEquipmentHelper.getTotalMultiplier(stack);
    }

    public static double applyCap(ResourceLocation attributeId, double value) {
        for (String raw : Config.ATTRIBUTE_GROWTH_CAPS.get()) {
            if (raw == null) continue;
            int split = raw.lastIndexOf('=');
            if (split <= 0 || split >= raw.length() - 1) continue;

            String pattern = raw.substring(0, split).trim();
            if (!KogasaIdListMatcher.matches(attributeId, java.util.List.of(pattern))) continue;

            try {
                double cap = Double.parseDouble(raw.substring(split + 1).trim());
                return Math.min(value, cap);
            } catch (NumberFormatException ignored) {
                return value;
            }
        }
        return value;
    }
}
