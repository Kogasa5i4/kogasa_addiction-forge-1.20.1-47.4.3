
package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.enchanting.EnchantmentLevelSetEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class FumoHandler {

    private static final int COOLDOWN_TICKS = 5 * 60 * 20;
    private static final int POWER_LIMIT = 10;
    private static final int CHANCE_DENOMINATOR = 9961;
    private static final Map<TableKey, int[]> ENCHANT_LEVELS =
            new HashMap<>();

    /**
     * 全局共享冷却。
     * 0 表示当前可以进行概率判定。
     */
    private static int cooldown = 0;

    /**
     * 记录最近检测到的候选附魔台。
     * 按维度和坐标区分，避免不同世界的坐标冲突。
     */
    private static final Map<TableKey, Integer> CANDIDATE_TABLES =
            new HashMap<>();

    private record TableKey(ResourceKey<Level> dimension, BlockPos pos) {}

    @SubscribeEvent
    public static void onEnchantmentLevelSet(
            EnchantmentLevelSetEvent event) {

        if (event.getLevel().isClientSide) {
            return;
        }

        if (event.getPower() >= POWER_LIMIT) {
            return;
        }

        if (!event.getItem().is(Items.NETHERITE_HOE)) {
            return;
        }

        BlockPos pos = event.getPos().immutable();
        TableKey key = new TableKey(event.getLevel().dimension(), pos);

        int[] levels = ENCHANT_LEVELS.computeIfAbsent(
                key, k -> new int[]{-1, -1, -1}
        );

        int row = event.getEnchantRow() - 1;
        if (row < 0 || row >= 3) {
            return;
        }

        levels[row] = event.getEnchantLevel();

        // 三个选项都已计算后，才检查全部等级是否 <= 1。
        if (levels[0] >= 0 && levels[1] >= 0 && levels[2] >= 0
                && levels[0] <= 1
                && levels[1] <= 1
                && levels[2] <= 1) {

            CANDIDATE_TABLES.put(key, event.getPower());
        } else {
            CANDIDATE_TABLES.remove(key);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        // 冷却期间只递减计数，不搜索玩家或附魔台。
        if (cooldown > 0) {
            cooldown--;
            return;
        }

        var server = event.getServer();

        Iterator<Map.Entry<TableKey, Integer>> iterator =
                CANDIDATE_TABLES.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<TableKey, Integer> entry = iterator.next();
            TableKey key = entry.getKey();

            ServerLevel level = server.getLevel(key.dimension());

            if (level == null
                    || !level.hasChunkAt(key.pos())
                    || !level.getBlockState(key.pos())
                    .is(Blocks.ENCHANTING_TABLE)) {
                iterator.remove();
                continue;
            }

            // 仅处理当前确实打开附魔台界面的玩家。
            for (ServerPlayer player : level.players()) {
                if (!(player.containerMenu instanceof EnchantmentMenu menu)) {
                    continue;
                }

                if (player.distanceToSqr(
                        key.pos().getX() + 0.5,
                        key.pos().getY() + 0.5,
                        key.pos().getZ() + 0.5) > 64.0) {
                    continue;
                }

                // 原版附魔台菜单的第 0 号槽位是待附魔物品槽。
                ItemStack input = menu.getSlot(0).getItem();

                if (!input.is(Items.NETHERITE_HOE)) {
                    continue;
                }

                // 额外防止记录的书架力量不符合要求。
                if (entry.getValue() >= POWER_LIMIT) {
                    continue;
                }

                // 只有条件成立时才进行一次概率判定。
                // 无论命中与否，立即进入全局冷却。
                cooldown = COOLDOWN_TICKS;

                if (level.random.nextInt(CHANCE_DENOMINATOR) == 0) {
                    ItemStack fumo = new ItemStack(ModItems.FUMO.get());

                    if (!player.getInventory().add(fumo)) {
                        player.drop(fumo, false);
                    }
                }

                return;
            }
        }
    }
}
