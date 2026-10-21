package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class SakuyaIngotHandler {

    private static final String NBT_LAST_DAY = "KogasaSakuyaLastDay";

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        MinecraftServer server = event.getServer();
        // 每 100 tick (5秒) 检查一次世界时间
        if (server.getTickCount() % 100 != 0) return;

        // 获取当前主世界的时间（原版中 getDayTime 累计增加，不受睡觉跳过影响）
        long currentDay = server.overworld().getDayTime() / 24000L;

        // 遍历所有在线玩家
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            long lastDay = player.getPersistentData().getLong(NBT_LAST_DAY);

            // 如果是首次进档，只记录，不给奖励
            if (lastDay == 0) {
                player.getPersistentData().putLong(NBT_LAST_DAY, currentDay);
                continue;
            }

            // 如果当前天数大于玩家记录的天数（说明跨天了）
            if (currentDay > lastDay) {
                player.getPersistentData().putLong(NBT_LAST_DAY, currentDay);
                giveReward(player);
            }
        }
    }

    private static void giveReward(ServerPlayer player) {
        int x = (int) Math.floor(player.getX());
        int z = (int) Math.floor(player.getZ());
        int y = (int) Math.floor(player.getY());
        String combined = String.valueOf(Math.abs(x)  + Math.abs(z)  + Math.abs(y));

        int digitSum = 0;
        for (char c : combined.toCharArray()) {
            digitSum += (c - '0');
        }

        if (digitSum % 3 == 0) {
            if (player.getRandom().nextDouble() < Config.SAKUYA_INGOT_CHANCE.get()) {
                ItemStack ingot = new ItemStack(ModItems.SAKUYA_INGOT.get());
                if (!player.getInventory().add(ingot)) {
                    player.drop(ingot, false);
                }
                player.displayClientMessage(Component.translatable("message.kogasa_addiction.sakuya_ingot_found"), true);
            }
        }
    }
}