package net.kogasachan.kogasa_addiction.item.kind;

import net.kogasachan.kogasa_addiction.datagen.ModItemTagsProvider;
import net.kogasachan.kogasa_addiction.item.FumoItem;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.common.Tags;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Locale;

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
    BOW("bow", Tags.Items.TOOLS_BOWS, -2.8D, "crossbow"),

    // ---- 工具：镐必须早于斧 ----
    PICKAXE("pickaxe", ItemTags.PICKAXES, -2.8D),
    AXE("axe", ItemTags.AXES, -3.2D, "pickaxe"),
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
    /** 更具体的同义后缀：命中这些后缀时说明是别的类型（如 crossbow / pickaxe），本类型必须让位 */
    private final String[] excludedSuffixes;

    KogasaEquipmentKind(String suffix, TagKey<Item> materialTag, double baseAttackSpeed,
                        String... excludedSuffixes) {
        this.suffix = suffix;
        this.materialTag = materialTag;
        this.baseAttackSpeed = baseAttackSpeed;
        this.excludedSuffixes = excludedSuffixes;
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

        // 否定判断放最前面：即使某个模组把弩塞进 #forge:tools/bows、把镐塞进 #minecraft:axes，
        // 只要名字是 crossbow / pickaxe，就绝不按 bow / axe 处理。
        if (nameEndsWithAny(item, excludedSuffixes)) return false;

        if (matchesClass(item)) return true;
        // ItemStack.is(TagKey) 自 1.18 起一直是稳定 API，
        // 代替在后续版本被弃用的 builtInRegistryHolder()。
        if (item.getDefaultInstance().is(materialTag)) return true;
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

        // Fumo：不再依赖 #forge:tools 标签，改为“有攻击伤害 / 攻击速度 / 挖掘速度任一属性”即兼容
        if (this == FUMO) {
            return material.getItem() instanceof FumoItem || hasEquipmentAttributes(material);
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
            case KNIFE -> item instanceof TridentItem;
            case CROSSBOW -> item instanceof CrossbowItem;
            // 否定判断：有些模组的弩继承 BowItem，必须先排除
            case BOW -> !(item instanceof CrossbowItem) && item instanceof BowItem;
            case PICKAXE -> item instanceof PickaxeItem;
            // 否定判断：避免奇怪继承（镐继承斧基类）时被当成斧
            case AXE -> !(item instanceof PickaxeItem) && item instanceof AxeItem;
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

    /**
     * Fumo 的材料判定：不再依赖通用工具 tag，只要物品带攻击伤害 / 攻击速度 / 挖掘速度中任意一种属性即可。
     *
     * <p>原版挖掘速度不是 Attribute，所以额外用 {@link DiggerItem} 与常见方块 destroy speed 兜底；
     * 模组的挖掘速度属性则按属性 id 里的 {@code mining_speed} 识别。
     */
    private static boolean hasEquipmentAttributes(ItemStack stack) {
        for (Attribute attribute : stack.getAttributeModifiers(EquipmentSlot.MAINHAND).keySet()) {
            String id = attribute.getDescriptionId().toLowerCase(Locale.ROOT);
            if (id.contains("attack_damage") || id.contains("attack_speed") || id.contains("mining_speed")) {
                return true;
            }
        }

        if (stack.getItem() instanceof DiggerItem) {
            return true;
        }
        return stack.getDestroySpeed(Blocks.STONE.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.DIRT.defaultBlockState()) > 1.0F
                || stack.getDestroySpeed(Blocks.OAK_LOG.defaultBlockState()) > 1.0F;
    }

    private static boolean nameEndsWithAny(Item item, String[] suffixes) {
        if (suffixes.length == 0) return false;
        for (String suffix : suffixes) {
            if (nameEndsWith(item, suffix)) return true;
        }
        return false;
    }
}
