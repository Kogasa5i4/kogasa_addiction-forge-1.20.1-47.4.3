package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.SakuyaKnifeItem;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Map;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 只在服务端执行，且确保是 ServerPlayer
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 1. 玩家登录时，遍历背包补发进度（防止旧存档里的装备没获得成就）
        KogasaAdvancementHelper.scanInventoryAndGrant(player);

        // 2. 清理咲夜飞刀上遗留的激流附魔
        removeRiptideFromKnives(player);
    }

    /**
     * 遍历玩家整个物品栏（主背包+护甲+副手），移除咲夜飞刀上的激流附魔
     */
    private static void removeRiptideFromKnives(ServerPlayer player) {
        boolean changed = false;
        int size = player.getInventory().getContainerSize();

        for (int i = 0; i < size; i++) {
            ItemStack stack = player.getInventory().getItem(i);

            // 只检查咲夜飞刀，并且必须有附魔
            if (stack.getItem() instanceof SakuyaKnifeItem && stack.isEnchanted()) {
                Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);

                // 如果包含激流，则移除
                if (enchants.containsKey(Enchantments.RIPTIDE)) {
                    enchants.remove(Enchantments.RIPTIDE);
                    EnchantmentHelper.setEnchantments(enchants, stack);
                    changed = true;
                }
            }
        }

        // 如果发生了数据修改，通知客户端同步更新（刷新 Tooltip）
        if (changed) {
            player.getInventory().setChanged();
        }
    }
}