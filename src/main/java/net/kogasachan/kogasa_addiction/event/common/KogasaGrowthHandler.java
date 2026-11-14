package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.SakuyaKnifeItem;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.kogasachan.kogasa_addiction.util.KogasaCuriosHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaIdListMatcher;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaGrowthHandler {

    /**
     * 配置重载后由 KogasaConfigEvents 调用；现在列表直接读配置，无需清缓存。
     */
    public static void invalidateCache() {
        // 保留方法供配置事件调用；匹配实时读取配置
    }

    // 攻击命中
    @SubscribeEvent
    public static void onAttack(AttackEntityEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        triggerGrowth(player);
    }

    // 破坏方块
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        triggerGrowth(player);
    }

    // 受到伤害
    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        DamageSource source = event.getSource();
        // 来源缺失（异常或其它模组构造的 DamageSource）直接跳过
        if (source == null) return;
        // 0 伤害（被完全吸收、无敌帧内等）不喂成长
        if (event.getAmount() <= 0.0F) return;
        // 环境伤害不算"承受攻击"：否则站在火里 / 反复摔落 / 泡水就能零成本刷成长
        if (source.is(DamageTypeTags.IS_FIRE)
                || source.is(DamageTypeTags.IS_FALL)
                || source.is(DamageTypeTags.IS_DROWNING)
                || source.is(DamageTypeTags.IS_FREEZING)) {
            return;
        }

        triggerGrowth(player);
    }

    // 斧锹锄专属
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        if (event.isSimulated()) return;
        if (event.getResult() == Event.Result.DENY) return;
        if (event.isCanceled()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        // 初状态 vs 末状态：一样就说明没有实际修改
        BlockState before = event.getState();
        BlockState after = event.getFinalState();
        if (before == after) return;

        ToolAction action = event.getToolAction();
        boolean isRightClickModification =
                action == ToolActions.HOE_TILL
                        || action == ToolActions.SHOVEL_FLATTEN
                        || action == ToolActions.AXE_STRIP
                        || action == ToolActions.AXE_SCRAPE
                        || action == ToolActions.AXE_WAX_OFF;

        if (!isRightClickModification) return;
        triggerGrowth(player);
    }

    public static void triggerGrowth(ServerPlayer player) {
        for (EquipmentSlot slot : new EquipmentSlot[]{
                EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                EquipmentSlot.LEGS, EquipmentSlot.FEET,
                EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND}) {
            ItemStack armor = player.getItemBySlot(slot);
            if (net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(armor)) {
                addGrowth(armor, player);
                player.getInventory().setChanged();
            }
        }

        // Curios 槽位与 EquipmentSlot 平行遍历；法术书/戒指等也能积累成长。
        KogasaCuriosHelper.forEachEquippedMutable(player, stack -> {
            if (net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(stack)) {
                addGrowth(stack, player);
            }
        });
    }

    public static void addGrowth(ItemStack stack, ServerPlayer player) {
        // 封印装备（战利品）不需要先强化也能成长：成长正是解封的进度
        boolean locked = KogasaEquipmentData.isLocked(stack);
        if (!KogasaEquipmentData.isGrowthUnlocked(stack) && !locked) return;

        double growth = KogasaEquipmentData.getGrowth(stack);
        double max = Config.MAX_GROWTH.get();
        if (growth >= max) return;

        double oldGrowth = growth;
        growth = Math.min(growth + Config.GROWTH_PER_USE.get(), max);
        KogasaEquipmentData.setGrowth(stack, growth);

        // 只在成长值真的增加时才检查附魔解锁；封印期间不发成长附魔（解封时一次性结算）
        if (growth > oldGrowth && !locked) {
            checkGrowthEnchant(stack, player);
        }

        // 封印装备跨过解封阈值
        if (growth > oldGrowth && locked) {
            checkLockedUnlock(stack, player);
        }

        KogasaAdvancementHelper.checkAndGrant(player, stack);
    }

    /**
     * 封印装备在成长达到阈值时自动解封：属性恢复全力、能力生效 + 结算应得的成长附魔。
     */
    private static void checkLockedUnlock(ItemStack stack, ServerPlayer player) {
        double threshold = Config.LOCKED_EQUIPMENT_UNLOCK_GROWTH.get();
        if (KogasaEquipmentData.getGrowth(stack) < threshold) return;

        KogasaEquipmentData.setLocked(stack, false);

        player.level().playSound(null, player.blockPosition(),
                SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.8F, 1.2F);
        player.displayClientMessage(
                Component.translatable("message.kogasa_addiction.locked_equipment.unlocked")
                        .withStyle(ChatFormatting.GOLD),
                true);

        // 解封瞬间一次性结算封印期间积累的成长附魔
        checkGrowthEnchant(stack, player);
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

        for (int i = granted; i < shouldHave; i++) {
            boolean success = grantOrUpgradeEnchant(stack, player);

            if (!success) {
                // 只在第一次失败时提示，之后静默
                if (!KogasaEquipmentData.hasGrowthEnchantFailNotified(stack)) {
                    KogasaEquipmentData.setGrowthEnchantFailNotified(stack, true);

                    player.level().playSound(null, player.blockPosition(),
                            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.5F, 1.5F);
                    player.displayClientMessage(
                            Component.translatable("message.kogasa_addiction.growth_enchant.max")
                                    .withStyle(ChatFormatting.RED),
                            true);
                }
                break;
            }

            // 成功：播放附魔台音效
            KogasaEquipmentData.setGrowthEnchantsGranted(stack, i + 1);
            player.level().playSound(null, player.blockPosition(),
                    SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(
                    Component.translatable("message.kogasa_addiction.growth_enchant")
                            .withStyle(ChatFormatting.LIGHT_PURPLE),
                    true);
            KogasaEquipmentData.setGrowthEnchantFailNotified(stack, false);
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
     * - 物品有附魔：随机选一个未达 maxLevel+1 的非黑名单附魔 +1；没有可升级项时随机授予新附魔
     * - 没有可升级的：返回 false
     */
    public static boolean grantOrUpgradeEnchant(ItemStack stack, Player player) {
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(stack);

        // 1. 优先：找可升级的已有附魔（排除黑名单）
        List<Enchantment> upgradeable = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : existing.entrySet()) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if (isBlacklisted(id) || isFrozen(id)) continue;
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

        // 2. 兜底：授予新附魔
        Enchantment newEnch = pickRandomNewEnchantment(stack, player, existing);
        if (newEnch == null) return false;

        int level = 1;
        if (Config.ALLOW_MAX_LEVEL_ENCHANTS.get()) {
            int maxLevel = Math.max(1, newEnch.getMaxLevel());
            level += player.getRandom().nextInt(maxLevel);
        }
        Map<Enchantment, Integer> updated = new HashMap<>(existing);
        updated.put(newEnch, level);
        EnchantmentHelper.setEnchantments(updated, stack);
        return true;
    }

    /**
     * 从物品可接受、尚未拥有、且不在黑名单中的附魔里随机选一个。
     */
    private static Enchantment pickRandomNewEnchantment(ItemStack stack, Player player,
                                                        Map<Enchantment, Integer> existing) {
        List<Enchantment> candidates = new ArrayList<>();

        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
            if (ench.isCurse()) continue;
            if (!ench.canEnchant(stack)) continue;
            if (existing.containsKey(ench)) continue;

            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(ench);
            if (isBlacklisted(id) || isUpgradeOnly(id) || isFrozen(id)) continue;   // ← 黑名单 / 只升级 / 冻结过滤

            if (stack.getItem() instanceof SakuyaKnifeItem && ench == Enchantments.RIPTIDE) {
                continue;
            }

            candidates.add(ench);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(player.getRandom().nextInt(candidates.size()));
    }

    /**
     * 模式 1：绝对黑名单。
     */
    public static boolean isBlacklisted(ResourceLocation id) {
        return KogasaIdListMatcher.matches(id, Config.GROWTH_ENCHANT_BLACKLIST.get());
    }

    /**
     * 模式 2：本来没有就不给，有的可以升级。
     */
    private static boolean isUpgradeOnly(ResourceLocation id) {
        return KogasaIdListMatcher.matches(id, Config.GROWTH_ENCHANT_UPGRADE_ONLY.get());
    }

    /**
     * 模式 3：本来有也不动、不升级；没有也不给。
     */
    private static boolean isFrozen(ResourceLocation id) {
        return KogasaIdListMatcher.matches(id, Config.GROWTH_ENCHANT_FROZEN.get());
    }
}