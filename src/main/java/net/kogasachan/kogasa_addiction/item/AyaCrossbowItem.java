package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;

public class AyaCrossbowItem extends CrossbowItem implements IKogasaEquipment {

    public AyaCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return KogasaEquipmentHelper.getMaxDamage(stack, super.getMaxDamage(stack));
    }
}