package net.kogasachan.kogasa_addiction.item.kind;

import net.kogasachan.kogasa_addiction.datagen.ModItemTagsProvider;
import net.kogasachan.kogasa_addiction.item.FumoItem;
import net.kogasachan.kogasa_addiction.item.SakuyaKnifeItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * 装备类型，以及"这件物品算不算该类型"的判定。
 *
 * <p><b>判定原则是宽容：类身份 / 物品 tag / 注册名后缀，三个信号取 OR。</b>
 * 单独任何一条都会漏判其它模组的非标准写法：
 * <ul>
 *   <li>只认 {@code endsWith("_sword")}：名字叫 {@code frost_blade} 但继承 SwordItem 的会漏；</li>
 *   <li>只认 {@code instanceof SwordItem}：用裸 Item + 自定义属性当剑用、但打了
 *       {@code minecraft:swords} tag 的会漏；</li>
 *   <li>只认 tag：偷懒继承 Item、名字却叫 {@code xx_sword}、没打 tag 的会漏。</li>
 * </ul>
 *
 * <p><b>枚举声明顺序 = 判定优先级</b>，先命中先返回。有三处顺序不能动，详见 README 注释：
 * 镐必须早于斧（{@code "pickaxe".endsWith("axe")}）、弩必须早于弓
 * （{@code "crossbow".endsWith("bow")}）、Fumo / 飞刀必须早于通用类型
 * （它们分别继承 PickaxeItem 与 TridentItem）。
 */
public enum KogasaEquipmentKind {

    // ---- 本模组专有类型：必须最先判定 ----
    FUMO("fumo", Tags.Items.TOOLS, -2.0D),
    KNIFE("knife", Tags.Items.TOOLS_TRIDENTS, -2.9D),

    // ---- 远程：弩必须早于弓 ----
    CROSSBOW("crossbow", Tags.Items.TOOLS_CROSSBOWS, -2.8D),
    BOW("bow", Tags.Items.TOOLS_BOWS, -2.8D),

    // ---- 工具：镐必须早于斧 ----
    PICKAXE("pickaxe", ItemTags.PICKAXES, -2.8D),
    AXE("axe", ItemTags.AXES, -3.2D),
    SHOVEL("shovel", ItemTags.SHOVELS, -3.0D),
    HOE("hoe", ItemTags.HOES, -3.0D),
    SWORD("sword", ItemTags.SWORDS, -2.4D),

    // ---- 护甲 ----
    HELMET("helmet", Tags.Items.ARMORS_HELMETS, -2.8D),
    CHESTPLATE("chestplate", Tags.Items.ARMORS_CHESTPLATES, -2.8D),
    LEGGINGS("leggings", Tags.Items.ARMORS_LEGGINGS, -2.8D),
    BOOTS("boots", Tags.Items.ARMORS_BOOTS, -2.8D);

    private final String suffix;
    private final TagKey<Item> materialTag;
    private final double baseAttackSpeed;

    KogasaEquipmentKind(String suffix, TagKey<Item> materialTag, double baseAttackSpeed) {
        this.suffix = suffix;
        this.materialTag = materialTag;
        this.baseAttackSpeed = baseAttackSpeed;
    }

    /** 类型后缀，用于 tooltip / 进度 / 资源名拼接 */
    public String suffix() {
        return suffix;
    }

    /** 强化时右侧材料应匹配的 tag */
    public TagKey<Item> materialTag() {
        return materialTag;
    }

    /** 原版攻速基准（AttributeModifier 的 ADDITION 值） */
    public double baseAttackSpeed() {
        return baseAttackSpeed;
    }

    /**
     * 宽容判定：类身份 / tag / 注册名后缀，任一成立即为该类型。
     */
    public boolean matches(Item item) {
        if (item == null || item == Items.AIR) return false;

        if (matchesClass(item)) return true;
        if (item.builtInRegistryHolder().is(materialTag)) return true;
        return nameEndsWith(item, suffix);
    }

    /**
     * 该类型是否接受这件材料（左侧装备用 {@link #matches} 判定出类型后调用）。
     *
     * <p>注意这里**不能**直接用 {@link #matches} 判断材料：材料侧同样会被后缀规则误伤 ——
     * {@code "diamond_pickaxe".endsWith("axe")} 为 true，若用 {@code matches}，
     * 镐就会变成斧的合法材料。所以改成比较"材料被解析出的类型"，
     * 这样 {@link KogasaEquipmentKinds} 的优先级顺序会自动生效。
     */
    public boolean acceptsMaterial(ItemStack material) {
        if (material.isEmpty()) return false;

        // Fumo：旧行为就是"接受所有工具类"，保持
        if (this == FUMO) {
            return material.is(Tags.Items.TOOLS) || material.getItem() instanceof FumoItem;
        }

        // 飞刀：保留旧的三叉戟特例，额外接受剑 / 三叉戟 / 刀
        if (this == KNIFE) {
            if (material.is(ItemTags.SWORDS)
                    || material.is(Tags.Items.TOOLS_TRIDENTS)
                    || material.is(ModItemTagsProvider.KNIVES)) {
                return true;
            }
            ResourceLocation id = ForgeRegistries.ITEMS.getKey(material.getItem());
            if (id == null) return false;
            String path = id.getPath();
            return path.endsWith("sword") || path.endsWith("trident") || path.endsWith("knife");
        }

        // 其余类型：材料的"已解析类型"必须与本类型一致（自动套用优先级）
        return KogasaEquipmentKinds.of(material) == this;
    }

    private boolean matchesClass(Item item) {
        return switch (this) {
            case FUMO -> item instanceof FumoItem;
            // 三叉戟类统一按飞刀处理：这与旧实现 "left instanceof TridentItem 时接受剑/三叉戟/刀" 一致
            case KNIFE -> item instanceof SakuyaKnifeItem || item instanceof TridentItem;
            case CROSSBOW -> item instanceof CrossbowItem;
            case BOW -> item instanceof BowItem;
            case PICKAXE -> item instanceof PickaxeItem;
            case AXE -> item instanceof AxeItem;
            case SHOVEL -> item instanceof ShovelItem;
            case HOE -> item instanceof HoeItem;
            case SWORD -> item instanceof SwordItem;
            case HELMET -> isArmorType(item, ArmorItem.Type.HELMET);
            case CHESTPLATE -> isArmorType(item, ArmorItem.Type.CHESTPLATE);
            case LEGGINGS -> isArmorType(item, ArmorItem.Type.LEGGINGS);
            case BOOTS -> isArmorType(item, ArmorItem.Type.BOOTS);
        };
    }

    private static boolean isArmorType(Item item, ArmorItem.Type type) {
        return item instanceof ArmorItem armor && armor.getType() == type;
    }

    private static boolean nameEndsWith(Item item, String suffix) {
        ResourceLocation id = ForgeRegistries.ITEMS.getKey(item);
        return id != null && id.getPath().endsWith(suffix);
    }
}
