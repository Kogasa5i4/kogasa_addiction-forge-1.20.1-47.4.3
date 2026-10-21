package net.kogasachan.kogasa_addiction.network;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ScreenshotTakenPacket {
    public ScreenshotTakenPacket() {
    }

    public ScreenshotTakenPacket(FriendlyByteBuf buf) {
    }

    public void encode(FriendlyByteBuf buf) {
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            // 1. 冷却检查
            long lastTime = player.getPersistentData().getLong("LastAyaShootTime");
            long now = player.level().getGameTime();
            int cooldown = Config.SHOOT_COOLDOWN_SECONDS.get() * 20;
            if (cooldown > 0 && now - lastTime < cooldown) {
                // 冷却中，静默忽略（不提示，避免刷屏）
                return;
            }

            // 2. 记录尝试时间（无论是否成功都记录，避免冷却期内反复触发）
            player.getPersistentData().putLong("LastAyaShootTime", now);

            // 3. 概率判定
            if (player.getRandom().nextFloat() >= Config.SHOOT_INGOT_CHANCE.get().floatValue()) {
                // 未命中：给个轻微反馈
                player.displayClientMessage(
                        Component.translatable("message.kogasa_addiction.screenshot.miss")
                                .withStyle(ChatFormatting.GRAY),
                        true);
                return;
            }

            // 4. 命中：发放文文锭
            ItemStack ingot = new ItemStack(ModItems.AYA_INGOT.get());
            if (!player.getInventory().add(ingot)) {
                player.drop(ingot, false);
            }

            // 音效 + 提示
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.6F, 1.5F);
            player.displayClientMessage(
                    Component.translatable("message.kogasa_addiction.screenshot.success")
                            .withStyle(ChatFormatting.AQUA),
                    true);
        });
        ctx.get().setPacketHandled(true);
    }
}
