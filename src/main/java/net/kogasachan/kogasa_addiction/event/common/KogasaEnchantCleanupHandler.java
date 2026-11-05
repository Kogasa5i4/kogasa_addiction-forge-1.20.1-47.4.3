package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.HashMap;
import java.util.Map;

/**
 * 模式 1（绝对黑名单）的存量清理：玩家进入存档时，把背包（含护甲/副手）与末影箱里
 * 已经存在的黑名单附魔删掉。
 *
 * <p>只处理玩家自己的容器；世界里的箱子/掉落物不动，避免登录时扫描全服。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KogasaEnchantCleanupHandler {

    private KogasaEnchantCleanupHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        cleanContainer(player.getInventory());
        cleanContainer(player.getEnderChestInventory());
    }

    private static void cleanContainer(Container container) {
        boolean changed = false;
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (stack.isEmpty()) continue;

            Map<Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(stack);
            if (enchantments.isEmpty()) continue;

            Map<Enchantment, Integer> cleaned = new HashMap<>(enchantments);
            for (Enchantment enchantment : enchantments.keySet()) {
                ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(enchantment);
                if (KogasaGrowthHandler.isBlacklisted(id)) {
                    cleaned.remove(enchantment);
                }
            }

            if (cleaned.size() != enchantments.size()) {
                EnchantmentHelper.setEnchantments(cleaned, stack);
                container.setItem(i, stack);
                changed = true;
            }
        }
        if (changed) {
            container.setChanged();
        }
    }
}
