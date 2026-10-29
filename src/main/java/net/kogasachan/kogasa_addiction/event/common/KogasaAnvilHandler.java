package net.kogasachan.kogasa_addiction.event.common;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.kogasachan.kogasa_addiction.item.KogasaAbilityIngotItem;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.item.SakuyaKnifeItem;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentData;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Map;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class KogasaAnvilHandler {

    @SubscribeEvent
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        if (!Config.ENABLE_KOGASA_EQUIPMENT.get()) return;

        ItemStack left = event.getLeft();
        ItemStack right = event.getRight();

        // 检查左侧是否为咲夜飞刀
        if (left.getItem() instanceof SakuyaKnifeItem) {
            // 检查右侧材料（附魔书或另一把三叉戟）是否含有激流
            Map<Enchantment, Integer> enchants = EnchantmentHelper.getEnchantments(right);
            if (enchants.containsKey(Enchantments.RIPTIDE)) {
                event.setOutput(ItemStack.EMPTY); // 禁止输出
                event.setCost(0);
                return;
            }
        }

        if (left.is(ModItems.BROKEN_SAKUYA_KNIFE.get()) && right.is(ModItems.SAKUYA_INGOT.get())) {
            ItemStack result = new ItemStack(ModItems.SAKUYA_KNIFE.get());
            if (left.getTag() != null && left.hasTag()) {
                result.setTag(left.getTag().copy());
            }
            // 一次最多修复 3 把；超过 3 把的余量由 AnvilMenuMixin 放回左侧输入槽
            result.setCount(Math.min(left.getCount(), 3));

            event.setOutput(result);
            event.setCost(Config.SAKUYA_KNIFE_REPAIR_COST.get());
            event.setMaterialCost(1);
        }

        // 左侧必须是 Kogasa 套件
        if (!(left.getItem() instanceof IKogasaEquipment)) return;
        if (right.isEmpty()) return;

        // ========== 分支 1：能力锻造（右侧是能力锭） ==========
        if (right.getItem() instanceof KogasaAbilityIngotItem ingotItem) {
            handleAbilityInfusion(event, left, ingotItem.getAbility());
            return;
        }

        // ========== 分支 2：弓弩弹药装载 ==========
        if (isRangedWeapon(left)) {
            if (handleAmmoInfusion(event, left, right)) return;
        }

        // 同一种可损坏装备的原版合并优先于自定义强化：
        // 若耐久修复或右侧附魔合并有意义，就让原版铁砧继续计算。
        if (shouldUseVanillaCombination(left, right)) return;

        // ========== 分支 3：强化（右侧是同类型装备） ==========
        handleReinforcement(event, left, right);
    }

    /**
     * 判断是否应交由原版铁砧处理同物品合并。
     * 当左侧需要修复、右侧带有损坏耐久，或右侧带有附魔时，优先保留原版合并行为。
     */
    private static boolean shouldUseVanillaCombination(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) return false;
        if (left.getItem() != right.getItem()) return false;
        if (!left.isDamageableItem()) return false;

        return left.isDamaged() || right.isDamaged() || right.isEnchanted();
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

        int currentCount = KogasaAbilityData.getUsedSlots(left);
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
     * 水桶清强化，奶桶清能力，保留成长及其他数据
     */
    private static void handleReinforcement(AnvilUpdateEvent event,
                                            ItemStack left, ItemStack right) {
        if (left.getItem() == right.getItem()) {
            event.setOutput(ItemStack.EMPTY);
            event.setCost(0);
            return;
        }

        // ===== 清除数据逻辑：水桶清强化，奶桶清能力 =====
        if (right.is(Items.WATER_BUCKET) || right.is(Items.MILK_BUCKET)) {
            ItemStack result = left.copy();
            net.minecraft.nbt.CompoundTag rootTag = result.getTag();

            if (rootTag != null) {
                if (right.is(Items.WATER_BUCKET)) {
                    // 水桶：精准移除强化相关字段，保留成长
                    if (rootTag.contains("KogasaEquipment", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
                        net.minecraft.nbt.CompoundTag equipTag = rootTag.getCompound("KogasaEquipment");
                        equipTag.remove("Reinforcements");
                        equipTag.remove("UsedMaterials");
                        equipTag.remove("AbsorbedValue");

                        // 如果强化清空后连成长也没了，就把这个标签彻底删掉
                        if (equipTag.isEmpty()) {
                            rootTag.remove("KogasaEquipment");
                        }
                    }
                } else if (right.is(Items.MILK_BUCKET)) {
                    // 奶桶：移除能力标签，保留强化和成长
                    rootTag.remove("KogasaAbilities");
                }

                // 如果清理后 NBT 完全空了，就清除空标签防止残留
                if (rootTag.isEmpty()) {
                    result.setTag(null);
                }
            }

            event.setOutput(result);
            event.setCost(Config.CLEAR_COST.get());
            event.setMaterialCost(1); // 消耗 1 个水桶或奶桶
            return;
        }

        int re = KogasaEquipmentData.getReinforcements(left);
        int maxRe = KogasaEquipmentHelper.getEffectiveMaxReinforcements(left);

        // ===== 判断材料是否有效 =====
        boolean isBowOrCrossbow = left.getItem() instanceof BowItem
                || left.getItem() instanceof CrossbowItem;
        boolean isArrowMaterial = isBowOrCrossbow && right.is(ItemTags.ARROWS);

        String materialId;
        if (isBowOrCrossbow) {
            // 弓弩：本体或 16 支箭矢
            if (isArrowMaterial) {
                if (right.getCount() < 16) return;
                ResourceLocation arrowId = ForgeRegistries.ITEMS.getKey(right.getItem());
                if (arrowId == null) return;
                materialId = "arrow:" + arrowId;
            } else if (right.getItem() instanceof BowItem
                    || right.getItem() instanceof CrossbowItem) {
                ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
                if (rightId == null) return;
                materialId = rightId.toString();
                if (KogasaEquipmentData.hasUsedMaterial(left, materialId)) {
                    event.setOutput(ItemStack.EMPTY);
                    return;
                }
            } else {
                return;
            }
        } else {
            // 非弓弩：tag 优先，后缀 fallback
            if (!KogasaEquipmentHelper.matchesReinforcementMaterial(left, right)) return;

            ResourceLocation rightId = ForgeRegistries.ITEMS.getKey(right.getItem());
            if (rightId == null) return;
            materialId = rightId.toString();
            if (KogasaEquipmentData.hasUsedMaterial(left, materialId)) {
                event.setOutput(ItemStack.EMPTY);
                return;
            }
        }

        // ===== 强化上限检查 =====
        if (re >= maxRe) {
            event.setOutput(ItemStack.EMPTY);
            return;
        }

        // ===== 应用强化 =====
        ItemStack result = left.copy();
        KogasaEquipmentData.setReinforcements(result, re + 1);
        KogasaEquipmentData.addUsedMaterial(result, materialId);

        double baseValue = KogasaEquipmentHelper.getEquipmentBaseValue(right);
        double ratio = KogasaEquipmentHelper.getAbsorbedRatio(right);
        double absorb = baseValue * ratio;
        if (absorb > 0) {
            KogasaEquipmentData.addAbsorbedValue(result, absorb);
        }

        event.setOutput(result);
        event.setCost(Config.REINFORCEMENT_COST.get());
        event.setMaterialCost(isArrowMaterial ? 16 : 1);
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem;
    }

    private static boolean handleAmmoInfusion(AnvilUpdateEvent event,
                                              ItemStack left, ItemStack right) {
        // ===== 药水（三种） =====
        if (right.getItem() instanceof PotionItem) {
            ResourceLocation potionId = ForgeRegistries.POTIONS.getKey(
                    PotionUtils.getPotion(right));
            if (potionId == null) return false;

            KogasaAbilityData.PotionKind kind =
                    right.getItem() instanceof LingeringPotionItem
                            ? KogasaAbilityData.PotionKind.LINGERING
                            : right.getItem() instanceof SplashPotionItem
                            ? KogasaAbilityData.PotionKind.SPLASH
                            : KogasaAbilityData.PotionKind.REGULAR;

            ItemStack result = left.copy();
            KogasaAbilityData.setPotionAmmo(result, potionId, kind);
            event.setOutput(result);
            event.setCost(Config.AMMO_INFUSION_COST.get());
            event.setMaterialCost(1);
            return true;
        }

        // ===== 烟花 =====
        if (right.is(Items.FIREWORK_ROCKET)) {
            CompoundTag nbt = right.getTag();
            if (nbt == null || !nbt.contains("Fireworks")) return false;

            ItemStack result = left.copy();
            KogasaAbilityData.setFireworkAmmo(result, nbt.copy());
            event.setOutput(result);
            event.setCost(Config.AMMO_INFUSION_COST.get());
            event.setMaterialCost(1);
            return true;
        }

        return false;
    }
}