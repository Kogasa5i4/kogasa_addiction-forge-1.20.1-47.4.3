package net.kogasachan.kogasa_addiction.item;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class KogasaSwordItem extends SwordItem implements IKogasaEquipment {
    public KogasaSwordItem(Tier tier, int damage, float speed, Properties props) {
        super(tier, damage, speed, props);
    }

    @Override
    public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
            EquipmentSlot slot, ItemStack stack) {
        return KogasaEquipmentHelper.getAttributeModifiers(this, slot, stack);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return KogasaEquipmentHelper.getMaxDamage(stack, super.getMaxDamage(stack));
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        return KogasaEquipmentHelper.getDestroySpeed(stack, super.getDestroySpeed(stack, state));
    }
}
