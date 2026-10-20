package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.critereon.*;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemEntityPropertyCondition;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.ForgeAdvancementProvider;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

public class ModAdvancementGenerator implements ForgeAdvancementProvider.AdvancementGenerator {

    @Override
    public void generate(HolderLookup.@NotNull Provider registries, @NotNull Consumer<Advancement> writer, @NotNull ExistingFileHelper existingFileHelper) {
        ResourceLocation modRoot = ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "root");

        // ===== 0. 模组根进度（独立标签页） =====
        Advancement root = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOGASA_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.root.title"),
                        Component.translatable("advancements.kogasa_addiction.root.description"),
                        ResourceLocation.fromNamespaceAndPath("minecraft", "textures/block/deepslate_bricks.png"),
                        FrameType.TASK, false, false, false
                ))
                .addCriterion("has_blast_furnace", InventoryChangeTrigger.TriggerInstance.hasItems(Items.BLAST_FURNACE))
                .save(writer, modRoot, existingFileHelper);

        // ===== 1. 获得小伞锭 =====
        Advancement obtain_kogasa_ingot = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(Items.BLAST_FURNACE),
                        Component.translatable("advancements.kogasa_addiction.obtain_kogasa_ingot.title"),
                        Component.translatable("advancements.kogasa_addiction.obtain_kogasa_ingot.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(root)
                .addCriterion("obtain_kogasa_ingot", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_INGOT.get()))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "obtain_kogasa_ingot"), existingFileHelper);

        // ===== 2. 幻想乡元素周期律：五种能力锭 =====
        Advancement ingot_collector = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.TENSHI_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.ingot_collector.title"),
                        Component.translatable("advancements.kogasa_addiction.ingot_collector.description"),
                        null, FrameType.GOAL, true, true, false
                ))
                .parent(obtain_kogasa_ingot)
                .addCriterion("has_kogasa", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_INGOT.get()))
                .addCriterion("has_aya", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.AYA_INGOT.get()))
                .addCriterion("has_cirno", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.CIRNO_INGOT.get()))
                .addCriterion("has_koishi", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOISHI_INGOT.get()))
                .addCriterion("has_tenshi", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.TENSHI_INGOT.get()))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "ingot_collector"), existingFileHelper);

        // ===== 3. 初出高炉：获得任意模组装备 =====
        Advancement first_gear = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOGASA_SWORD.get()),
                        Component.translatable("advancements.kogasa_addiction.first_gear.title"),
                        Component.translatable("advancements.kogasa_addiction.first_gear.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(obtain_kogasa_ingot)
                .addCriterion("first_gear", InventoryChangeTrigger.TriggerInstance.hasItems(
                        ModItems.KOGASA_SWORD.get(), ModItems.KOGASA_PICKAXE.get(),
                        ModItems.KOGASA_AXE.get(), ModItems.KOGASA_SHOVEL.get(), ModItems.KOGASA_HOE.get(),
                        ModItems.TENSHI_HELMET.get(), ModItems.TENSHI_CHESTPLATE.get(),
                        ModItems.TENSHI_LEGGINGS.get(), ModItems.TENSHI_BOOTS.get()
                ))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "first_gear"), existingFileHelper);

        // ===== 4. 初级奉献：获得小伞锄 =====
        Advancement obtain_hoe = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOGASA_HOE.get()),
                        Component.translatable("advancements.kogasa_addiction.obtain_hoe.title"),
                        Component.translatable("advancements.kogasa_addiction.obtain_hoe.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(ResourceLocation.fromNamespaceAndPath("minecraft", "husbandry/obtain_netherite_hoe"))
                .addCriterion("obtain_hoe", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_HOE.get()))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "obtain_hoe"), existingFileHelper);

        // ===== 5. 锻造能手：收集模组工具、武器与天子护甲 =====
        Advancement full_set = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.TENSHI_CHESTPLATE.get()),
                        Component.translatable("advancements.kogasa_addiction.full_set.title"),
                        Component.translatable("advancements.kogasa_addiction.full_set.description"),
                        null, FrameType.GOAL, true, true, false
                ))
                .parent(first_gear)
                .addCriterion("sword", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_SWORD.get()))
                .addCriterion("pickaxe", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_PICKAXE.get()))
                .addCriterion("axe", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_AXE.get()))
                .addCriterion("shovel", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_SHOVEL.get()))
                .addCriterion("hoe", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.KOGASA_HOE.get()))
                .addCriterion("helmet", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.TENSHI_HELMET.get()))
                .addCriterion("chestplate", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.TENSHI_CHESTPLATE.get()))
                .addCriterion("leggings", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.TENSHI_LEGGINGS.get()))
                .addCriterion("boots", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.TENSHI_BOOTS.get()))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "full_set"), existingFileHelper);

        // ===== 6. 求“升”之路：第一次锻造升级 =====
        Advancement first_upgrade = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(Items.ANVIL),
                        Component.translatable("advancements.kogasa_addiction.first_upgrade.title"),
                        Component.translatable("advancements.kogasa_addiction.first_upgrade.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(first_gear)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "first_upgrade"), existingFileHelper);

        // ===== 7. 最强？！：获得第一个能力 =====
        Advancement first_ability = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOGASA_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.first_ability.title"),
                        Component.translatable("advancements.kogasa_addiction.first_ability.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(ingot_collector)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "first_ability"), existingFileHelper);

        // ===== 8. 这是个问题：拥有两种不同能力 =====
        Advancement two_abilities = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOISHI_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.two_abilities.title"),
                        Component.translatable("advancements.kogasa_addiction.two_abilities.description"),
                        null, FrameType.GOAL, true, true, false
                ))
                .parent(first_ability)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "two_abilities"), existingFileHelper);

        // ===== 9. 锻造高手：使用 6 个强化槽 =====
        Advancement six_slots = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(Items.CHIPPED_ANVIL),
                        Component.translatable("advancements.kogasa_addiction.six_slots.title"),
                        Component.translatable("advancements.kogasa_addiction.six_slots.description"),
                        null, FrameType.GOAL, true, true, false
                ))
                .parent(first_upgrade)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "six_slots"), existingFileHelper);

        // ===== 10. 你是怎么做到的（隐藏）：用完 7 个强化槽 =====
        Advancement seven_slots = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(Items.DAMAGED_ANVIL),
                        Component.translatable("advancements.kogasa_addiction.seven_slots.title"),
                        Component.translatable("advancements.kogasa_addiction.seven_slots.description"),
                        null, FrameType.CHALLENGE, true, true, true
                ))
                .parent(six_slots)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .rewards(AdvancementRewards.Builder.experience(500))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "seven_slots"), existingFileHelper);

        // ===== 11. 阅历渐长：开始累计成长值 =====
        Advancement start_growth = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOGASA_PICKAXE.get()),
                        Component.translatable("advancements.kogasa_addiction.start_growth.title"),
                        Component.translatable("advancements.kogasa_addiction.start_growth.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(obtain_kogasa_ingot)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "start_growth"), existingFileHelper);

        // ===== 12. 炉火纯青：成长值 >= 100% =====
        Advancement max_growth = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOGASA_AXE.get()),
                        Component.translatable("advancements.kogasa_addiction.max_growth.title"),
                        Component.translatable("advancements.kogasa_addiction.max_growth.description"),
                        null, FrameType.CHALLENGE, true, true, false
                ))
                .parent(start_growth)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .rewards(AdvancementRewards.Builder.experience(50))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "max_growth"), existingFileHelper);

        // ===== 13. 风神少女：弩的成长值足够触发自动装填 =====
        Advancement wind_god_girl = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.AYA_CROSSBOW.get()),
                        Component.translatable("advancements.kogasa_addiction.wind_god_girl.title"),
                        Component.translatable("advancements.kogasa_addiction.wind_god_girl.description"),
                        null, FrameType.GOAL, true, true, false
                ))
                .parent(start_growth)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "wind_god_girl"), existingFileHelper);

        // ===== 14. 月战老兵：不再消耗箭矢 =====
        Advancement moon_veteran = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(Items.ARROW),
                        Component.translatable("advancements.kogasa_addiction.moon_veteran.title"),
                        Component.translatable("advancements.kogasa_addiction.moon_veteran.description"),
                        null, FrameType.CHALLENGE, true, true, false
                ))
                .parent(wind_god_girl)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .rewards(AdvancementRewards.Builder.experience(100))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "moon_veteran"), existingFileHelper);

        // ===== 15. 传统的幻想记者：新闻值 >= 7.23% =====
        Advancement reporter_novice = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.AYA_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.reporter_novice.title"),
                        Component.translatable("advancements.kogasa_addiction.reporter_novice.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(first_ability)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "reporter_novice"), existingFileHelper);

        // ===== 16. 文花帖：新闻值 >= 100% =====
        Advancement reporter_master = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.AYA_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.reporter_master.title"),
                        Component.translatable("advancements.kogasa_addiction.reporter_master.description"),
                        null, FrameType.CHALLENGE, true, true, false
                ))
                .parent(reporter_novice)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .rewards(AdvancementRewards.Builder.experience(723))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "reporter_master"), existingFileHelper);

        // ===== 17. 我复活辣！：触发琪露诺的复活 =====
        Advancement cirno_revive = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.CIRNO_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.cirno_revive.title"),
                        Component.translatable("advancements.kogasa_addiction.cirno_revive.description"),
                        null, FrameType.GOAL, true, true, false
                ))
                .parent(first_ability)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "cirno_revive"), existingFileHelper);

        // ===== 18. 这样就能吓到人了！：工具攻击或护甲值 > 30 =====
        Advancement high_stats = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.KOGASA_SWORD.get()),
                        Component.translatable("advancements.kogasa_addiction.high_stats.title"),
                        Component.translatable("advancements.kogasa_addiction.high_stats.description"),
                        null, FrameType.CHALLENGE, true, true, false
                ))
                .parent(first_ability)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .rewards(AdvancementRewards.Builder.experience(611))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "high_stats"), existingFileHelper);

        // ===== 19. 幻想风靡：获得全套文文装备（弓、弩、箭） =====
        Advancement aya_full_set = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.AYA_BOW.get()),
                        Component.translatable("advancements.kogasa_addiction.aya_full_set.title"),
                        Component.translatable("advancements.kogasa_addiction.aya_full_set.description"),
                        null, FrameType.GOAL, true, true, false
                ))
                .parent(first_gear)
                .addCriterion("has_bow", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.AYA_BOW.get()))
                .addCriterion("has_crossbow", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.AYA_CROSSBOW.get()))
                .addCriterion("has_arrow", InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.AYA_ARROW.get()))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "aya_full_set"), existingFileHelper);

        // ===== 20. 河童科技：弓/弩上拥有特殊弹药 =====
        Advancement kappa_tech = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.AYA_CROSSBOW.get()),
                        Component.translatable("advancements.kogasa_addiction.kappa_tech.title"),
                        Component.translatable("advancements.kogasa_addiction.kappa_tech.description"),
                        null, FrameType.TASK, true, true, false
                ))
                .parent(aya_full_set)
                .addCriterion("impossible", new ImpossibleTrigger.TriggerInstance())
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "kappa_tech"), existingFileHelper);

        // ===== 21. ⑨⑨81：获得64个琪露诺锭（隐藏） =====
        ItemPredicate has64Cirno = ItemPredicate.Builder.item()
                .of(ModItems.CIRNO_INGOT.get())
                .withCount(MinMaxBounds.Ints.atLeast(64))
                .build();

        Advancement cirno_64_ingots = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.CIRNO_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.cirno_64_ingots.title"),
                        Component.translatable("advancements.kogasa_addiction.cirno_64_ingots.description"),
                        null, FrameType.CHALLENGE, true, true, true // 隐藏
                ))
                .parent(ingot_collector)
                .addCriterion("has_64_cirno", InventoryChangeTrigger.TriggerInstance.hasItems(has64Cirno))
                .rewards(AdvancementRewards.Builder.experience(81))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "cirno_64_ingots"), existingFileHelper);

        // ===== 22. 天之女、地之神、人之心：在 300 格以上获取天子锭（隐藏） =====
        LootItemCondition heightCondition = LootItemEntityPropertyCondition
                .hasProperties(LootContext.EntityTarget.THIS, EntityPredicate.Builder.entity()
                        .located(LocationPredicate.Builder.location()
                                .setY(MinMaxBounds.Doubles.atLeast(300)).build()
                        ))
                .build();

        ContextAwarePredicate playerPredicate = ContextAwarePredicate.create(heightCondition);

        ItemPredicate[] tenshiIngotPredicate = new ItemPredicate[]{ItemPredicate.Builder.item()
                .of(ModItems.TENSHI_INGOT.get())
                .build()};

        InventoryChangeTrigger.TriggerInstance tenshiHeightTrigger =
                new InventoryChangeTrigger.TriggerInstance(
                        playerPredicate,
                        MinMaxBounds.Ints.ANY,
                        MinMaxBounds.Ints.ANY,
                        MinMaxBounds.Ints.ANY,
                        tenshiIngotPredicate
                );

        Advancement tenshi_height_challenge = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.TENSHI_INGOT.get()),
                        Component.translatable("advancements.kogasa_addiction.tenshi_height_challenge.title"),
                        Component.translatable("advancements.kogasa_addiction.tenshi_height_challenge.description"),
                        null, FrameType.CHALLENGE, true, true, true
                ))
                .parent(ResourceLocation.fromNamespaceAndPath("minecraft", "adventure/trade_at_world_height")) // 星际商人
                .addCriterion("get_tenshi_ingot_at_height", tenshiHeightTrigger)
                .rewards(AdvancementRewards.Builder.experience(1050))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "tenshi_height_challenge"), existingFileHelper);

        // ===== 23. fumofumo：获得 Fumo（隐藏） =====
        Advancement fumofumo = Advancement.Builder.advancement()
                .display(new DisplayInfo(
                        new ItemStack(ModItems.FUMO.get()),
                        Component.translatable("advancements.kogasa_addiction.fumofumo.title"),
                        Component.translatable("advancements.kogasa_addiction.fumofumo.description"),
                        null, FrameType.CHALLENGE, true, true, true
                ))
                .parent(first_gear)
                .addCriterion("obtain_fumo",
                        InventoryChangeTrigger.TriggerInstance.hasItems(ModItems.FUMO.get()))
                .rewards(AdvancementRewards.Builder.experience(99))
                .save(writer, ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "fumofumo"), existingFileHelper);
    }
}