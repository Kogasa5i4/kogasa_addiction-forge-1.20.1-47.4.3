package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

/**
 * 手册补发：
 *
 * <ul>
 *   <li>没装 Patchouli：不发（物品本身仍注册，避免客户端/服务端注册表不一致；创造栏和配方也已经按 mod_loaded 隐藏）；</li>
 *   <li>装了 Patchouli：玩家首次登录（或后来补装后登录）时补发一次，旧玩家已有手册则不重复发。</li>
 * </ul>
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ManualGrantHandler {

    private static final String NBT_KEY = "KogasaManualGranted";

    private ManualGrantHandler() {
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!ModList.get().isLoaded("patchouli")) return;

        CompoundTag data = player.getPersistentData();
        if (data.getBoolean(NBT_KEY)) return;

        // 已经有手册（老玩家 / 从旧 guide_book 迁移过来的）只补标记，不重复发
        if (hasManual(player)) {
            data.putBoolean(NBT_KEY, true);
            return;
        }

        ItemStack manual = new ItemStack(ModItems.KOGASA_MANUAL.get());
        if (!player.getInventory().add(manual)) {
            ItemEntity drop = new ItemEntity(player.level(),
                    player.getX(), player.getY() + 0.5, player.getZ(), manual);
            drop.setPickUpDelay(0);
            player.level().addFreshEntity(drop);
        }
        data.putBoolean(NBT_KEY, true);

        player.displayClientMessage(
                Component.translatable("message.kogasa_addiction.manual.granted")
                        .withStyle(ChatFormatting.LIGHT_PURPLE),
                true);
    }

    private static boolean hasManual(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.KOGASA_MANUAL.get())) {
                return true;
            }
        }
        for (int i = 0; i < player.getEnderChestInventory().getContainerSize(); i++) {
            if (player.getEnderChestInventory().getItem(i).is(ModItems.KOGASA_MANUAL.get())) {
                return true;
            }
        }
        return false;
    }
}
