package net.kogasachan.kogasa_addiction.datagen;

import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

/**
 * 战利品表 datagen 入口：把 {@link ModChestLootTables} 注册给原版 {@link LootTableProvider}。
 */
public class ModLootTableProvider extends LootTableProvider {

    public ModLootTableProvider(PackOutput output) {
        super(output, Set.of(),
                List.of(new SubProviderEntry(ModChestLootTables::new, LootContextParamSets.CHEST)));
    }
}
