package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class TenshiIngotHandler {
    private TenshiIngotHandler() {}

    @SubscribeEvent
    public static void onBeaconBasePlaced(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel level)) return;

        BlockPos pos = event.getPos();
        if (pos.getY() < Config.TENSHI_INGOT_HEIGHT.get()) return;
        BlockState state = event.getPlacedBlock();
        // Minecraft 1.20.1 provides the vanilla beacon_base_blocks tag; this also supports compatible tagged blocks.
        if (!state.is(BlockTags.BEACON_BASE_BLOCKS)) return;
        if (player.getRandom().nextDouble() >= Config.TENSHI_INGOT_CHANCE.get()) return;

        // Consume the newly placed block without drops and reward one Tenshi Ingot.
        level.destroyBlock(pos, false);
        ItemStack reward = new ItemStack(ModItems.TENSHI_INGOT.get());
        if (!player.getInventory().add(reward)) {
            player.drop(reward, false);
        }
    }
}
