package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KogasaAddiction.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // 1. 能力锭
        basicItem(ModItems.KOGASA_INGOT.get());
        basicItem(ModItems.AYA_INGOT.get());
        basicItem(ModItems.CIRNO_INGOT.get());
        basicItem(ModItems.KOISHI_INGOT.get());
        basicItem(ModItems.TENSHI_INGOT.get());
        basicItem(ModItems.SAKUYA_INGOT.get());

        // 2. 工具与武器
        withExistingParent("kogasa_sword", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/kogasa_sword"));
        withExistingParent("kogasa_pickaxe", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/kogasa_pickaxe"));
        withExistingParent("kogasa_axe", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/kogasa_axe"));
        withExistingParent("kogasa_shovel", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/kogasa_shovel"));
        withExistingParent("kogasa_hoe", mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/kogasa_hoe"));

        // 3. 护甲
        basicItem(ModItems.TENSHI_HELMET.get());
        basicItem(ModItems.TENSHI_CHESTPLATE.get());
        basicItem(ModItems.TENSHI_LEGGINGS.get());
        basicItem(ModItems.TENSHI_BOOTS.get());

        // 4. 远程武器和箭矢
        basicItem(ModItems.AYA_ARROW.get());
        registerBow();
        registerCrossbow();

        // 5. 咲夜飞刀
        basicItem(ModItems.SAKUYA_KNIFE.get());
        basicItem(ModItems.BROKEN_SAKUYA_KNIFE.get());

        // 6. Fumo
        basicItem(ModItems.FUMO.get());
    }

    // ==================== 弓 ====================

    private void registerBow() {
        // 拉弓状态子模型
        withExistingParent("aya_bow_pulling_0", mcLoc("item/bow_pulling_0"))
                .texture("layer0", modLoc("item/aya_bow_pulling_0"));
        withExistingParent("aya_bow_pulling_1", mcLoc("item/bow_pulling_1"))
                .texture("layer0", modLoc("item/aya_bow_pulling_1"));
        withExistingParent("aya_bow_pulling_2", mcLoc("item/bow_pulling_2"))
                .texture("layer0", modLoc("item/aya_bow_pulling_2"));

        // 主模型
        ItemModelBuilder bow = withExistingParent("aya_bow", mcLoc("item/bow"))
                .texture("layer0", modLoc("item/aya_bow"));

        bow.override()
                .predicate(mcLoc("pulling"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_bow_pulling_0")))
                .end();
        bow.override()
                .predicate(mcLoc("pull"), 0.65F)
                .predicate(mcLoc("pulling"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_bow_pulling_1")))
                .end();
        bow.override()
                .predicate(mcLoc("pull"), 0.9F)
                .predicate(mcLoc("pulling"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_bow_pulling_2")))
                .end();
    }

    // ==================== 弩 ====================

    private void registerCrossbow() {
        // 装填状态子模型
        withExistingParent("aya_crossbow_pulling_0", mcLoc("item/crossbow_pulling_0"))
                .texture("layer0", modLoc("item/aya_crossbow_pulling_0"));
        withExistingParent("aya_crossbow_pulling_1", mcLoc("item/crossbow_pulling_1"))
                .texture("layer0", modLoc("item/aya_crossbow_pulling_1"));
        withExistingParent("aya_crossbow_pulling_2", mcLoc("item/crossbow_pulling_2"))
                .texture("layer0", modLoc("item/aya_crossbow_pulling_2"));
        withExistingParent("aya_crossbow_arrow", mcLoc("item/crossbow_arrow"))
                .texture("layer0", modLoc("item/aya_crossbow_arrow"));
        withExistingParent("aya_crossbow_firework", mcLoc("item/crossbow_firework"))
                .texture("layer0", modLoc("item/aya_crossbow_firework"));

        // 主模型
        ItemModelBuilder crossbow = withExistingParent("aya_crossbow", mcLoc("item/crossbow"))
                .texture("layer0", modLoc("item/aya_crossbow"));

        // 装填中
        crossbow.override()
                .predicate(mcLoc("pulling"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_crossbow_pulling_0")))
                .end();
        crossbow.override()
                .predicate(mcLoc("pull"), 0.58F)
                .predicate(mcLoc("pulling"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_crossbow_pulling_1")))
                .end();
        crossbow.override()
                .predicate(mcLoc("pull"), 1.0F)
                .predicate(mcLoc("pulling"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_crossbow_pulling_2")))
                .end();
        // 已装填普通箭
        crossbow.override()
                .predicate(mcLoc("charged"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_crossbow_arrow")))
                .end();
        // 已装填烟花
        crossbow.override()
                .predicate(mcLoc("charged"), 1.0F)
                .predicate(mcLoc("firework"), 1.0F)
                .model(new ModelFile.UncheckedModelFile(modLoc("item/aya_crossbow_firework")))
                .end();
    }

    // ==================== 工具方法 ====================

    /**
     * 手持模型：继承 item/handheld，覆盖 layer0
     */
    private void handheld(String name) {
        withExistingParent(name, mcLoc("item/handheld"))
                .texture("layer0", modLoc("item/" + name));
    }
}