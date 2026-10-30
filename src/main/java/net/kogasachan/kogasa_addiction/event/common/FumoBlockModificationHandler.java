package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.FumoItem;
import net.kogasachan.kogasa_addiction.network.KeyStateTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FumoBlockModificationHandler {

    /**
     * 斧：先去蜡，再刮削，最后去皮
     */
    private static final List<ToolAction> AXE_ACTIONS = List.of(
            ToolActions.AXE_WAX_OFF,
            ToolActions.AXE_SCRAPE,
            ToolActions.AXE_STRIP
    );

    /**
     * 锹：压土径
     */
    private static final List<ToolAction> SHOVEL_ACTIONS = List.of(
            ToolActions.SHOVEL_FLATTEN
    );

    /**
     * 锄：耕地
     */
    private static final List<ToolAction> HOE_ACTIONS = List.of(
            ToolActions.HOE_TILL
    );

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        // 1. 找 Fumo（任意手，优先主手）
        InteractionHand fumoHand;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.getItem() instanceof FumoItem) fumoHand = InteractionHand.MAIN_HAND;
        else if (off.getItem() instanceof FumoItem) fumoHand = InteractionHand.OFF_HAND;
        else return;
        ItemStack fumo = player.getItemInHand(fumoHand);
        if (fumo.isDamageableItem() && fumo.getDamageValue() >= fumo.getMaxDamage()) return;

        // 2. 根据修饰键选动作（互斥，优先级 Shift > Ctrl > Alt）
        List<ToolAction> actions;
        if (player.isShiftKeyDown()) {
            actions = AXE_ACTIONS;
        } else if (KeyStateTracker.isCtrlDown(player)) {
            actions = SHOVEL_ACTIONS;
        } else if (KeyStateTracker.isAltDown(player)) {
            actions = HOE_ACTIONS;
        } else {
            return;
        }

        // 3. 公共逻辑
        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);
        if (state.isAir()) return;
        Direction face = Direction.UP;
        if (event.getFace() != null) face = event.getFace();
        if (!player.mayUseItemAt(pos, face, fumo)) return;

        BlockHitResult hit = new BlockHitResult(event.getHitVec().getLocation(), face, pos, false);
        UseOnContext context = new UseOnContext(player, fumoHand, hit);

        for (ToolAction action : actions) {
            BlockState newState = state.getToolModifiedState(context, action, true);
            if (newState == null || newState == state) continue;

            player.level().gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, newState));
            player.level().setBlock(pos, newState, 11);

            playFeedback(player, pos, action);

            EquipmentSlot slot = fumoHand == InteractionHand.MAIN_HAND
                    ? EquipmentSlot.MAINHAND
                    : EquipmentSlot.OFFHAND;
            fumo.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(slot));

            player.swing(fumoHand, true);
            event.setCanceled(true);
            break;
        }
    }

    private static SoundEvent resolveSound(ToolAction action) {
        if (action == ToolActions.AXE_STRIP) return SoundEvents.AXE_STRIP;
        if (action == ToolActions.AXE_SCRAPE) return SoundEvents.AXE_SCRAPE;
        if (action == ToolActions.AXE_WAX_OFF) return SoundEvents.AXE_WAX_OFF;
        if (action == ToolActions.SHOVEL_FLATTEN) return SoundEvents.SHOVEL_FLATTEN;
        if (action == ToolActions.HOE_TILL) return SoundEvents.HOE_TILL;
        return null;
    }

    private static void playFeedback(ServerPlayer player, BlockPos pos, ToolAction action) {
        Level level = player.level();

        SoundEvent sound = resolveSound(action);
        if (sound != null) {
            // 传 null：纯服务端实现，触发者也要能听到
            level.playSound(null, pos, sound, SoundSource.BLOCKS, 1.0F, 1.0F);
        }

        // 只有除锈和去蜡有原版粒子
        if (action == ToolActions.AXE_SCRAPE) {
            // LevelEvent.PARTICLES_SCRAPE = 3005
            level.levelEvent(null, LevelEvent.PARTICLES_SCRAPE, pos, 0);
        } else if (action == ToolActions.AXE_WAX_OFF) {
            // LevelEvent.PARTICLES_WAX_OFF = 3004
            level.levelEvent(null, LevelEvent.PARTICLES_WAX_OFF, pos, 0);
        }
        // AXE_STRIP / SHOVEL_FLATTEN / HOE_TILL 原版无粒子，不处理
    }
}