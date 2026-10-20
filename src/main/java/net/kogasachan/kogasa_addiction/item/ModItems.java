package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KogasaAddiction.MODID);

    // 材料

    // ===== 能力锭 =====
    public static final RegistryObject<Item> KOGASA_INGOT = ITEMS.register(
            "kogasa_ingot",
            () -> new KogasaAbilityIngotItem(KogasaAbility.KOGASA,
                    new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Item> AYA_INGOT = ITEMS.register(
            "aya_ingot",
            () -> new KogasaAbilityIngotItem(KogasaAbility.AYA,
                    new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Item> CIRNO_INGOT = ITEMS.register(
            "cirno_ingot",
            () -> new KogasaAbilityIngotItem(KogasaAbility.CIRNO,
                    new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Item> KOISHI_INGOT = ITEMS.register(
            "koishi_ingot",
            () -> new KogasaAbilityIngotItem(KogasaAbility.KOISHI,
                    new Item.Properties().fireResistant())
    );

    public static final RegistryObject<Item> TENSHI_INGOT = ITEMS.register(
            "tenshi_ingot",
            () -> new KogasaAbilityIngotItem(KogasaAbility.TENSHI,
                    new Item.Properties().fireResistant())
    );

    // 工具/武器
    public static final RegistryObject<Item> KOGASA_SWORD = ITEMS.register(
            "kogasa_sword",
            () -> new IKogasaEquipment.KogasaSwordItem(KogasaTier.INSTANCE, 3, -2.4F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_PICKAXE = ITEMS.register(
            "kogasa_pickaxe",
            () -> new IKogasaEquipment.KogasaPickaxeItem(KogasaTier.INSTANCE, 1, -2.8F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_AXE = ITEMS.register(
            "kogasa_axe",
            () -> new IKogasaEquipment.KogasaAxeItem(KogasaTier.INSTANCE, 6.0F, -3.2F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_SHOVEL = ITEMS.register(
            "kogasa_shovel",
            () -> new IKogasaEquipment.KogasaShovelItem(KogasaTier.INSTANCE, 1.5F, -3.0F,
                    new Item.Properties())
    );
    public static final RegistryObject<Item> KOGASA_HOE = ITEMS.register(
            "kogasa_hoe",
            () -> new IKogasaEquipment.KogasaHoeItem(KogasaTier.INSTANCE, 0, -3.0F,
                    new Item.Properties())
    );

    // 护甲
    public static final RegistryObject<Item> TENSHI_HELMET = ITEMS.register(
            "tenshi_helmet",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.HELMET, new Item.Properties())
    );
    public static final RegistryObject<Item> TENSHI_CHESTPLATE = ITEMS.register(
            "tenshi_chestplate",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.CHESTPLATE, new Item.Properties())
    );
    public static final RegistryObject<Item> TENSHI_LEGGINGS = ITEMS.register(
            "tenshi_leggings",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.LEGGINGS, new Item.Properties())
    );
    public static final RegistryObject<Item> TENSHI_BOOTS = ITEMS.register(
            "tenshi_boots",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.BOOTS, new Item.Properties())
    );

    // 远程
    public static final RegistryObject<Item> AYA_BOW = ITEMS.register(
            "aya_bow",
            () -> new AyaBowItem(new Item.Properties().durability(384))
    );
    public static final RegistryObject<Item> AYA_CROSSBOW = ITEMS.register(
            "aya_crossbow",
            () -> new AyaCrossbowItem(new Item.Properties().durability(465))
    );
    public static final RegistryObject<Item> AYA_ARROW = ITEMS.register(
            "aya_arrow",
            () -> new AyaArrowItem(new Item.Properties())
    );

    // fumo
    public static final RegistryObject<Item> FUMO = ITEMS.register(
            "fumo",
            () -> new FumoItem(FumoTier.INSTANCE, 5, -2.0F,
                    new Item.Properties())
    );
}