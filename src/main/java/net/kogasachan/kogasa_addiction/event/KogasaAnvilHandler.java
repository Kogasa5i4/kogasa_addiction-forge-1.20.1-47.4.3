package net.kogasachan.kogasa_addiction.event;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaAnvilHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!Config.ENABLE_KOGASA_EQUIPMENT.get()) return;

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // 左侧必须是 Kogasa 套件
        if (!(left.getItem() instanceof IKogasaEquipment)) return;

        // 右侧不能为空
        if (right.isEmpty()) return;

        // 强化次数已满
        int re = KogasaEquipmentData.getReinforcements(left);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(left);

        // 超过上限：必须主动阻止原版输出，否则原版会正常合成
        if (re >= maxRe) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            event.setCanceled(true); // 关键
            return;
        }

        // 检查右侧是否为同类型装备
        TagKey<Item> matchTag = KogasaEquipmentHelper.getMatchingTag(left);
        if (matchTag == null || !right.is(matchTag)) return;

        // 检查该类型装备是否已经吸收过
        ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
        if (rightId == null) return;
        String materialId = rightId.toString();
        if (KogasaEquipmentData.hasUsedMaterial(left, materialId)) return;

        // 生成强化结果
        ItemStack result = left.copy();

        // 记录强化次数与吸收材料
        KogasaEquipmentData.setReinforcements(result, re + 1);
        KogasaEquipmentData.addUsedMaterial(result, rightId.toString());

        // 累加材料的固有价值（按配置比例缩放）
        double baseValue = KogasaEquipmentHelper.getEquipmentBaseValue(right);
        double ratio = KogasaEquipmentHelper.getAbsorbedRatio(right);
        double absorbValue = baseValue * ratio;
        if (absorbValue > 0) {
            KogasaEquipmentData.addAbsorbedValue(result, absorbValue);
        }

        // 设置输出与成本
        event.setOutput(result);
        event.setCost(Config.REINFORCEMENT_COST.get());
        event.setMaterialCost(1); // 消耗 1 个右侧装备
    }
}