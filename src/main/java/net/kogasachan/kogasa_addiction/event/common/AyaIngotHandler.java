package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ArrowLooseEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AyaIngotHandler {

    private static final String NBT_COOLDOWN_KEY = "LastAyaShootTime";

    /**
     * 射箭/射弩时概率获得文文锭（shoot 双关）
     */
    @SubscribeEvent
    public static void onArrowLoose(ArrowLooseEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;

        if (!Config.ENABLE_AYA_INGOT.get()) return;

        // 冷却检查：与截图奖励共用同一个时间戳与冷却配置
        long lastTime = serverPlayer.getPersistentData().getLong(NBT_COOLDOWN_KEY);
        long now = serverPlayer.level().getGameTime();
        int cooldown = Config.SHOOT_COOLDOWN_SECONDS.get() * 20;
        if (cooldown > 0 && now - lastTime < cooldown) return;

        // 记录本次尝试（无论是否命中）
        serverPlayer.getPersistentData().putLong(NBT_COOLDOWN_KEY, now);

        // 概率判定
        if (serverPlayer.getRandom().nextFloat() >= Config.SHOOT_INGOT_CHANCE.get().floatValue()) {
            return;  // 未命中静默（射箭本身有音效，再加提示会吵）
        }

        // 发放文文锭
        ItemStack ingot = new ItemStack(ModItems.AYA_INGOT.get());
        if (!serverPlayer.getInventory().add(ingot)) {
            serverPlayer.drop(ingot, false);
        }

        // 音效 + 提示
        serverPlayer.level().playSound(null, serverPlayer.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
        serverPlayer.displayClientMessage(
                Component.translatable("message.kogasa_addiction.screenshot.success")
                        .withStyle(ChatFormatting.AQUA),
                true);
    }
}