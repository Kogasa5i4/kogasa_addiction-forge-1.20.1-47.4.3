package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

/**
 * 本模组"可成长装备"的标记接口。
 *
 * <p>只保留三样东西：
 * <ul>
 *   <li>标记本身（各处用 {@code instanceof IKogasaEquipment} 收敛类型）；</li>
 *   <li>共用的挖掘等级判定 {@link #isCorrectToolForDrops}；</li>
 *   <li>所有装备都一致的名字颜色与稀有度 —— 用 {@code default} 实现，
 *       具体工具类不必再各写一遍。</li>
 * </ul>
 *
 * <p>原来的 5 个实现类（{@code KogasaSwordItem} 等）曾作为嵌套类写在接口里，
 * 那是隐式 {@code public static}，既不方便测试也不好检索；现已移到
 * {@code net.kogasachan.kogasa_addiction.item.tool} 包。
 */
public interface IKogasaEquipment {

    static boolean isCorrectToolForDrops(ItemStack stack, BlockState state, int tierLevel,
                                         BooleanSupplier vanillaFallback) {
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

    /**
     * 所有本模组装备统一用金色名字。
     *
     * <p>等价于原来的 {@code super.getName(stack).copy().withStyle(GOLD)}：
     * 这些物品都没有覆写 {@code Item#getName(ItemStack)} 的默认实现
     * （默认实现就是 {@code translatable(getDescriptionId())}），
     * 而每次调用都新建组件，不存在改动共享组件的问题。
     */
    default @NotNull Component getName(@NotNull ItemStack stack) {
        return Component.translatable(stack.getDescriptionId()).withStyle(ChatFormatting.GOLD);
    }

    /** 所有本模组装备统一为 EPIC 稀有度 */
    default @NotNull Rarity getRarity(@NotNull ItemStack stack) {
        return Rarity.EPIC;
    }
}
