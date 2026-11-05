package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.TierSortingRegistry;
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

    /**
     * 挖掘等级判定：强化次数可把有效等级抬到原版阶梯的任意一档（下界合金封顶），
     * 判定走 Forge 的 {@link TierSortingRegistry}（数据驱动，认全部 NEEDS tag，
     * 包括其它模组的），不再使用已弃用的 {@code Tier#getLevel()} 数字比较。
     */
    static boolean isCorrectToolForDrops(ItemStack stack, BlockState state, Tier tier,
                                         BooleanSupplier vanillaFallback) {
        if (net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper.isKogasaEquipment(stack)) {
            Tier effective = KogasaEquipmentHelper.getDynamicHarvestTier(stack, tier);
            return TierSortingRegistry.isCorrectTierForDrops(effective, state);
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
}
