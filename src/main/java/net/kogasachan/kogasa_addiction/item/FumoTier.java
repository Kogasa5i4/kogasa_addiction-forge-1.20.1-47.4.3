package net.kogasachan.kogasa_addiction.item;

import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

public class FumoTier implements Tier {

    public static final FumoTier INSTANCE = new FumoTier();

    @Override
    public int getUses() {
        return 9961;
    }

    @Override
    public float getSpeed() {
        return 9.0F;
    }

    @Override
    public float getAttackDamageBonus() {
        return 4.0F;
    }

    @Override
    public int getLevel() {
        return 60;
    }

    @Override
    public int getEnchantmentValue() {
        return 60;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        return Ingredient.of(Items.ENCHANTING_TABLE);
    }
}