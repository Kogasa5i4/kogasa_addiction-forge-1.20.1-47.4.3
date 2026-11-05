package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.ability.KogasaAbilityRegistry;
import net.kogasachan.kogasa_addiction.item.tool.*;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.jetbrains.annotations.NotNull;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KogasaAddiction.MODID);

    // 材料

    // ===== 能力锭：由能力注册表统一驱动（新增能力会自动生成对应锭，无需在此加注册代码） =====
    public static final RegistryObject<Item> KOGASA_INGOT = registerAbilityIngot(KogasaAbility.KOGASA);
    public static final RegistryObject<Item> AYA_INGOT = registerAbilityIngot(KogasaAbility.AYA);
    public static final RegistryObject<Item> CIRNO_INGOT = registerAbilityIngot(KogasaAbility.CIRNO);
    public static final RegistryObject<Item> KOISHI_INGOT = registerAbilityIngot(KogasaAbility.KOISHI);
    public static final RegistryObject<Item> TENSHI_INGOT = registerAbilityIngot(KogasaAbility.TENSHI);
    public static final RegistryObject<Item> SAKUYA_INGOT = registerAbilityIngot(KogasaAbility.SAKUYA);

    private static RegistryObject<Item> registerAbilityIngot(KogasaAbility ability) {
        RegistryObject<Item> ingot = ITEMS.register(
                ability.getId() + "_ingot",
                () -> new KogasaAbilityIngotItem(ability,
                        new Item.Properties().fireResistant().rarity(Rarity.EPIC))
        );
        KogasaAbilityRegistry.registerIngot(ability, ingot);
        return ingot;
    }

    // 手册（独立物品，右键打开帕秋莉手册）
    public static final RegistryObject<Item> KOGASA_MANUAL = ITEMS.register(
            "kogasa_manual",
            () -> new KogasaManualItem(new Item.Properties().fireResistant().stacksTo(1).rarity(Rarity.RARE)));

    // 工具/武器
    public static final RegistryObject<Item> KOGASA_SWORD = ITEMS.register(
            "kogasa_sword",
            () -> new KogasaSwordItem(KogasaTier.INSTANCE, 3, -2.4F,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> KOGASA_PICKAXE = ITEMS.register(
            "kogasa_pickaxe",
            () -> new KogasaPickaxeItem(KogasaTier.INSTANCE, 1, -2.8F,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> KOGASA_AXE = ITEMS.register(
            "kogasa_axe",
            () -> new KogasaAxeItem(KogasaTier.INSTANCE, 6.0F, -3.2F,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> KOGASA_SHOVEL = ITEMS.register(
            "kogasa_shovel",
            () -> new KogasaShovelItem(KogasaTier.INSTANCE, 1.5F, -3.0F,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> KOGASA_HOE = ITEMS.register(
            "kogasa_hoe",
            () -> new KogasaHoeItem(KogasaTier.INSTANCE, 0, -3.0F,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );

    // 护甲
    public static final RegistryObject<Item> TENSHI_HELMET = ITEMS.register(
            "tenshi_helmet",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.HELMET, new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> TENSHI_CHESTPLATE = ITEMS.register(
            "tenshi_chestplate",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.CHESTPLATE, new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> TENSHI_LEGGINGS = ITEMS.register(
            "tenshi_leggings",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.LEGGINGS, new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> TENSHI_BOOTS = ITEMS.register(
            "tenshi_boots",
            () -> new KogasaArmorItem(TenshiArmorMaterial.INSTANCE,
                    ArmorItem.Type.BOOTS, new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );

    // 远程
    public static final RegistryObject<Item> AYA_BOW = ITEMS.register(
            "aya_bow",
            () -> new AyaBowItem(new Item.Properties().fireResistant().durability(384).rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> AYA_CROSSBOW = ITEMS.register(
            "aya_crossbow",
            () -> new AyaCrossbowItem(new Item.Properties().fireResistant().durability(465).rarity(Rarity.EPIC))
    );
    public static final RegistryObject<Item> AYA_ARROW = ITEMS.register(
            "aya_arrow",
            () -> new AyaArrowItem(new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );

    // 咲夜飞刀
    public static final RegistryObject<Item> SAKUYA_KNIFE = ITEMS.register(
            "sakuya_knife",
            () -> new SakuyaKnifeItem(new Item.Properties().fireResistant().stacksTo(3).fireResistant().rarity(Rarity.EPIC))
    );

    // 损坏的咲夜飞刀
    public static final RegistryObject<Item> BROKEN_SAKUYA_KNIFE = ITEMS.register(
            "broken_sakuya_knife",
            () -> new Item(new Item.Properties().fireResistant().stacksTo(6).fireResistant().rarity(Rarity.EPIC)) {
                @Override
                public @NotNull Component getName(@NotNull ItemStack stack) {
                    return super.getName(stack).copy()
                            .withStyle(style -> style.withColor(ChatFormatting.GOLD));
                }
            }
    );

    // fumo
    public static final RegistryObject<Item> FUMO = ITEMS.register(
            "fumo",
            () -> new FumoItem(FumoTier.INSTANCE, 5, -2.0F,
                    new Item.Properties().fireResistant().rarity(Rarity.EPIC))
    );
}