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
    /**
     * 创造模式专用：客户端看到的目标物品，服务端槽位为空时用它兜底
     */
    private final ItemStack targetStack;

    public CombineCrystalPacket(int slotIndex, boolean inventorySlot) {
        this(slotIndex, inventorySlot, ItemStack.EMPTY);
    }

    public CombineCrystalPacket(int slotIndex, boolean inventorySlot, ItemStack targetStack) {
        this.slotIndex = slotIndex;
        this.inventorySlot = inventorySlot;
        this.targetStack = targetStack;
    }

    public CombineCrystalPacket(FriendlyByteBuf buf) {
        this.slotIndex = buf.readVarInt();
        this.inventorySlot = buf.readBoolean();
        this.targetStack = buf.readItem();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeVarInt(slotIndex);
        buf.writeBoolean(inventorySlot);
        buf.writeItem(targetStack);
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
            handleCreative(player, msg.slotIndex, msg.targetStack);
        } else {
            handleSurvival(player, msg.slotIndex);
        }
    }

    /**
     * 创造模式：按背包槽位定位目标，不消耗水晶
     */
    private static void handleCreative(ServerPlayer player, int inventorySlot, ItemStack provided) {
        // 只接受创造模式玩家，避免生存模式借此绕过水晶消耗
        if (!player.getAbilities().instabuild) {
            return;
        }
        if (inventorySlot < 0 || inventorySlot >= player.getInventory().getContainerSize()) {
            return;
        }
        // 创造模式里客户端快捷栏可能是“客户端先行”的，服务端槽位有时还是空的；
        // 这时用客户端带上来的物品兜底，保证结合一定生效。
        ItemStack serverStack = player.getInventory().getItem(inventorySlot);
        ItemStack target = serverStack.isEmpty() ? provided.copy() : serverStack;
        if (!checkTarget(player, target)) {
            return;
        }

        KogasaCompat.markCompatible(target);
        player.getInventory().setItem(inventorySlot, target);
        // 容器 id -2 = 直接更新玩家背包槽位
        player.connection.send(new ClientboundContainerSetSlotPacket(-2, 0, inventorySlot, target));
        playSuccess(player);
    }

    /**
     * 生存模式：水晶在鼠标携带栏，扣 1 个
     */
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

    /**
     * 目标物品是否可结合；不可时给玩家提示
     */
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
        if (!KogasaCompat.canCombine(target)) {
            player.displayClientMessage(Component.translatable(
                    "message.kogasa_addiction_compat.not_compatible"), true);
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
