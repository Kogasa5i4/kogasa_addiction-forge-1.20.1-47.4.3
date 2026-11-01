package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.TradeWithVillagerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * "被封印的众神"隐藏进度的获取时发放（事件驱动，零周期开销）：
 * <ul>
 *   <li>捡起掉落物（怪物掉落的封印装备）；</li>
 *   <li>与村民/流浪商人完成交易（含随机封印装备交易）。</li>
 * </ul>
 * 铁砧木棍封印的取件在 {@code AnvilMenuMixin} 里发放；
 * 战利品箱取出的装备由 {@link KogasaAdvancementHelper#scanInventoryAndGrant}（登录/快捷取出）
 * 与首次使用时 {@code checkAndGrant} 兜底。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SealedEquipmentGrantHandler {

    private SealedEquipmentGrantHandler() {
    }

    @SubscribeEvent
    public static void onItemPickup(EntityItemPickupEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack stack = event.getItem().getItem();
        if (!stack.isEmpty() && KogasaEquipmentData.isLocked(stack)) {
            KogasaAdvancementHelper.grantSealedEquipment(player);
        }
    }

    @SubscribeEvent
    public static void onVillagerTrade(TradeWithVillagerEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ItemStack result = event.getMerchantOffer().getResult();
        if (!result.isEmpty() && KogasaEquipmentData.isLocked(result)) {
            KogasaAdvancementHelper.grantSealedEquipment(player);
        }
    }
}
