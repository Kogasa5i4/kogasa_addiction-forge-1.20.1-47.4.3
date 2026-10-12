package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import java.util.List;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KogasaAddiction.MODID);

    // 材料
    public static final RegistryObject<Item> KOGASA_INGOT = ITEMS.register(
            "kogasa_ingot",
            () -> new Item(new Item.Properties().fireResistant()) {
                @Override
                public void appendHoverText(@NotNull ItemStack itemStack, @Nullable Level level, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
                    super.appendHoverText(itemStack, level, tooltip, flag);
                    tooltip.add(Component.translatable("tooltip.kogasa_addiction.kogasa_ingot.obtain"));
                }
            }
    );

    // 工具/武器
    public static final RegistryObject<Item> KOGASA_SWORD = ITEMS.register(
            "kogasa_sword",
            () -> new KogasaSwordItem(KogasaTier.INSTANCE, 3, -2.4F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_PICKAXE = ITEMS.register(
            "kogasa_pickaxe",
            () -> new KogasaPickaxeItem(KogasaTier.INSTANCE, 1, -2.8F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_AXE = ITEMS.register(
            "kogasa_axe",
            () -> new KogasaAxeItem(KogasaTier.INSTANCE, 6.0F, -3.2F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_SHOVEL = ITEMS.register(
            "kogasa_shovel",
            () -> new KogasaShovelItem(KogasaTier.INSTANCE, 1.5F, -3.0F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_HOE = ITEMS.register(
            "kogasa_hoe",
            () -> new KogasaHoeItem(KogasaTier.INSTANCE, 0, -3.0F,
                    new Item.Properties())
    );

    // 护甲
    public static final RegistryObject<Item> KOGASA_HELMET = ITEMS.register(
            "kogasa_helmet",
            () -> new KogasaArmorItem(KogasaArmorMaterial.INSTANCE,
                    ArmorItem.Type.HELMET, new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_CHESTPLATE = ITEMS.register(
            "kogasa_chestplate",
            () -> new KogasaArmorItem(KogasaArmorMaterial.INSTANCE,
                    ArmorItem.Type.CHESTPLATE, new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_LEGGINGS = ITEMS.register(
            "kogasa_leggings",
            () -> new KogasaArmorItem(KogasaArmorMaterial.INSTANCE,
                    ArmorItem.Type.LEGGINGS, new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_BOOTS = ITEMS.register(
            "kogasa_boots",
            () -> new KogasaArmorItem(KogasaArmorMaterial.INSTANCE,
                    ArmorItem.Type.BOOTS, new Item.Properties())
    );
}