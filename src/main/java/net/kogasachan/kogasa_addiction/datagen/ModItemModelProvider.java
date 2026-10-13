package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KogasaAddiction.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        // 1. 材料：普通物品模型
        basicItem(ModItems.KOGASA_INGOT.get());
        basicItem(ModItems.AYA_INGOT.get());
        basicItem(ModItems.CIRNO_INGOT.get());
        basicItem(ModItems.KOISHI_INGOT.get());

        // 2. 工具与武器：手持模型 (item/handheld)
        // 注意：这里使用了 withExistingParent 来覆盖默认的 item/generated
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

        // 3. 护甲：普通物品模型 (因为护甲在物品栏中也是平面图标)
        basicItem(ModItems.KOGASA_HELMET.get());
        basicItem(ModItems.KOGASA_CHESTPLATE.get());
        basicItem(ModItems.KOGASA_LEGGINGS.get());
        basicItem(ModItems.KOGASA_BOOTS.get());
    }
}