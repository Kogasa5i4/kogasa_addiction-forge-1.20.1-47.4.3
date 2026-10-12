package net.kogasachan.kogasa_addiction.compat.ponder;

import net.createmod.ponder.api.registration.PonderPlugin;
import net.createmod.ponder.api.registration.PonderSceneRegistrationHelper;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.NotNull;

public class KogasaPonderPlugin implements PonderPlugin {
    @Override
    public @NotNull String getModId() {
        return KogasaAddiction.MODID; // 你的模组ID
    }

    @Override
    public void registerScenes(PonderSceneRegistrationHelper<ResourceLocation> helper) {
        if (!ModList.get().isLoaded("ponder")) return;
        // 场景 1：高炉视角
        helper.addStoryBoard(
                ResourceLocation.fromNamespaceAndPath("minecraft", "blast_furnace"),
                ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "kogasa_ingot_from_furnace"),
                KogasaPonderScenes::kogasaIngotScene
        );

        // 场景 2：锭视角
        helper.addStoryBoard(
                ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "kogasa_ingot"),
                ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "kogasa_ingot_obtain"),
                KogasaPonderScenes::kogasaIngotScene
        );
    }
}
