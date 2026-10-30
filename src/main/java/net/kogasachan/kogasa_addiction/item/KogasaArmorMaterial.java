package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.NotNull;

public class KogasaArmorMaterial {

    public static final ArmorMaterial INSTANCE = new ArmorMaterial() {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            // 基础耐久（皮革档）：头部 55，胸甲 80，护腿 75，靴子 65
            return switch (type) {
                case HELMET -> 55;
                case CHESTPLATE -> 80;
                case LEGGINGS -> 75;
                case BOOTS -> 65;
            };
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return switch (type) {
                case HELMET, BOOTS -> 1;
                case CHESTPLATE -> 3;
                case LEGGINGS -> 2;
            };
        }

        @Override
        public int getEnchantmentValue() {
            return 30;
        }

        @Override
        public @NotNull SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_DIAMOND;
        }

        @Override
        public @NotNull Ingredient getRepairIngredient() {
            return Ingredient.of(ModItems.KOGASA_INGOT.get());
        }

        @Override
        public @NotNull String getName() {
            return KogasaAddiction.MODID + ":kogasa";
        }

        @Override
        public float getToughness() {
            return 2.0F;
        }

        @Override
        public float getKnockbackResistance() {
            return 0.05F;
        }
    };
}