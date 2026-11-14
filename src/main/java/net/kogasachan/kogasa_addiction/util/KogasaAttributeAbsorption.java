package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.item.IKogasaEquipment;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.registries.ForgeRegistries;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;

import java.util.*;

/**
 * 从强化材料读取可吸收属性；物品 modifier 是相对 0 的增量，不涉及实体默认值。
 */
public final class KogasaAttributeAbsorption {

    public enum Category {
        MELEE, RANGED, ARMOR, OTHER
    }

    private KogasaAttributeAbsorption() {
    }

    public record Multiplier(UUID uuid, ResourceLocation attribute, double amount,
                             AttributeModifier.Operation operation) {
    }

    public record Result(Map<ResourceLocation, Double> additive,
                         List<Multiplier> multipliers) {
    }

    /**
     * 汇总材料所有装备槽的属性 modifier；同一属性取最大值，避免跨槽重复。
     */
    @SuppressWarnings("deprecation")
    public static Result collect(ItemStack material, Category category, LivingEntity wearer) {
        Map<ResourceLocation, Double> additive = new LinkedHashMap<>();
        List<Multiplier> multipliers = new ArrayList<>();
        Result result = new Result(additive, multipliers);
        if (material == null || material.isEmpty()) return result;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            // 非本模组物品走 getAttributeModifiers，才能拿到 ISS/FE 通过
            // ItemAttributeModifierEvent 动态注册的属性；本模组装备仍用默认表避免递归。
            var modifiers = material.getItem() instanceof IKogasaEquipment
                    ? material.getItem().getDefaultAttributeModifiers(slot)
                    : material.getAttributeModifiers(slot);
            collectModifiers(result, category, modifiers);
        }

        // ISS 法术书等 Curios 物品的属性只存在于 ICurio#getAttributeModifiers(String)，
        // 普通 ItemStack 属性表拿不到。
        if (ModList.get().isLoaded("curios")) {
            for (String slot : CuriosApi.getItemStackSlots(material, false).keySet()) {
                // 1.20.1 仍可用，且不是 1.21 计划移除的 ICurio#getAttributeModifiers(String)。
                SlotContext context = new SlotContext(slot, wearer, 0, false, true);
                collectModifiers(result, category,
                        CuriosApi.getAttributeModifiers(context, java.util.UUID.randomUUID(), material));
            }
        }
        return result;
    }

    private static void collectModifiers(Result result,
                                         Category category,
                                         com.google.common.collect.Multimap<Attribute, AttributeModifier> modifiers) {
        for (var entry : modifiers.entries()) {
            Attribute attribute = entry.getKey();
            ResourceLocation id = ForgeRegistries.ATTRIBUTES.getKey(attribute);
            if (id == null || isBlacklisted(id, category)) continue;
            // 攻速不走吸收：攻速只由成长值/文文新闻值控制。
            if (id.equals(ResourceLocation.withDefaultNamespace("generic.attack_speed"))) continue;

            double amount = entry.getValue().getAmount();
            if (amount <= 0.0D) continue;
            if (entry.getValue().getOperation() == AttributeModifier.Operation.ADDITION) {
                result.additive().merge(id, amount, Math::max);
            } else {
                result.multipliers().add(new Multiplier(
                        entry.getValue().getId(), id, amount, entry.getValue().getOperation()));
            }
        }
    }

    public static boolean isBlacklisted(ResourceLocation id, Category category) {
        var list = switch (category) {
            case MELEE -> Config.ATTRIBUTE_ABSORB_MELEE_BLACKLIST.get();
            case RANGED -> Config.ATTRIBUTE_ABSORB_RANGED_BLACKLIST.get();
            case ARMOR -> Config.ATTRIBUTE_ABSORB_ARMOR_BLACKLIST.get();
            case OTHER -> java.util.List.<String>of();
        };
        return KogasaIdListMatcher.matches(id, list);
    }

    /**
     * 按 attributeAbsorbScale 取倍率；第一个匹配的 pattern 生效，默认 1.0。
     */
    public static double scale(ResourceLocation id) {
        for (String raw : Config.ATTRIBUTE_ABSORB_SCALE.get()) {
            if (raw == null) continue;
            int split = raw.lastIndexOf('=');
            if (split <= 0 || split >= raw.length() - 1) continue;

            String pattern = raw.substring(0, split).trim();
            if (!KogasaIdListMatcher.matches(id, java.util.List.of(pattern))) continue;

            try {
                return Double.parseDouble(raw.substring(split + 1).trim());
            } catch (NumberFormatException ignored) {
                return 1.0D;
            }
        }
        return 1.0D;
    }
}
