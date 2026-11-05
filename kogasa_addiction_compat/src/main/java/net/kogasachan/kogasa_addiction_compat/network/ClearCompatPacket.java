package net.kogasachan.kogasa_addiction_compat.network;

import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundContainerSetSlotPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 客户端 → 服务端：用光标上的熔岩桶右键非本模组物品，消耗熔岩并删除本模组添加的 NBT，
 * 让物品恢复原始状态。
 */
public class ClearCompatPacket {

    private final int slotIndex;
    private final boolean inventorySlot;
    private final ItemStack targetStack;

    public ClearCompatPacket(int slotIndex, boolean inventorySlot) {
        this(slotIndex, inventorySlot, ItemStack.EMPTY);
    }

    public ClearCompatPacket(int slotIndex, boolean inventorySlot, ItemStack targetStack) {
        this.slotIndex = slotIndex;
        this.inventorySlot = inventorySlot;
        this.targetStack = targetStack;
    }

    public ClearCompatPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readVarInt();
        this.inventorySlot = buf.readBoolean();
        this.targetStack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeBoolean(inventorySlot);
        buf.writeItem(targetStack);
    }

    public static void handle(ClearCompatPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> handleServer(msg, ctx.get().getSender()));
        ctx.get().setPacketHandled(true);
    }

    private static void handleServer(ClearCompatPacket msg, ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (msg.inventorySlot) {
            handleCreative(player, msg.slotIndex, msg.targetStack);
        } else {
            handleSurvival(player, msg.slotIndex);
        }
    }

    private static void handleCreative(ServerPlayer player, int inventorySlot, ItemStack provided) {
        if (!player.getAbilities().instabuild) {
            return;
        }
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) {
            return;
        }
        ItemStack serverStack = player.getInventory().getItem(inventorySlot);
        ItemStack target = serverStack.isEmpty() ? provided.copy() : serverStack;
        if (!canRevert(target)) {
            return;
        }

        KogasaCompat.clearAddedData(target);
        player.getInventory().setItem(inventorySlot, target);
        player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, inventorySlot, target));
        success(player);
    }

    private static void handleSurvival(ServerPlayer player, int slotIndex) {
        AbstractContainerMenu menu = player.containerMenu;
        ItemStack carried = menu.getCarried();
        if (!carried.is(Items.LAVA_BUCKET)) {
            return;
        }
        if (slotIndex < 0 || slotIndex >= menu.slots.size()) {
            return;
        }
        ItemStack target = menu.getSlot(slotIndex).getItem();
        if (!canRevert(target)) {
            return;
        }

        KogasaCompat.clearAddedData(target);
        // 熔岩被消耗，光标变成空桶
        menu.setCarried(new ItemStack(Items.BUCKET));
        menu.broadcastChanges();
        success(player);
    }

    private static boolean canRevert(ItemStack target) {
        return !target.isEmpty()
                && !(target.getItem() instanceof IKogasaEquipment)
                && KogasaCompat.hasKogasaData(target);
    }

    private static void success(ServerPlayer player) {
        player.displayClientMessage(
                Component.translatable("message.kogasa_addiction_compat.reverted"), true);
        player.level().playSound(null, player.blockPosition(),
                SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.8F, 1.2F);
    }
}
