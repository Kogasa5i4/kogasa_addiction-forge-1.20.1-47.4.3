package net.kogasachan.kogasa_addiction.item;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ToolAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class FumoItem extends PickaxeItem implements IKogasaEquipment {
    public FumoItem(Tier tier, int attackDamageModifier, float attackSpeedModifier, Properties properties) {
        super(tier, attackDamageModifier, attackSpeedModifier, properties);
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
        return KogasaEquipmentHelper.getDestroySpeed(stack, getTier().getSpeed());
    }

    @Override
    public boolean isCorrectToolForDrops(@NotNull ItemStack stack, @NotNull BlockState state) {
        return true;
    }

    @Override
    public boolean canPerformAction(@NotNull ItemStack stack, @NotNull ToolAction toolAction) {
        return true;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack,
                                @Nullable Level level,
                                @NotNull List<Component> tooltip,
                                @NotNull TooltipFlag flag) {
        tooltip.add(
                Component.translatable("tooltip.kogasa_addiction.fumo")
                        .withStyle(ChatFormatting.AQUA)
        );
    }
}
