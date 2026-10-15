package net.kogasachan.kogasa_addiction.event.server;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.KogasaAbilityIngotItem;
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
        if (right.isEmpty()) return;

        // ========== 分支 1：能力锻造（右侧是能力锭） ==========
        if (right.getItem() instanceof KogasaAbilityIngotItem ingotItem) {
            handleAbilityInfusion(event, left, ingotItem.getAbility());
            return;
        }

        // ========== 分支 2：强化（右侧是同类型装备） ==========
        handleReinforcement(event, left, right);
    }

    /**
     * 能力锻造：装备 + 能力锭 → 装备获得能力
     */
    private static void handleAbilityInfusion(AnvilUpdateEvent event,
                                              ItemStack left,
                                              KogasaAbility ability) {
        if (KogasaAbilityData.hasAbility(left, ability)) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        int currentCount = KogasaAbilityData.getAbilities(left).size();
        int maxSlots = KogasaAbilityData.getMaxAbilitySlots(left);
        if (currentCount >= maxSlots) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        ItemStack result = left.copy();
        KogasaAbilityData.addAbility(result, ability);

        event.setOutput(result);
        event.setCost(Config.ABILITY_INFUSION_COST.get());
        event.setMaterialCost(1);
    }

    /**
     * 强化：装备 + 同类型装备 → 装备强化 1 级
     * 原有逻辑，未改动
     */
    private static void handleReinforcement(AnvilUpdateEvent event,
                                            ItemStack left, ItemStack right) {
        int re = KogasaEquipmentData.getReinforcements(left);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(left);

        TagKey<Item> matchTag = KogasaEquipmentHelper.getMatchingTag(left);
        if (matchTag == null || !right.is(matchTag)) return;

        ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
        if (rightId == null) return;

        if (KogasaEquipmentData.hasUsedMaterial(left, rightId.toString())) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        if (re >= maxRe) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        ItemStack result = left.copy();
        KogasaEquipmentData.setReinforcements(result, re + 1);
        KogasaEquipmentData.addUsedMaterial(result, rightId.toString());

        double baseValue = KogasaEquipmentHelper.getEquipmentBaseValue(right);
        double ratio = KogasaEquipmentHelper.getAbsorbedRatio(right);
        double absorb = baseValue * ratio;
        if (absorb > 0) {
            KogasaEquipmentData.addAbsorbedValue(result, absorb);
        }

        event.setOutput(result);
        event.setCost(Config.REINFORCEMENT_COST.get());
        event.setMaterialCost(1);
    }
}