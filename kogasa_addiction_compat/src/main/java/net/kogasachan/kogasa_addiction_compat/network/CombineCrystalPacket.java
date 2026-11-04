package net.kogasachan.kogasa_addiction_compat.network;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：物品栏里用彩虹水晶右键目标物品槽位。
 *
 * <p>两种模式：
 * <ul>
 *   <li>生存模式：水晶在鼠标携带栏（{@code menu.getCarried()}）里，服务端扣携带栏 1 个；</li>
 *   <li>创造模式：{@link net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen}
 *       的携带栏是客户端独立的，服务端拿不到，因此改成按“背包槽位号”定位目标，
 *       只打标记、<b>不消耗</b>水晶。</li>
 * </ul>
 */
public class CombineCrystalPacket {

    private final int slotIndex;
    private final boolean inventorySlot;

    public CombineCrystalPacket(int slotIndex, boolean inventorySlot) {
        this.slotIndex = slotIndex;
        this.inventorySlot = inventorySlot;
    }

    public CombineCrystalPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readVarInt();
        this.inventorySlot = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeBoolean(inventorySlot);
    }

    public static void handle(CombineCrystalPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleServer(msg, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }

    private static void handleServer(CombineCrystalPacket msg, ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (msg.inventorySlot) {
            handleCreative(player, msg.slotIndex);
        } else {
            handleSurvival(player, msg.slotIndex);
        }
    }

    /** 创造模式：按背包槽位定位目标，不消耗水晶 */
    private static void handleCreative(ServerPlayer player, int inventorySlot) {
        // 只接受创造模式玩家，避免生存模式借此绕过水晶消耗
        if (!player.getAbilities().instabuild) {
            return;
        }
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack target = player.getInventory().getItem(inventorySlot);
        if (!checkTarget(player, target)) {
            return;
        }

        KogasaCompat.markCompatible(target);
        // 容器 id -2 = 直接更新玩家背包槽位
        player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, inventorySlot, target));
        playSuccess(player);
    }

    /** 生存模式：水晶在鼠标携带栏，扣 1 个 */
    private static void handleSurvival(ServerPlayer player, int slotIndex) {
        AbstractContainerMenu menu = player.containerMenu;

        ItemStack carried = menu.getCarried();
        if (carried.isEmpty() || !carried.is(CompatRegistry.RAINBOW_CRYSTAL.get())) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        ItemStack target = menu.getSlot(slotIndex).getItem();
        if (!checkTarget(player, target)) {
            return;
        }

        KogasaCompat.markCompatible(target);
        if (!player.getAbilities().instabuild) {
            carried.shrink(1);
        }
        menu.setCarried(carried);
        // 同时同步槽位变化与鼠标携带物品（内部会 synchronizeCarriedToRemote）
        menu.broadcastChanges();
        playSuccess(player);
    }

    /** 目标物品是否可结合；不可时给玩家提示 */
    private static boolean checkTarget(ServerPlayer player, ItemStack target) {
        if (target.isEmpty()) {
            return false;
        }
        if (target.getItem() instanceof IKogasaEquipment || KogasaCompat.hasMarker(target)) {
            player.displayClientMessage(Component.translatable(
                    "message.kogasa_addiction_compat.already_compatible"), true);
            return false;
        }
        if (!KogasaCompat.isEnabled()) {
            player.displayClientMessage(Component.translatable(
                    "message.kogasa_addiction_compat.compat_disabled"), true);
            return false;
        }
        return true;
    }

    private static void playSuccess(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 1.0F, 1.2F);
        player.level().playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                SoundSource.PLAYERS, 0.6F, 1.4F);
    }
}
