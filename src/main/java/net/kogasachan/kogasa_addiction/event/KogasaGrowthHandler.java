package net.kogasachan.kogasa_addiction.event;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaGrowthHandler {

    // 攻击命中
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IKogasaEquipment) {
            addGrowth(main);
            player.getInventory().setChanged();
        }
    }

    // 破坏方块
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        Player player = event.getPlayer();
        if (player == null) return;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IKogasaEquipment) {
            addGrowth(main);
            player.getInventory().setChanged();
        }
    }

    // 受到伤害
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        if (!(event.getEntity() instanceof Player player)) return;

        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (armor.getItem() instanceof IKogasaEquipment) {
                addGrowth(armor);
                player.getInventory().setChanged();
            }
        }
    }

    // 斧锹锄专属
    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;

        // 模拟操作不触发（客户端预览），只处理真实执行
        if (event.isSimulated()) return;

        // 根据 ToolAction 筛选
        ToolAction action = event.getToolAction();
        if (action != ToolActions.HOE_TILL
                && action != ToolActions.SHOVEL_FLATTEN
                && action != ToolActions.AXE_STRIP) {
            return;
        }

        // 获取玩家与主手物品
        Player player = event.getContext().getPlayer();
        if (player == null) return;

        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof IKogasaEquipment)) return;

        // 加成长
        if (KogasaEquipmentData.isGrowthUnlocked(main)) {
            double growth = KogasaEquipmentData.getGrowth(main);
            double max = Config.MAX_GROWTH.get();
            if (growth < max) {
                growth = Math.min(growth + Config.GROWTH_PER_USE.get(), max);
                KogasaEquipmentData.setGrowth(main, growth);
                player.getInventory().setChanged();
            }
        }
    }

    private static void addGrowth(ItemStack stack) {
        if (!KogasaEquipmentData.isGrowthUnlocked(stack)) return;

        double growth = KogasaEquipmentData.getGrowth(stack);
        double max = Config.MAX_GROWTH.get();
        if (growth >= max) return;

        growth = Math.min(growth + Config.GROWTH_PER_USE.get(), max);
        KogasaEquipmentData.setGrowth(stack, growth);
    }
}