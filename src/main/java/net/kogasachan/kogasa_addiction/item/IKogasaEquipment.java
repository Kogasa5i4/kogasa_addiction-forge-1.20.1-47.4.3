package net.kogasachan.kogasa_addiction.item;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public interface IKogasaEquipment {

    static boolean isCorrectToolForDrops(ItemStack stack, BlockState state, int tierLevel, BooleanSupplier vanillaFallback) {
        if (stack.getItem() instanceof IKogasaEquipment) {
            int level = KogasaEquipmentHelper.getDynamicHarvestLevel(stack, tierLevel);
            if (level > 6) return true;
            if (state.is(Tags.Blocks.NEEDS_NETHERITE_TOOL)) return level >= 4;
            if (state.is(BlockTags.NEEDS_DIAMOND_TOOL)) return level >= 3;
            if (state.is(BlockTags.NEEDS_IRON_TOOL)) return level >= 2;
            if (state.is(BlockTags.NEEDS_STONE_TOOL)) return level >= 1;
            if (state.is(Tags.Blocks.NEEDS_WOOD_TOOL) || state.is(Tags.Blocks.NEEDS_GOLD_TOOL)) return level >= 0;
        }
        return vanillaFallback.getAsBoolean();
    }

    class KogasaSwordItem extends SwordItem implements IKogasaEquipment {
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

        @Override
        public boolean isCorrectToolForDrops(ItemStack stack, @NotNull BlockState state) {
            return IKogasaEquipment.isCorrectToolForDrops(
                    stack, state, this.getTier().getLevel(),
                    () -> super.isCorrectToolForDrops(stack, state));
        }
    }

    class KogasaAxeItem extends AxeItem implements IKogasaEquipment {
        public KogasaAxeItem(Tier tier, float damage, float speed, Properties props) {
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
        public boolean isCorrectToolForDrops(@NotNull ItemStack stack, @NotNull BlockState state) {
            return IKogasaEquipment.isCorrectToolForDrops(
                    stack, state, this.getTier().getLevel(),
                    () -> super.isCorrectToolForDrops(stack, state));
        }
    }

    class KogasaPickaxeItem extends PickaxeItem implements IKogasaEquipment {
        public KogasaPickaxeItem(Tier tier, int damage, float speed, Properties props) {
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
        public boolean isCorrectToolForDrops(@NotNull ItemStack stack, @NotNull BlockState state) {
            return IKogasaEquipment.isCorrectToolForDrops(
                    stack, state, this.getTier().getLevel(),
                    () -> super.isCorrectToolForDrops(stack, state));
        }
    }

    class KogasaShovelItem extends ShovelItem implements IKogasaEquipment {
        public KogasaShovelItem(Tier tier, float damage, float speed, Properties props) {
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
        public boolean isCorrectToolForDrops(@NotNull ItemStack stack, @NotNull BlockState state) {
            return IKogasaEquipment.isCorrectToolForDrops(
                    stack, state, this.getTier().getLevel(),
                    () -> super.isCorrectToolForDrops(stack, state));
        }
    }

    class KogasaHoeItem extends HoeItem implements IKogasaEquipment {
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
        public boolean isCorrectToolForDrops(@NotNull ItemStack stack, @NotNull BlockState state) {
            return IKogasaEquipment.isCorrectToolForDrops(
                    stack, state, this.getTier().getLevel(),
                    () -> super.isCorrectToolForDrops(stack, state));
        }
    }
}