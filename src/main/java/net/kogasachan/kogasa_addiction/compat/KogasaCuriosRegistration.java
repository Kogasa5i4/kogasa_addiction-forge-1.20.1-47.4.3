package net.kogasachan.kogasa_addiction.compat;

import com.google.common.collect.Multimap;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotContext;
import top.theillusivec4.curios.api.type.capability.ICurioItem;

import java.util.UUID;

/**
 * 只在 Curios 已加载时由 KogasaCuriosCompat 调用。
 */
final class KogasaCuriosRegistration {

    private KogasaCuriosRegistration() {
    }

    private static final ICurioItem CURIO = new ICurioItem() {
        @Override
        public Multimap<Attribute, AttributeModifier> getAttributeModifiers(
                SlotContext context, UUID uuid, ItemStack stack) {
            Item item = stack.getItem();
            EquipmentSlot slot = item instanceof ArmorItem armor
                    ? armor.getEquipmentSlot()
                    : EquipmentSlot.MAINHAND;
            return KogasaEquipmentHelper.getAttributeModifiers(item, slot, stack);
        }
    };

    static void register() {
        for (Item item : new Item[]{
                ModItems.KOGASA_SWORD.get(), ModItems.KOGASA_PICKAXE.get(),
                ModItems.KOGASA_AXE.get(), ModItems.KOGASA_SHOVEL.get(),
                ModItems.KOGASA_HOE.get(), ModItems.FUMO.get(),
                ModItems.SAKUYA_KNIFE.get(), ModItems.AYA_BOW.get(),
                ModItems.AYA_CROSSBOW.get(), ModItems.TENSHI_HELMET.get(),
                ModItems.TENSHI_CHESTPLATE.get(), ModItems.TENSHI_LEGGINGS.get(),
                ModItems.TENSHI_BOOTS.get()
        }) {
            CuriosApi.registerCurio(item, CURIO);
        }
    }
}
