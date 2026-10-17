package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class PlayerLoginHandler {

    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        // 只在服务端执行，且确保是 ServerPlayer
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 玩家登录时，遍历背包补发进度（防止旧存档里的装备没获得成就）
        KogasaAdvancementHelper.scanInventoryAndGrant(player);
    }
}