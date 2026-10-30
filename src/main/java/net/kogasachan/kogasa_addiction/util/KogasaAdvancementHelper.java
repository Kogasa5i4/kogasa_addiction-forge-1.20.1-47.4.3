package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.minecraft.advancements.Advancement;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public class KogasaAdvancementHelper {

    /**
     * 检查指定装备，并尝试授予所有相关进度。
     * 应在所有会修改装备 NBT 的地方调用。
     */
    public static void checkAndGrant(ServerPlayer player, ItemStack stack) {
        if (!(stack.getItem() instanceof IKogasaEquipment)) return;

        // 强化
        int re = KogasaEquipmentData.getReinforcements(stack);
        if (re >= 1) grant(player, "first_upgrade");
        if (re >= 6) grant(player, "six_slots");
        if (re >= 7) grant(player, "seven_slots");

        // 能力
        int abilityCount = KogasaAbilityData.getAbilities(stack).size();
        if (abilityCount >= 1) grant(player, "first_ability");
        if (abilityCount >= 2) grant(player, "two_abilities");

        // 成长值
        double growth = KogasaEquipmentData.getGrowth(stack);
        if (growth > 0) grant(player, "start_growth");
        if (growth >= 1.0) grant(player, "max_growth");

        // 风神少女（弩 + 自动装填阈值）
        if (stack.getItem() instanceof CrossbowItem
                && growth >= Config.CROSSBOW_AUTO_RELOAD_GROWTH.get()) {
            grant(player, "wind_god_girl");
        }

        // 月战老兵（不消耗概率 100%）
        if ((stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem)
                && KogasaEquipmentHelper.getNoConsumeArrowChance(stack) >= 1.0) {
            grant(player, "moon_veteran");
        }

        // 新闻值
        double news = KogasaAbilityData.getNewsValue(stack);
        if (news >= 0.0723) grant(player, "reporter_novice");
        if (news >= 1.0) grant(player, "reporter_master");

        // 高面板（攻击 > 30 或 防御 > 30）
        if (KogasaEquipmentHelper.getDisplayAttackDamage(stack) > 30) {
            grant(player, "high_stats");
        }
        if (stack.getItem() instanceof ArmorItem armor && armor.getDefense() * KogasaEquipmentData.getMultiplier(stack) > 30) {
            grant(player, "high_stats");
        }

        // 河童科技（有弹药）
        if (KogasaAbilityData.hasAmmo(stack)) {
            grant(player, "kappa_tech");
        }
    }

    /**
     * 授予单个进度
     */
    public static void grant(ServerPlayer player, String advancementPath) {
        var server = player.getServer();
        if (server == null) return;

        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                KogasaAddiction.MODID, advancementPath);
        Advancement advancement = server.getAdvancements().getAdvancement(id);

        if (advancement != null) {
            player.getAdvancements().award(advancement, "impossible");
        }
    }

    /**
     * 扫描玩家整个背包，检查并补发所有符合条件的进度。
     * 用于：玩家登录、Shift快捷取出铁砧物品等场景。
     */
    public static void scanInventoryAndGrant(ServerPlayer player) {
        var inventory = player.getInventory();
        // 41 格：36 主背包 + 4 护甲 + 1 副手
        int size = inventory.getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof IKogasaEquipment) {
                checkAndGrant(player, stack);
            }
        }
    }
}
