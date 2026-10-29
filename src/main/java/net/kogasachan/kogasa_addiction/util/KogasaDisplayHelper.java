package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 物品/药水显示名解析。放在 common 侧，供命令与客户端 Tooltip 共用，
 * 避免服务端代码（/kogasa info）引用只存在于客户端的 Tooltip 处理类。
 */
public final class KogasaDisplayHelper {

    private KogasaDisplayHelper() {
    }

    /**
     * 把 "minecraft:iron_sword" 或 "arrow:minecraft:arrow" 转成当前语言下的显示名。
     */
    public static Component getItemDisplayName(String id) {
        String actualId = id;
        boolean isArrow = false;
        if (id.startsWith("arrow:")) {
            actualId = id.substring("arrow:".length());
            isArrow = true;
        }

        ResourceLocation loc = ResourceLocation.tryParse(actualId);
        if (loc == null) return Component.literal(id);

        var item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) return Component.literal(id);

        Component name = new ItemStack(item).getHoverName();
        if (isArrow) {
            // 显示"16× 箭"而不是"箭"，提示消耗数量
            return Component.translatable(
                    "tooltip.kogasa_addiction.reinforcement.entry.arrow", name);
        }
        return name;
    }

    /**
     * 按弹药类型（普通/喷溅/滞留）构造药水显示名。
     */
    public static Component getPotionDisplayName(ResourceLocation potionId,
                                                 KogasaAbilityData.PotionKind kind) {
        var potion = ForgeRegistries.POTIONS.getValue(potionId);
        if (potion == null) return Component.literal(potionId.toString());

        ItemStack potionStack = switch (kind) {
            case REGULAR -> new ItemStack(Items.POTION);
            case SPLASH -> new ItemStack(Items.SPLASH_POTION);
            case LINGERING -> new ItemStack(Items.LINGERING_POTION);
        };
        PotionUtils.setPotion(potionStack, potion);
        return potionStack.getHoverName();
    }
}
