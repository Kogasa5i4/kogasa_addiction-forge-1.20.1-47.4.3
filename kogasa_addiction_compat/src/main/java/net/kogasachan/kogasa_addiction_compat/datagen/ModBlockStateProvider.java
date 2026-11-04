package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;

/** 方块状态与方块/物品模型。 */
public class ModBlockStateProvider extends BlockStateProvider {

    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, KogasaCompatAddon.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlock(CompatRegistry.RAINBOW_ORE.get());
        // 方块物品模型（背包/手持里用）
        itemModels().withExistingParent("rainbow_ore", modLoc("block/rainbow_ore"));
    }
}
