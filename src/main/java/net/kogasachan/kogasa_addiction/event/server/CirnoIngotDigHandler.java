package net.kogasachan.kogasa_addiction.event.server;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class CirnoIngotDigHandler {

    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        BlockState state = event.getState();
        if (!isIceBlock(state)) return;

        Player player = event.getPlayer();
        if (player == null) return;

        // 精准采集 → 掉落冰本体，不给锭
        ItemStack tool = player.getMainHandItem();
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.SILK_TOUCH, tool) > 0) {
            return;
        }

        // 9% 掉落
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) return;
        if (serverLevel.random.nextFloat() < Config.CIRNO_INGOT_DROP_CHANCE.get()) {
            Block.popResource(serverLevel, event.getPos(),
                    new ItemStack(ModItems.CIRNO_INGOT.get()));
        }
    }

    /**
     * ！？冰冰冰？！
     */
    public static boolean isIceBlock(BlockState state) {
        var id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;
        String path = id.getPath();
        return path.equals("ice") || path.endsWith("_ice");
    }
}