package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/** 物品模型（彩虹水晶、兼容手册）。 */
public class ModItemModelProvider extends ItemModelProvider {

    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KogasaCompatAddon.MODID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        basicItem(CompatRegistry.RAINBOW_CRYSTAL.get());
    }
}
