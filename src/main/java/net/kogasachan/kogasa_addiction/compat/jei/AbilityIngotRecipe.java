package net.kogasachan.kogasa_addiction.compat.jei;

import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.effect.AbilityEffect;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * JEI 能力锭信息分类的一条“配方”。
 */
public record AbilityIngotRecipe(
        KogasaAbility ability,
        ItemStack ingot,
        List<ItemStack> outputs,
        List<AbilityEffect> effects
) {
}
