package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.entity.player.PlayerXpEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 经验修补 × 损坏飞刀。
 *
 * <p>损坏飞刀是无耐久度的独立物品，原版经验修补的 "isDamaged()" 判断不会选中它。
 * 这里在玩家获得经验时（{@link PlayerXpEvent.XpChange}，所有经验来源都会触发）：
 * 若背包里有带"经验修补"的损坏飞刀，则以配置概率（默认 20%）消耗 1 点经验，
 * 把 1 把损坏飞刀还原为正常飞刀，并保留全部 NBT（附魔、强化、成长等）。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class SakuyaKnifeMendingHandler {

    private SakuyaKnifeMendingHandler() {
    }

    @SubscribeEvent
    public static void onXpChange(PlayerXpEvent.XpChange event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        int amount = event.getAmount();
        if (amount <= 0) return;

        float chance = Config.SAKUYA_KNIFE_MENDING_REPAIR_CHANCE.get().floatValue();
        if (chance <= 0.0F) return;
        if (player.getRandom().nextFloat() >= chance) return;

        int slot = findMendingBrokenKnife(player);
        if (slot < 0) return;

        // 消耗 1 点经验
        event.setAmount(amount - 1);

        var inventory = player.getInventory();
        ItemStack broken = inventory.getItem(slot);

        // 还原为正常飞刀，保留原 NBT（附魔 / 强化 / 成长 / 能力数据）
        ItemStack repaired = new ItemStack(ModItems.SAKUYA_KNIFE.get());
        if (broken.hasTag() && broken.getTag() != null) {
            repaired.setTag(broken.getTag().copy());
        }
        repaired.setCount(1);

        broken.shrink(1);
        inventory.setItem(slot, broken.isEmpty() ? ItemStack.EMPTY : broken);

        if (!inventory.add(repaired)) {
            player.drop(repaired, false);
        }
        inventory.setChanged();
    }

    /**
     * 找到第一把带经验修补的损坏飞刀所在的背包槽位，没有返回 -1
     */
    private static int findMendingBrokenKnife(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.isEmpty()) continue;
            if (!stack.is(ModItems.BROKEN_SAKUYA_KNIFE.get())) continue;
            if (stack.getEnchantmentLevel(Enchantments.MENDING) <= 0) continue;
            return i;
        }
        return -1;
    }
}
