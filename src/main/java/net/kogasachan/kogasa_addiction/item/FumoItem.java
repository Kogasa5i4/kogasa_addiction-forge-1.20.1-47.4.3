package net.kogasachan.kogasa_addiction.item;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
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
    public @NotNull Component getName(@NotNull ItemStack stack) {
        return super.getName(stack).copy()
                .withStyle(style -> style.withColor(ChatFormatting.BLUE));
    }

    @Override
    public @NotNull Rarity getRarity(@NotNull ItemStack pStack) {
        return Rarity.EPIC;
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

    /**
     * 万能工具：接受"镐 / 剑 / 斧 / 锹 / 锄"范围内的任意附魔。
     *
     * <p>Forge 把 {@code Enchantment#canEnchant(ItemStack)} 委托到
     * {@code ItemStack#canApplyAtEnchantingTable} → 物品的这个方法，
     * 所以覆写一处即可同时覆盖三条路径：
     * <ul>
     *   <li>附魔台随机附魔的候选过滤；</li>
     *   <li>铁砧上附魔书合并时的兼容判定；</li>
     *   <li>本模组的成长附魔（{@code KogasaGrowthHandler#pickRandomNewEnchantment}）。</li>
     * </ul>
     *
     * <p>判定用"原版五类工具样板"而不是 fumo 自身：样板走的是原版默认实现
     * （{@code category.canEnchant(item)}），不会递归回本方法。
     * 顺带说明：耐久类（Unbreaking / Mending）、消失诅咒等"任意可损坏物品"类附魔
     * 铁制样板同样能接受，因此一并保留。
     */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return enchantment.canEnchant(new ItemStack(Items.IRON_PICKAXE))
                || enchantment.canEnchant(new ItemStack(Items.IRON_SWORD))
                || enchantment.canEnchant(new ItemStack(Items.IRON_AXE))
                || enchantment.canEnchant(new ItemStack(Items.IRON_SHOVEL))
                || enchantment.canEnchant(new ItemStack(Items.IRON_HOE));
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
