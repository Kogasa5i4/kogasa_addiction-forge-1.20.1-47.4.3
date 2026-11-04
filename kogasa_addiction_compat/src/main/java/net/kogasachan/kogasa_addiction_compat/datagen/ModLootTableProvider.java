package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** 方块战利品表：彩虹矿石套用铁矿掉落。 */
public class ModLootTableProvider extends LootTableProvider {

    public ModLootTableProvider(PackOutput output) {
        super(output, Set.of(),
                List.of(new SubProviderEntry(Blocks::new, LootContextParamSets.BLOCK)));
    }

    private static class Blocks extends BlockLootSubProvider {

        protected Blocks() {
            super(Set.of(), FeatureFlags.REGISTRY.allFlags());
        }

        @Override
        protected void generate() {
            add(CompatRegistry.RAINBOW_ORE.get(),
                    createOreDrop(CompatRegistry.RAINBOW_ORE.get(), CompatRegistry.RAINBOW_CRYSTAL.get()));
        }

        @Override
        protected Iterable<Block> getKnownBlocks() {
            return CompatRegistry.BLOCKS.getEntries().stream()
                    .map(holder -> (Block) holder.get())
                    .collect(Collectors.toList());
        }
    }
}
