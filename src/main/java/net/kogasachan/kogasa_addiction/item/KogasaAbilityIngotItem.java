package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
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
    public void appendHoverText(ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.ability",
                        ability.getDisplayName())
                .withStyle(ChatFormatting.AQUA));

        tooltip.add(ability.getDescription()
                .copy()
                .withStyle(ChatFormatting.GRAY));

        // kogasa 锭额外提示高炉来源
        if (ability == KogasaAbility.KOGASA) {
            tooltip.add(Component.translatable(
                            "tooltip.kogasa_addiction.kogasa_ingot.obtain")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.usage")
                .withStyle(ChatFormatting.DARK_GRAY));

        super.appendHoverText(stack, level, tooltip, flag);
    }
}
