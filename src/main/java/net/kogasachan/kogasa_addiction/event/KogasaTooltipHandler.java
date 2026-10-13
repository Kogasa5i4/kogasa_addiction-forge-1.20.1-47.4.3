package net.kogasachan.kogasa_addiction.event;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction.util.MixinConfigCache;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaTooltipHandler {

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        if (!(stack.getItem() instanceof IKogasaEquipment)) return;

        List<Component> tooltip = event.getToolTip();
        tooltip.add(Component.empty());

        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        List<String> used = KogasaEquipmentData.getUsedMaterials(stack);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(stack);

        double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);

        // 找到原版攻击伤害行的索引并替换
        String damageAttrKey = Component.translatable(
                "attribute.name.generic.attack_damage").getString();

        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (line.getString().contains(damageAttrKey)) {
                double damage = KogasaEquipmentHelper.getDisplayAttackDamage(stack);
                Component newLine = Component.translatable(
                                "tooltip.kogasa_addiction.vanilla_attack_damage",
                                String.format("%.1f", damage))
                        .withStyle(line.getStyle()) // 继承原样式
                        .withStyle(ChatFormatting.DARK_GREEN);
                tooltip.set(i, newLine);
                break;
            }
        }

        // ===== 强化信息（Shift） =====
        if (Screen.hasShiftDown()) {
            // 强化次数
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.reinforcement", re, maxRe)
                    .withStyle(ChatFormatting.GOLD));

            // ===== 倍率信息 =====
            double reMult = KogasaEquipmentHelper.getReinforcementMultiplier(stack);
            double growthMult = KogasaEquipmentHelper.getGrowthMultiplier(stack);
            double totalMult = KogasaEquipmentHelper.getTotalMultiplier(stack);

            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.multiplier.reinforcement",
                            String.format("%.2f", reMult))
                    .withStyle(ChatFormatting.YELLOW));

            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.multiplier.growth",
                            String.format("%.2f", growthMult))
                    .withStyle(ChatFormatting.YELLOW));

            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.multiplier.total",
                            String.format("%.3f", totalMult))
                    .withStyle(ChatFormatting.GOLD));

            // 已吸收装备
            if (used.isEmpty()) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.reinforcement.none")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (String id : used) {
                    Component itemName = getItemDisplayName(id);
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.reinforcement.entry", itemName)
                            .withStyle(ChatFormatting.GRAY));
                }
            }

            // 吸收值
            if (absorbed > 0) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.absorbed", String.format("%.1f", absorbed))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.shift")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        // ===== 成长信息（Ctrl，仅解锁后显示） =====
        if (re >= MixinConfigCache.growthUnlockReinforcements) {
            if (Screen.hasControlDown()) {
                String percent = String.format("%.2f%%", growth * 100);
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.growth", percent)
                        .withStyle(ChatFormatting.GREEN));

                // 新增：成长附魔进度
                int grantedEnchants = KogasaEquipmentData.getGrowthEnchantsGranted(stack);
                int maxEnchants = Config.MAX_GROWTH_ENCHANTS.get();
                if (Config.ENABLE_GROWTH_ENCHANT.get() && maxEnchants > 0) {
                    tooltip.add(Component.translatable(
                                    "tooltip.kogasa_addiction.growth_enchant", grantedEnchants, maxEnchants)
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            } else {
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.ctrl")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        // ===== 能力信息（Alt） =====
        if (Screen.hasAltDown()) {
            List<KogasaAbility> abilities = KogasaAbilityData.getAbilities(stack);
            int maxSlots = KogasaAbilityData.getMaxAbilitySlots(stack);

            // 标题
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.abilities", abilities.size(), maxSlots)
                    .withStyle(ChatFormatting.AQUA));

            // 能力列表 / 空提示
            if (abilities.isEmpty()) {
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.abilities.none")
                        .withStyle(ChatFormatting.DARK_GRAY));
                tooltip.add(Component.translatable("tooltip.kogasa_addiction.abilities.how_to")
                        .withStyle(ChatFormatting.DARK_GRAY));
            } else {
                for (KogasaAbility ability : abilities) {
                    tooltip.add(Component.literal("  ✦ ")
                            .append(ability.getDisplayName())
                            .withStyle(ChatFormatting.LIGHT_PURPLE));
                }
            }
            // 槽位满且未解锁第 2 槽 → 显示解锁提示（只在有能力时显示）
            if (abilities.size() >= maxSlots && maxSlots < 2) {
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.abilities.full_hint")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.KOGASA)) {
                double reMult = KogasaEquipmentHelper.getReinforcementMultiplier(stack);
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.kogasa.multiplier",
                                String.format("%.2f", reMult))
                        .withStyle(ChatFormatting.GOLD));
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.AYA)) {
                double news = KogasaAbilityData.getNewsValue(stack);
                int killed = KogasaAbilityData.getKilledMobCount(stack);
                int mined = KogasaAbilityData.getMinedBlockCount(stack);
                int damage = KogasaAbilityData.getDamageTakenCount(stack);

                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.aya.news_value",
                                String.format("%.2f%%", news * 100),
                                String.format("×%.3f", 1.0 + news))
                        .withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.aya.records",
                                killed, mined, damage)
                        .withStyle(ChatFormatting.GRAY));
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.CIRNO)) {
                double cost = Config.CIRNO_REVIVE_GROWTH_COST.get();
                boolean canRevive = growth >= cost;

                MutableComponent reviveLine = Component.translatable(
                        "tooltip.kogasa_addiction.cirno.revive_status",
                        String.format("%.2f", cost));
                reviveLine.withStyle(canRevive ? ChatFormatting.GREEN : ChatFormatting.RED);
                tooltip.add(reviveLine);
            }

            if (KogasaAbilityData.hasAbility(stack, KogasaAbility.KOISHI)) {
                double chance = Config.KOISHI_CRIT_CHANCE.get() * 100;
                double mult = Config.KOISHI_CRIT_MULTIPLIER.get();
                tooltip.add(Component.translatable(
                                "tooltip.kogasa_addiction.koishi.crit_info",
                                String.format("%.0f%%", chance),
                                String.format("%.1f", mult))
                        .withStyle(ChatFormatting.LIGHT_PURPLE));
            }
        } else {
            tooltip.add(Component.translatable("tooltip.kogasa_addiction.hint.alt")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * 把 "minecraft:iron_sword" 转成玩家当前语言下的物品显示名
     */
    private static Component getItemDisplayName(String id) {
        ResourceLocation loc = ResourceLocation.tryParse(id);
        if (loc == null) return Component.literal(id);

        var item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) return Component.literal(id);
        return new ItemStack(item).getHoverName();
    }
}