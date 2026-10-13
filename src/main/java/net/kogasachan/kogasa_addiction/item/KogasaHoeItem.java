package net.kogasachan.kogasa_addiction.item;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

public class KogasaHoeItem extends HoeItem implements IKogasaEquipment {
    public KogasaHoeItem(Tier tier, int damage, float speed, Properties props) {
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

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (stack.getItem() instanceof IKogasaEquipment) {
            int level = KogasaEquipmentHelper.getDynamicHarvestLevel(
                    stack, this.getTier().getLevel());

            if (level > 6) {
                return true;
            }

            if (state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) return level >= 4;
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return level >= 3;
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return level >= 2;
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return level >= 1;
        }
        return super.isCorrectToolForDrops(stack, state);
    }
}
