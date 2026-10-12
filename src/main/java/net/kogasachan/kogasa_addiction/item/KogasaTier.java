package net.kogasachan.kogasa_addiction.item;

import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

public class KogasaTier implements Tier {

    public static final KogasaTier INSTANCE = new KogasaTier();

    @Override
    public int getUses() {
        return 59;
    }                  // 木

    @Override
    public float getSpeed() {
        return 2.0F;
    }             // 木

    @Override
    public float getAttackDamageBonus() {
        return 0.0F;
    } // 木

    @Override
    public int getLevel() {
        return 0;
    }                  // 木

    @Override
    public int getEnchantmentValue() {
        return 30;
    }

    @Override
    public @NotNull Ingredient getRepairIngredient() {
        return Ingredient.of(ModItems.KOGASA_INGOT.get());
    }
}