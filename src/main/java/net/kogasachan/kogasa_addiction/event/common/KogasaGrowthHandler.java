package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.SakuyaKnifeItem;
import net.kogasachan.kogasa_addiction.util.KogasaAdvancementHelper;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.AttackEntityEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.*;
import java.util.regex.Pattern;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaGrowthHandler {

    private static volatile Set<ResourceLocation> cachedExactIds;
    private static volatile List<Pattern> cachedPatterns;

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() == Config.SPEC) {
            cachedExactIds = null;
            cachedPatterns = null;
        }
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

        triggerGrowth(player);
    }

    // 斧锹锄专属
    @SubscribeEvent
    public static void onBlockToolModification(BlockEvent.BlockToolModificationEvent event) {
        if (!Config.ENABLE_GROWTH.get()) return;
        if (event.isSimulated()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

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
            if (armor.getItem() instanceof IKogasaEquipment) {
                addGrowth(armor, player);
                player.getInventory().setChanged();
            }
        }
    }

    public static void addGrowth(ItemStack stack, ServerPlayer player) {
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

        KogasaAdvancementHelper.checkAndGrant(player, stack);
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
     * - 物品有附魔：随机选一个未达 maxLevel+1 的附魔 +1，如果都满级则随机授予一个新附魔
     * - 没有可升级的：返回 false
     */
    public static boolean grantOrUpgradeEnchant(ItemStack stack, Player player) {
        Map<Enchantment, Integer> existing = EnchantmentHelper.getEnchantments(stack);

        // 1. 优先：找可升级的已有附魔（排除黑名单）
        List<Enchantment> upgradeable = new ArrayList<>();
        for (Map.Entry<Enchantment, Integer> entry : existing.entrySet()) {
            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(entry.getKey());
            if (isBlacklisted(id)) continue;
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
     * 从物品可接受、且尚未拥有的附魔中随机选一个
     */
    private static Enchantment pickRandomNewEnchantment(ItemStack stack, Player player,
                                                        Map<Enchantment, Integer> existing) {
        List<Enchantment> candidates = new ArrayList<>();

        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS) {
            if (ench.isCurse()) continue;
            if (!ench.canEnchant(stack)) continue;
            if (existing.containsKey(ench)) continue;

            ResourceLocation id = ForgeRegistries.ENCHANTMENTS.getKey(ench);
            if (isBlacklisted(id)) continue;   // ← 黑名单过滤

            if (stack.getItem() instanceof SakuyaKnifeItem && ench == Enchantments.RIPTIDE) {
                continue;
            }

            candidates.add(ench);
        }
        if (candidates.isEmpty()) return null;
        return candidates.get(player.getRandom().nextInt(candidates.size()));
    }

    private static void rebuildBlacklistCache() {
        Set<ResourceLocation> exact = new HashSet<>();
        List<Pattern> patterns = new ArrayList<>();

        for (String raw : Config.GROWTH_ENCHANT_BLACKLIST.get()) {
            String s = raw.trim();
            if (s.isEmpty()) continue;

            if (s.indexOf('*') >= 0 || s.indexOf('?') >= 0) {
                // 含通配符 → 转成正则
                patterns.add(Pattern.compile(globToRegex(s)));
            } else {
                // 无通配符 → 精确 ID
                ResourceLocation id = ResourceLocation.tryParse(s);
                if (id != null) exact.add(id);
            }
        }

        cachedExactIds = exact;
        cachedPatterns = List.copyOf(patterns);
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*' -> sb.append(".*");
                case '?' -> sb.append('.');
                case '.', '(', ')', '[', ']', '{', '}', '+', '^', '$', '|', '\\' ->
                        sb.append('\\').append(c);
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    private static boolean isBlacklisted(ResourceLocation id) {
        if (id == null) return false;
        if (cachedExactIds == null || cachedPatterns == null) {
            rebuildBlacklistCache();
        }
        if (cachedExactIds.contains(id)) return true;

        String full = id.toString();
        for (Pattern p : cachedPatterns) {
            if (p.matcher(full).matches()) return true;
        }
        return false;
    }
}