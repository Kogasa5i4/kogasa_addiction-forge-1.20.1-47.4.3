package net.kogasachan.kogasa_addiction_compat.compat;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 兼容附属的方块/物品注册。
 *
 * <p>彩虹矿石与彩虹水晶先注册好占位，后续扩展：
 * <ul>
 *   <li>彩虹水晶与物品“结合”，开启该物品的全部本模组功能；</li>
 *   <li>彩虹水晶丢进熔岩/熔岩炼药锅，按概率转化为随机能力锭。</li>
 * </ul>
 */
public final class CompatRegistry {

    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, KogasaCompatAddon.MODID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, KogasaCompatAddon.MODID);

    /** 彩虹矿石：只在高空（默认 Y≥128）生成 */
    public static final RegistryObject<Block> RAINBOW_ORE = BLOCKS.register("rainbow_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
                            .strength(3.0F, 3.0F)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.STONE),
                    UniformInt.of(0, 2)));

    /** 彩虹水晶：兼容层的“钥匙”物品 */
    public static final RegistryObject<Item> RAINBOW_CRYSTAL = ITEMS.register("rainbow_crystal",
            () -> new RainbowCrystalItem(new Item.Properties().fireResistant()));

    public static final RegistryObject<Item> RAINBOW_ORE_ITEM = ITEMS.register("rainbow_ore",
            () -> new BlockItem(RAINBOW_ORE.get(), new Item.Properties().fireResistant()));

    private CompatRegistry() {
    }

    public static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
    }
}
