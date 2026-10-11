package net.kogasachan.kogasa_addiction.datagen;

import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = KogasaAddiction.MODID,
        bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataGenerators {

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        DataGenerator generator = event.getGenerator();
        PackOutput output = generator.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();

        // 服务端数据 (配方、战利品表等)
        generator.addProvider(event.includeServer(), new ModRecipeProvider(output));

        // 客户端数据 (模型、语言文件等)
        generator.addProvider(event.includeClient(),
                new ModItemModelProvider(output, existingFileHelper));
    }
}