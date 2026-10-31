package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.network.PacketHandler;
import net.kogasachan.kogasa_addiction.network.TenshiBeaconBlockReplacementPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TenshiIngotHandler {

    @SubscribeEvent
    public static void onPlaceBeaconBase(BlockEvent.EntityPlaceEvent event) {
        // 1. 仅在服务端处理，且必须是玩家放置
        if (!(event.getLevel() instanceof Level level) || level.isClientSide) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockPos pos = event.getPos();
        BlockState state = event.getPlacedBlock();

        // 2. 检查 Y 轴高度
        if (pos.getY() < Config.TENSHI_INGOT_MIN_Y.get()) return;

        // 3. 检查方块是否属于原版的信标基座标签（铁块、金块、钻石块、绿宝石块、下界合金块）
        if (!state.is(BlockTags.BEACON_BASE_BLOCKS)) return;

        // 4. 概率判定
        if (level.random.nextDouble() >= Config.TENSHI_INGOT_CHANCE.get()) return;

        // ===== 触发成功，开始拦截 =====

        // 取消本次方块放置，不产生方块掉落
        PacketHandler.INSTANCE.send(
                PacketDistributor.PLAYER.with(() -> player),
                new TenshiBeaconBlockReplacementPacket(pos)
        );
        event.setCanceled(true);

        // 是否消耗被放置的方块：显式控制，不再依赖 Forge 在 BlockItem#place 里的注入点语义。
        // 旧写法完全没有处理这一点，实际结果是"扣不扣"取决于注入点，行为不确定。
        if (Config.TENSHI_INGOT_CONSUME_BLOCK.get() && !player.getAbilities().instabuild) {
            ItemStack used = findPlacedStack(player, state);
            if (!used.isEmpty()) {
                used.shrink(1);
            }
        }

        // 手动播放方块破坏/信标激活的音效，给玩家反馈
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
        level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);

        // 发放天子锭
        ItemStack ingot = new ItemStack(ModItems.TENSHI_INGOT.get());
        if (!player.getInventory().add(ingot)) {
            player.drop(ingot, false);
        }

        // 聊天栏提示
        player.displayClientMessage(
                net.minecraft.network.chat.Component.translatable("message.kogasa_addiction.tenshi_ingot_found")
                        .withStyle(net.minecraft.ChatFormatting.GOLD),
                true
        );
    }

    /**
     * 找出玩家身上用于放置这个方块的那个物品栈（主手优先，其次副手）。
     */
    private static ItemStack findPlacedStack(ServerPlayer player, BlockState placed) {
        for (ItemStack candidate : new ItemStack[]{player.getMainHandItem(), player.getOffhandItem()}) {
            if (candidate.getItem() instanceof net.minecraft.world.item.BlockItem blockItem
                    && blockItem.getBlock() == placed.getBlock()) {
                return candidate;
            }
        }
        return ItemStack.EMPTY;
    }
}