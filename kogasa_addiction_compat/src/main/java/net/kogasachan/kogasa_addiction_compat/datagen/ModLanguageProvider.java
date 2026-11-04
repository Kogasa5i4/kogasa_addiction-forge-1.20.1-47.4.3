package net.kogasachan.kogasa_addiction_compat.datagen;

import net.kogasachan.kogasa_addiction_compat.KogasaCompatAddon;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * 语言文件（en_us / zh_cn 各一份）。
 * {@code zh} 为 true 时写入中文文本，否则写英文。
 */
public class ModLanguageProvider extends LanguageProvider {

    private final boolean zh;

    public ModLanguageProvider(PackOutput output, String locale, boolean zh) {
        super(output, KogasaCompatAddon.MODID, locale);
        this.zh = zh;
    }

    @Override
    protected void addTranslations() {
        if (zh) {
            add("item.kogasa_addiction_compat.rainbow_crystal", "彩虹水晶");
            add("block.kogasa_addiction_compat.rainbow_ore", "彩虹矿石");
            add("item.kogasa_addiction_compat.rainbow_crystal.tooltip", "§7在物品栏里用鼠标拿起水晶，右键目标物品即可“结合”，开启全部本模组机制");
            add("item.kogasa_addiction_compat.rainbow_crystal.tooltip2", "§8丢进熔岩/熔岩炼药锅可转化为随机能力锭");
            add("message.kogasa_addiction_compat.combined", "已将彩虹水晶与 %s 结合：现在它能使用全部本模组功能");
            add("message.kogasa_addiction_compat.already_compatible", "这个物品已经兼容本模组机制了");
            add("message.kogasa_addiction_compat.compat_disabled", "兼容总开关未开启（compat.enableCompatEquipment）");
            add("book.kogasa_addiction_compat.manual.name", "Kogasa Addiction: Compat 手册");
            add("book.kogasa_addiction_compat.manual.landing", "这个附属让成长、强化、封印与属性机制可以作用到其他模组的工具武器上——但默认只对“被彩虹水晶结合过”的物品生效。");
            add("book.kogasa_addiction_compat.category.compat.name", "兼容机制");
            add("book.kogasa_addiction_compat.category.compat.desc", "兼容总开关、结合方式与彩虹水晶。");
            add("book.kogasa_addiction_compat.entry.overview.name", "兼容是怎么生效的");
            add("book.kogasa_addiction_compat.entry.overview.page1", "兼容总开关在配置文件 $(6)compat.enableCompatEquipment$(0)，默认开启。只有在白名单里的物品才会应用本模组机制：$(li)本模组自己的装备；$(li)用彩虹水晶结合过的物品；$(li)数据包写进 $(6)#kogasa_addiction_compat:compatible$(0) 标签的物品。");
            add("book.kogasa_addiction_compat.entry.overview.page2", "结合方法：把彩虹水晶拿在鼠标上（左键拿起），在物品栏里用$(6)右键$(0)点击目标物品槽位。生存模式消耗 1 个水晶，创造模式只打标记不消耗。$(br2)兼容后的物品会获得成长、强化、封印与属性加成，tooltip 也会显示本模组的数值行。");
            add("book.kogasa_addiction_compat.entry.rainbow_crystal.name", "彩虹矿石与彩虹水晶");
            add("book.kogasa_addiction_compat.entry.rainbow_crystal.page1", "彩虹矿石只在高空生成（Y 坐标 128 以上），外观和生成方式参考铁矿石，挖掘需要石镐或更好。破坏后用精准采集会掉落矿石本体，否则掉落 1 个彩虹水晶。");
            add("book.kogasa_addiction_compat.entry.rainbow_crystal.page2", "把彩虹水晶丢进$(6)熔岩$(0)或$(6)熔岩炼药锅$(0)，会按配置概率（默认 100%%）转化为随机一种能力锭。$(br2)转化概率与开关在 $(6)rainbow$(0) 配置段里。");
        } else {
            add("item.kogasa_addiction_compat.rainbow_crystal", "Rainbow Crystal");
            add("block.kogasa_addiction_compat.rainbow_ore", "Rainbow Ore");
            add("item.kogasa_addiction_compat.rainbow_crystal.tooltip", "§7Pick up the crystal with your cursor, then right-click a target item in the inventory to combine it and enable all Kogasa mechanics");
            add("item.kogasa_addiction_compat.rainbow_crystal.tooltip2", "§8Drop into lava / lava cauldron to convert into a random ability ingot");
            add("message.kogasa_addiction_compat.combined", "Combined rainbow crystal with %s: all Kogasa mechanics are now enabled on it");
            add("message.kogasa_addiction_compat.already_compatible", "This item is already compatible");
            add("message.kogasa_addiction_compat.compat_disabled", "Compatibility master switch is disabled (compat.enableCompatEquipment)");
            add("book.kogasa_addiction_compat.manual.name", "Kogasa Addiction: Compat Manual");
            add("book.kogasa_addiction_compat.manual.landing", "This addon lets growth, reinforcement, sealing and attribute mechanics apply to other mods' tools and weapons — but by default only to items combined with a rainbow crystal.");
            add("book.kogasa_addiction_compat.category.compat.name", "Compatibility");
            add("book.kogasa_addiction_compat.category.compat.desc", "Master switch, combining and the rainbow crystal.");
            add("book.kogasa_addiction_compat.entry.overview.name", "How compatibility works");
            add("book.kogasa_addiction_compat.entry.overview.page1", "The master switch is $(6)compat.enableCompatEquipment$(0) (enabled by default). Only whitelisted items get Kogasa mechanics: $(li)Kogasa's own equipment; $(li)items combined with a rainbow crystal; $(li)items added to the $(6)#kogasa_addiction_compat:compatible$(0) item tag by a datapack.");
            add("book.kogasa_addiction_compat.entry.overview.page2", "To combine: pick up a rainbow crystal with your cursor, then $(6)right-click$(0) the target item slot in your inventory. Survival consumes 1 crystal; creative only marks the item.$(br2)Compatible items gain growth, reinforcement, sealing and attribute bonuses, and their tooltip shows Kogasa's stat lines.");
            add("book.kogasa_addiction_compat.entry.rainbow_crystal.name", "Rainbow Ore & Crystal");
            add("book.kogasa_addiction_compat.entry.rainbow_crystal.page1", "Rainbow ore only generates high up (above Y 128), following iron ore's style, and needs a stone pickaxe or better. Without silk touch it drops 1 rainbow crystal.");
            add("book.kogasa_addiction_compat.entry.rainbow_crystal.page2", "Drop a rainbow crystal into $(6)lava$(0) or a $(6)lava cauldron$(0) to convert it into a random ability ingot, with a configurable chance (100%% by default).$(br2)See the $(6)rainbow$(0) config section.");
        }
    }
}
