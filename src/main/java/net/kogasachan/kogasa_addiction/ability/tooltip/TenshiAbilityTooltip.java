package net.kogasachan.kogasa_addiction.ability.tooltip;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.*;

import java.util.List;

/**
 * TENSHI（非想非非想天之女）的 tooltip：
 * 护甲上的境界蓄力/抗性等级，以及武器上的当前正面 Buff 列表。
 */
public final class TenshiAbilityTooltip implements KogasaAbilityTooltip {

    @Override
    public void append(KogasaAbilityTooltipContext ctx) {
        boolean isArmor = ctx.stack().getItem() instanceof ArmorItem;
        boolean isWeapon = ctx.stack().getItem() instanceof SwordItem
                || ctx.stack().getItem() instanceof DiggerItem
                || ctx.stack().getItem() instanceof BowItem
                || ctx.stack().getItem() instanceof CrossbowItem
                || ctx.stack().getItem() instanceof TridentItem;

        // 护甲专属描述与动态状态
        if (isArmor) {
            double threshold = Config.TENSHI_DOMAIN_THRESHOLD.get();

            net.minecraft.client.player.LocalPlayer player =
                    net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                boolean hasTenshiArmor = false;
                for (EquipmentSlot slot : new EquipmentSlot[]{
                        EquipmentSlot.HEAD, EquipmentSlot.CHEST,
                        EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    if (KogasaAbilityData.hasAbility(player.getItemBySlot(slot), KogasaAbility.TENSHI)) {
                        hasTenshiArmor = true;
                        break;
                    }
                }

                if (hasTenshiArmor) {
                    double charge = player.getPersistentData().getDouble("KogasaTenshiCharge");
                    boolean isReady = player.getPersistentData().getBoolean("KogasaTenshiBurstReady");
                    int resistance = (int) Math.min(5, Math.floor(charge / (threshold / 4.0)));

                    if (isReady) {
                        ctx.add(Component.translatable("tooltip.kogasa_addiction.tenshi.ready")
                                .withStyle(ChatFormatting.AQUA));
                    } else {
                        ctx.add(Component.translatable("tooltip.kogasa_addiction.tenshi.charge",
                                        KogasaNumberFormatHelper.formatTrimmed(charge, 1),
                                        KogasaNumberFormatHelper.formatTrimmed(threshold, 1),
                                        resistance)
                                .withStyle(ChatFormatting.DARK_AQUA));
                    }
                }
            }
        }

        // 武器专属描述
        if (isWeapon) {
            net.minecraft.client.player.LocalPlayer player =
                    net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) {
                List<Component> buffs = player.getActiveEffects().stream()
                        .filter(e -> e.getEffect().isBeneficial())
                        .<Component>map(e -> Component.translatable(e.getEffect().getDescriptionId())
                                .append(" ")
                                .append(Component.translatable("enchantment.level." + (e.getAmplifier() + 1))))
                        .toList();

                if (!buffs.isEmpty()) {
                    MutableComponent buffLine = Component.literal("");
                    for (Component buff : buffs) {
                        buffLine.append(buff).append(" ");
                    }
                    ctx.add(Component.translatable("tooltip.kogasa_addiction.tenshi.buffs", buffLine)
                            .withStyle(ChatFormatting.AQUA));
                }
            }
        }
    }
}
