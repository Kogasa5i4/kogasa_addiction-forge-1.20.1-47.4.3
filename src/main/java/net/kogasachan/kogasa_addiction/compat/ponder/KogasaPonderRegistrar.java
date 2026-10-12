package net.kogasachan.kogasa_addiction.compat.ponder;

import net.createmod.ponder.foundation.PonderIndex;

// 这个类只被反射加载，不会出现在 @Mod.EventBusSubscriber 扫描范围
public class KogasaPonderRegistrar {
    public static void register() {
        PonderIndex.addPlugin(new KogasaPonderPlugin());
    }
}
