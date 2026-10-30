package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.FumoItem;
import net.kogasachan.kogasa_addiction.network.KeyStateTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FumoSuspiciousBlockHandler {

    // ==================== 右键 ====================

    @SubscribeEvent
    public static void onRightClick(PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (event.getHand() != InteractionHand.MAIN_HAND) return;

        // 只有「无修饰键」的右键才走可疑方块逻辑
        // Shift/Ctrl/Alt 交给 FumoBlockModificationHandler 处理斧/锹/锄
        if (player.isShiftKeyDown()) return;
        if (KeyStateTracker.isCtrlDown(player)) return;
        if (KeyStateTracker.isAltDown(player)) return;

        // 找 Fumo（任意手，优先主手）
        InteractionHand fumoHand;
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        if (main.getItem() instanceof FumoItem) fumoHand = InteractionHand.MAIN_HAND;
        else if (off.getItem() instanceof FumoItem) fumoHand = InteractionHand.OFF_HAND;
        else return;
        ItemStack fumo = player.getItemInHand(fumoHand);

        BlockPos pos = event.getPos();
        BlockState state = player.level().getBlockState(pos);
        if (!isSuspicious(state)) return;
        if (event.getFace() == null) return;
        if (!player.mayUseItemAt(pos, event.getFace(), fumo)) return;

        if (process((ServerLevel) player.level(), pos, player, fumo, fumoHand, state)) {
            player.swing(fumoHand, true);
            event.setCanceled(true);
        }
    }

    // ==================== 破坏 ====================

    @SubscribeEvent
    public static void onBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        BlockState state = event.getState();
        if (!isSuspicious(state)) return;

        // 找 Fumo
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        ItemStack fumo;
        InteractionHand fumoHand;
        if (main.getItem() instanceof FumoItem) {
            fumo = main;
            fumoHand = InteractionHand.MAIN_HAND;
        } else if (off.getItem() instanceof FumoItem) {
            fumo = off;
            fumoHand = InteractionHand.OFF_HAND;
        } else {
            return;
        }

        BlockPos pos = event.getPos();
        // BreakEvent 没有 face，用 UP 做 spawn protection 检查即可
        if (!player.mayUseItemAt(pos, Direction.UP, fumo)) return;

        if (process(level, pos, player, fumo, fumoHand, state)) {
            event.setCanceled(true);
            // 方块本体不按原版掉落；战利品已在上方单独提取，这里只返还普通沙/砂砾。
            level.destroyBlock(pos, false);
            ItemStack drop = new ItemStack(state.is(Blocks.SUSPICIOUS_SAND)
                    ? Items.SAND
                    : Items.GRAVEL);
            Block.popResource(level, pos, drop);
        }
    }

    // ==================== 公共逻辑 ====================

    private static boolean isSuspicious(BlockState state) {
        return state.is(Blocks.SUSPICIOUS_SAND) || state.is(Blocks.SUSPICIOUS_GRAVEL);
    }

    /**
     * 提取可疑方块内的战利品，并把方块替换为普通沙/砂砾。
     *
     * @return 是否成功处理（成功则调用方应 cancel 事件）
     */
    private static boolean process(ServerLevel level, BlockPos pos, ServerPlayer player,
                                   ItemStack fumo, InteractionHand hand, BlockState originalState) {
        boolean isSand = originalState.is(Blocks.SUSPICIOUS_SAND);

        // 1. 提取战利品（仅当 BlockEntity 是 BrushableBlockEntity 且 lootTable 非空）
        if (level.getBlockEntity(pos) instanceof BrushableBlockEntity brushable) {
            ResourceLocation lootTableKey = brushable.lootTable;
            if (lootTableKey != null) {
                LootTable lootTable = level.getServer().getLootData().getLootTable(lootTableKey);
                LootParams params = new LootParams.Builder(level)
                        .withParameter(LootContextParams.BLOCK_STATE, originalState)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, fumo)
                        .withParameter(LootContextParams.THIS_ENTITY, player)
                        .create(LootContextParamSets.BLOCK);

                List<ItemStack> drops = lootTable.getRandomItems(params);
                for (ItemStack drop : drops) {
                    if (!player.getInventory().add(drop)) {
                        player.drop(drop, false);
                    }
                }
            }
        }

        // 2. 方块转换为普通沙/砂砾
        BlockState newState = isSand
                ? Blocks.SAND.defaultBlockState()
                : Blocks.GRAVEL.defaultBlockState();
        level.setBlockAndUpdate(pos, newState);

        // 3. 消耗 1 点耐久（hurtAndBreak 内部已处理创造模式/Unbreaking/损坏）
        EquipmentSlot slot = hand == InteractionHand.MAIN_HAND
                ? EquipmentSlot.MAINHAND
                : EquipmentSlot.OFFHAND;
        fumo.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(slot));

        return true;
    }
}