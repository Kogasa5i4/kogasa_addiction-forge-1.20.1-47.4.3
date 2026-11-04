package net.kogasachan.kogasa_addiction_compat.compat;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 彩虹水晶。
 *
 * <p>用法：在物品栏界面里，**手持**彩虹水晶右键目标物品槽位即可“结合”——
 * 给目标物品打上兼容标记，此后它和本模组装备走同一套逻辑
 * （需要配置 {@code compat.enableCompatEquipment} 开启）。
 *
 * <p>把水晶丢进熔岩（或熔岩炼药锅）会按概率转化为随机能力锭，见 {@link RainbowLavaHandler}。
 */
public class RainbowCrystalItem extends Item {

    public RainbowCrystalItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.kogasa_addiction_compat.rainbow_crystal.tooltip")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.kogasa_addiction_compat.rainbow_crystal.tooltip2")
                .withStyle(ChatFormatting.DARK_GRAY));
    }
}
