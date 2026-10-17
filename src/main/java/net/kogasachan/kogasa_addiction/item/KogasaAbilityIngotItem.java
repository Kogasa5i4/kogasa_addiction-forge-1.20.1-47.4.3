package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KogasaAbilityIngotItem extends Item {

    private final KogasaAbility ability;

    public KogasaAbilityIngotItem(KogasaAbility ability, Properties properties) {
        super(properties);
        this.ability = ability;
    }

    public KogasaAbility getAbility() {
        return ability;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, @NotNull TooltipFlag flag) {
        // 1. 能力名
        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.ability",
                        ability.getDisplayName())
                .withStyle(ChatFormatting.AQUA));

        // 2. 能力描述
        tooltip.add(ability.getDescription()
                .copy()
                .withStyle(ChatFormatting.GRAY));

        // 3. 空行分隔
        tooltip.add(Component.empty());

        // 4. 获取方式
        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.obtain.header")
                .withStyle(ChatFormatting.YELLOW));
        for (String key : ability.getObtainKeys()) {
            tooltip.add(Component.translatable(key).withStyle(ChatFormatting.GRAY));
        }

        // 5. 空行 + 使用提示
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.usage")
                .withStyle(ChatFormatting.DARK_GRAY));

        super.appendHoverText(stack, level, tooltip, flag);
    }
}