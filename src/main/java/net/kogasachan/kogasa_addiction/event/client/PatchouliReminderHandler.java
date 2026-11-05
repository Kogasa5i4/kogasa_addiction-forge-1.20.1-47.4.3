package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * 客户端没装 Patchouli 时，进入世界后给一条只显示一次的聊天栏提示，附 Modrinth 链接。
 *
 * <p>“只显示一次”用配置目录里的标记文件实现（每个客户端一次，不随世界重置）。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class PatchouliReminderHandler {

    private static final String PATCHOULI_URL = "https://modrinth.com/mod/patchouli";

    private static Path marker() {
        return FMLPaths.CONFIGDIR.get().resolve("kogasa_addiction_patchouli_reminder.txt");
    }

    /** 本次游戏进程是否已经检查过，避免每 tick 重复 */
    private static boolean handledThisSession;

    private PatchouliReminderHandler() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || handledThisSession) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.level == null) {
            return;
        }
        handledThisSession = true;

        if (ModList.get().isLoaded("patchouli")) {
            return;
        }
        if (Files.exists(marker())) {
            return;
        }

        Component link = Component.literal(PATCHOULI_URL).withStyle(Style.EMPTY
                .withColor(ChatFormatting.AQUA)
                .withUnderlined(true)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, PATCHOULI_URL)));

        // false = 聊天栏，而不是动作栏
        player.displayClientMessage(
                Component.translatable("message.kogasa_addiction.patchouli_missing", link), false);

        try {
            Files.writeString(marker(), "shown", StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // 文件写不进去就只影响“只显示一次”，不影响游戏
        }
    }
}
