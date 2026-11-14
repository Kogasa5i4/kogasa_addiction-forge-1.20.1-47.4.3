package net.kogasachan.kogasa_addiction_compat.compat;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaIdListMatcher;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.DiggerItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 通用装备兜底：给“没有继承原版武器/工具基类、但确实有装备数值”的物品用。
 *
 * <p>判定顺序（固定，不要随意调换）：
 * <ol>
 *   <li>攻击伤害 / 攻击速度属性；</li>
 *   <li>DiggerItem 或常见方块 destroy speed &gt; 1；</li>
 *   <li>配置列表 genericEquipment 内、有耐久、且不是 ArmorItem 的物品。</li>
 * </ol>
 * 护甲不参与通用武器路径，避免把护甲当武器；挖掘速度用 DiggerItem / 常见方块 destroy speed 兜底。
 */
public final class CompatGenericEquipment {

    private CompatGenericEquipment() {
    }

    public static boolean isGeneric(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Multimap<Attribute, AttributeModifier> original =
                stack.getItem().getDefaultAttributeModifiers(EquipmentSlot.MAINHAND);
        return isGeneric(stack, original);
    }

    /**
     * 事件里用 getOriginalModifiers()，避免递归触发属性事件。
     */
    public static boolean isGeneric(ItemStack stack, Multimap<Attribute, AttributeModifier> original) {
        if (!KogasaCompat.isCompatOnly(stack)) return false;

        if (hasAttackAttribute(original, Attributes.ATTACK_DAMAGE)
                || hasAttackAttribute(original, Attributes.ATTACK_SPEED)) {
            return true;
        }

        if (stack.getItem() instanceof DiggerItem) {
            return true;
        }
        if (stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1.0F) {
            return true;
        }

        if (stack.getItem() instanceof ArmorItem) {
            return false;
        }
        if (stack.getMaxDamage() <= 0) {
            return false;
        }

        ResourceLocation id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return KogasaIdListMatcher.matches(id, CompatConfig.GENERIC_EQUIPMENT.get());
    }

    private static boolean hasAttackAttribute(Multimap<Attribute, AttributeModifier> modifiers,
                                              Attribute attribute) {
        if (modifiers == null) return false;
        for (var entry : modifiers.entries()) {
            if (entry.getKey() == attribute) {
                return true;
            }
        }
        return false;
    }
}
