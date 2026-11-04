package net.kogasachan.kogasa_addiction_compat;

import net.kogasachan.kogasa_addiction.util.KogasaEquipmentHelper;
import net.kogasachan.kogasa_addiction_compat.compat.CompatConfig;
import net.kogasachan.kogasa_addiction_compat.compat.CompatConfigEntries;
import net.kogasachan.kogasa_addiction_compat.compat.CompatRegistry;
import net.kogasachan.kogasa_addiction_compat.network.CompatNetwork;
import net.kogasachan.kogasa_addiction_compat.compat.KogasaCompat;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Kogasa Addiction: Compat —— 兼容附属。
 *
 * <p>目前提供两层内容：
 * <ul>
 *   <li><b>兼容层</b>（{@code compat} 包）：配置总开关默认开启；开启后标签/标记过的非本模组
 *       工具武器也能吃成长、强化、封印等数值机制（属性通过 Forge 的
 *       {@code ItemAttributeModifierEvent} 接管，不覆盖其他模组物品）；</li>
 *   <li><b>彩虹内容</b>：彩虹矿石与彩虹水晶（后续扩展结合/熔岩转化功能）。</li>
 * </ul>
 */
@Mod(KogasaCompatAddon.MODID)
public final class KogasaCompatAddon {

    public static final String MODID = "kogasa_addiction_compat";

    /**
     * 使用 Forge 的构造器注入（1.20.1 支持单参数 {@link FMLJavaModLoadingContext}），
     * 避免调用已标记为移除的 {@code FMLJavaModLoadingContext.get()} / {@code ModLoadingContext.get()}。
     */
    public KogasaCompatAddon(FMLJavaModLoadingContext context) {
        IEventBus modBus = context.getModEventBus();

        // 把“兼容物品”接入主模组的统一装备判定（主模组侧只保留一个注册点，逻辑仍在 compat 包）
        KogasaEquipmentHelper.registerEquipmentPredicate(KogasaCompat::isCompatible);

        CompatNetwork.register();
        // 接入主模组的 /kogasa config（路径前缀 compat:）
        net.kogasachan.kogasa_addiction.command.KogasaConfigCommands.registerExternalConfig(
                "compat", CompatConfig.SPEC, CompatConfigEntries::values);

        CompatRegistry.register(modBus);
        context.registerConfig(ModConfig.Type.COMMON, CompatConfig.SPEC,
                "kogasa_addiction_compat-common-" + CompatConfig.CONFIG_VERSION + ".toml");

        modBus.addListener(this::onBuildCreativeTabs);
    }

    private void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(CompatRegistry.RAINBOW_CRYSTAL.get());
        } else if (event.getTabKey() == CreativeModeTabs.NATURAL_BLOCKS) {
            event.accept(CompatRegistry.RAINBOW_ORE_ITEM.get());
        }
    }
}
