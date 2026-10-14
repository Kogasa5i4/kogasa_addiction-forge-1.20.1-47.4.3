package net.kogasachan.kogasa_addiction.event.server;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CirnoIngotExplosionHandler {

    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        List<BlockPos> affected = event.getAffectedBlocks();
        if (affected.isEmpty()) return;

        for (BlockPos pos : affected) {
            BlockState state = serverLevel.getBlockState(pos);
            if (!CirnoIngotDigHandler.isIceBlock(state)) continue;

            if (serverLevel.random.nextFloat() >= Config.CIRNO_INGOT_DROP_CHANCE.get()) continue;

            // 延迟一 tick，避免 ItemEntity 被爆炸摧毁
            ItemStack ingot = new ItemStack(ModItems.CIRNO_INGOT.get());
            serverLevel.getServer().execute(() -> {
                // 再次检查方块是否真的消失了
                if (serverLevel.getBlockState(pos).isAir()) {
                    Block.popResource(serverLevel, pos, ingot);
                }
            });
        }
    }
}