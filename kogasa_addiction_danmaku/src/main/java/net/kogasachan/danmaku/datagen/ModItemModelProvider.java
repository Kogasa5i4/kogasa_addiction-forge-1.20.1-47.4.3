package net.kogasachan.danmaku.datagen;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/**
 * 物品模型：29 种弹幕物品共用一张白色图标贴图（运行期按颜色 tint），发射器沿用原版烈焰棒外观。
 */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KogasaDanmaku.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        for (DanmakuType type : DanmakuType.values()) {
            withExistingParent("danmaku_" + type.id(), mcLoc("item/generated"))
                    .texture("layer0", modLoc("item/danmaku_item"));
        }

        withExistingParent("danmaku_wand", mcLoc("item/handheld"))
                .texture("layer0", mcLoc("item/blaze_rod"));
    }
}
