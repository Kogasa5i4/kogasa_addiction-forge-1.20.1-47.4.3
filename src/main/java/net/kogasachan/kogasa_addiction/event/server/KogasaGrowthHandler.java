package net.kogasachan.kogasa_addiction.event.server;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaGrowthHandler {

    // 攻击命中
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        Player player = event.getEntity();
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof IKogasaEquipment) {
            addGrowth(main, player);
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
            addGrowth(main, player);
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
                addGrowth(armor, player);
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

        addGrowth(main, player);
        player.getInventory().setChanged();
    }

    private static void addGrowth(ItemStack stack, Player player) {
        if (!KogasaEquipmentData.isGrowthUnlocked(stack)) return;

        double growth = KogasaEquipmentData.getGrowth(stack);
        double max = Config.MAX_GROWTH.get();
        if (growth >= max) return;

        double oldGrowth = growth;
        growth = Math.min(growth + Config.GROWTH_PER_USE.get(), max);
        KogasaEquipmentData.setGrowth(stack, growth);

        // 只在成长值真的增加时才检查附魔解锁
        if (growth > oldGrowth) {
            checkGrowthEnchant(stack, player);
        }
    }

    /**
     * 检查成长值是否达到新的附魔解锁阈值
     */
    public static void checkGrowthEnchant(ItemStack stack, Player player) {
        if (!Config.ENABLE_GROWTH_ENCHANT.get()) return;

        double growth = KogasaEquipmentData.getGrowth(stack);
        double perEnchant = Config.GROWTH_PER_ENCHANT.get();
        int maxEnchants = Config.MAX_GROWTH_ENCHANTS.get();

        int shouldHave = (int) Math.floor(growth / perEnchant);
        shouldHave = Math.min(shouldHave, maxEnchants);

        int granted = KogasaEquipmentData.getGrowthEnchantsGranted(stack);
        if (shouldHave <= granted) return;

        // 预检查：如果所有已有附魔都达到 maxLevel+1，不再触发机制
        if (isFullyMaxed(stack)) {
            // granted 仍然推进到 shouldHave，但不播放任何音效
            KogasaEquipmentData.setGrowthEnchantsGranted(stack, shouldHave);
            return;
        }

        for (int i = granted; i < shouldHave; i++) {
            boolean success = grantOrUpgradeEnchant(stack, player);

            // granted 无条件累加，玩家可以叠数字
            KogasaEquipmentData.setGrowthEnchantsGranted(stack, i + 1);

            if (!success) {
                // 失败：播放沉闷的失败音效
                player.level().playSound(null, player.blockPosition(),
                        SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.5F);
                player.displayClientMessage(
                        Component.translatable("message.kogasa_addiction.growth_enchant.max")
                                .withStyle(ChatFormatting.RED),
                        true);
                break;
            }

            // 成功：播放附魔台音效
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.kogasa_addiction.growth_enchant")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
        }
    }

    /**
     * 判断物品上所有已有附魔是否都达到 maxLevel + 1
     */
    public static boolean isFullyMaxed(ItemStack stack) {
        Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(stack);
        if (enchants.isEmpty()) return false;
        for (Map.Entry<Enchantment, Integer> entry : enchants.entrySet()) {
            if (entry.getValue() < entry.getKey().getMaxLevel() + 1) {
                return false;
            }
        }
        return true;
    }

    /**
     * 授予或升级一个附魔
     * - 物品无附魔：随机授予一个新附魔
     * - 物品有附魔：随机选一个未达 maxLevel+1 的附魔 +1
     * - 没有可升级的：返回 false
     */
    public static boolean grantOrUpgradeEnchant(ItemStack stack, Player player) {
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(stack);

        // 1. 优先：找可升级的已有附魔
        List<Enchantment> upgradeable = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : existing.entrySet()) {
            if (entry.getValue() < entry.getKey().getMaxLevel() + 1) {
                upgradeable.add(entry.getKey());
            }
        }

        if (!upgradeable.isEmpty()) {
            Enchantment chosen = upgradeable.get(player.getRandom().nextInt(upgradeable.size()));
            int newLevel = existing.get(chosen) + 1;
            Map<Enchantment, Integer> updated = new HashMap<>(existing);
            updated.put(chosen, newLevel);
            EnchantmentHelper.setEnchantments(updated, stack);
            return true;
        }

        // 2. 兜底：所有已有附魔都满级，授予新附魔
        Enchantment newEnch = pickRandomNewEnchantment(stack, player, existing);
        if (newEnch == null) return false;

        int level;
        if (Config.ALLOW_MAX_LEVEL_ENCHANTS.get()) {
            level = 1 + player.getRandom().nextInt(newEnch.getMaxLevel());
        } else {
            level = 1;
        }
        Map<Enchantment, Integer> updated = new HashMap<>(existing);
        updated.put(newEnch, level);
        EnchantmentHelper.setEnchantments(updated, stack);
        return true;
    }

    /**
     * 从物品可接受、且尚未拥有的附魔中随机选一个
     */
    private static Enchantment pickRandomNewEnchantment(ItemStack stack, Player player,
                                                        Map<Enchantment, Integer> existing) {
        List<Enchantment> candidates = new ArrayList<>();
        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
            if (ench.isCurse()) continue;
            if (!ench.canEnchant(stack)) continue;
            if (existing.containsKey(ench)) continue;  // 已有则跳过
            candidates.add(ench);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(player.getRandom().nextInt(candidates.size()));
    }
}