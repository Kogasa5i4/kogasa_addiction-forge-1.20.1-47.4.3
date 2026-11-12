package net.kogasachan.kogasa_addiction.event.client;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

/**
 * 本模组 tooltip 的协调入口：只负责编排各区块的执行顺序，
 * 具体渲染在 {@link KogasaTooltipStats} / {@link KogasaTooltipReinforcement} /
 * {@link KogasaTooltipGrowth} / {@link KogasaTooltipAlt}。
 *
 * <p>原先同一个物品的 tooltip 被 KogasaTooltipHandler 与
 * MiningVanillaTooltipHandler 两个订阅者分头修改（数值行与区块的执行顺序不明确），
 * 现在合并成一条确定顺序的链：数值行 → Shift → Ctrl → Alt。
 */
@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class KogasaTooltipHandler {

    private KogasaTooltipHandler() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();
        boolean isKogasa = net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(stack);
        List<Component> tooltip = event.getToolTip();

        if (!isKogasa) {
            // 非本模组物品：只有按配置开启时才显示挖掘速度/蓄力弹速
            KogasaTooltipStats.apply(stack, tooltip, false);
            return;
        }

        // 原版属性行与本模组行之间的空行
        tooltip.add(Component.empty());

        // 只有本模组原生装备替换攻击伤害/攻速行；外部兼容装备保留它自己的
        // 原生 Tooltip（拔刀剑的 bladeState 伤害不能被 SwordItem 字段覆盖）。
        boolean nativeKogasa = stack.getItem() instanceof IKogasaEquipment;
        KogasaTooltipStats.apply(stack, tooltip, nativeKogasa);

        int re = KogasaEquipmentData.getReinforcements(stack);
        double growth = KogasaEquipmentData.getGrowth(stack);
        List<String> used = KogasaEquipmentData.getUsedMaterials(stack);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(stack);
        double absorbed = KogasaEquipmentData.getAbsorbedValue(stack);

        KogasaTooltipReinforcement.apply(stack, tooltip, re, maxRe, used, absorbed);
        KogasaTooltipGrowth.apply(stack, tooltip, re, growth);
        KogasaTooltipAlt.apply(stack, tooltip, growth);

    }
}
