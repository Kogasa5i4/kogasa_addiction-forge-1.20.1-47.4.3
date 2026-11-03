package net.kogasachan.danmaku.bullet;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * 弹幕种类表（东方 STG 常用弹型），按中文名 → 日文名 → 英文名整理。
 *
 * <p>每种弹幕自带：显示名（中/英，供 datagen 生成语言文件）、渲染尺寸（宽/高，格）、
 * 命中伤害、默认速度、存活 tick。
 * id 同时作为贴图名：{@code assets/kogasa_addiction_danmaku/textures/entity/bullet/<id>.png}
 *
 * <p>尺寸参照：史蒂夫宽 0.6 格，东方里点弹约为角色宽度的一半、大玉超过一个身位，
 * 因此本表的尺寸整体按此比例放大（点弹 0.40 格、小玉 0.70、大玉 1.45），保证肉眼可辨。
 *
 * <p>说明：
 * <ul>
 *   <li>「米弹 / 粥弹」统一为 {@link #RICE}；</li>
 *   <li>「针弹 / 結晶弾」统一为 {@link #SHARD}；</li>
 *   <li>「环玉 / 带圈小玉」统一为 {@link #ORBS}；</li>
 *   <li>「炎弹」「火弹」统一为 {@link #FIREBALL}。</li>
 * </ul>
 */
public enum DanmakuType {

    /** 点弹 */
    PELLET("pellet", "点弹", "Pellet", 0.40F, 0.40F, 3.0F, 0.30D, 150),
    /** 菌弹 / 葡萄弹 / 葡萄菌（ウィルス弾） */
    POPCORN("popcorn", "菌弹", "Popcorn", 0.44F, 0.62F, 3.0F, 0.27D, 150),
    /** 粒弹 / 超小弹 */
    TINY_PETAL("tiny_petal", "粒弹", "Tiny Petal", 0.31F, 0.31F, 2.0F, 0.33D, 125),
    /** 小玉 */
    BALL("ball", "小玉", "Ball", 0.70F, 0.70F, 4.0F, 0.25D, 175),
    /** 环玉 / 带圈小玉 */
    ORBS("orbs", "环玉", "Orbs", 0.75F, 0.75F, 4.0F, 0.24D, 175),
    /** 米弹 */
    RICE("rice", "米弹", "Rice", 0.31F, 0.66F, 3.0F, 0.30D, 150),
    /** 链弹 / 苦无（クナイ弾） */
    KUNAI("kunai", "链弹", "Kunai", 0.88F, 0.35F, 5.0F, 0.42D, 125),
    /** 针弹 / 結晶弾 */
    SHARD("shard", "针弹", "Shard", 0.48F, 0.92F, 5.0F, 0.36D, 125),
    /** 札弹 */
    AMULET("amulet", "札弹", "Amulet", 0.48F, 0.88F, 4.0F, 0.27D, 200),
    /** 鳞弹 / 楔弾 */
    ARROWHEAD("arrowhead", "鳞弹", "Arrowhead", 0.62F, 0.62F, 5.0F, 0.36D, 125),
    /** 铳弹 */
    BULLET("bullet", "铳弹", "Bullet", 0.31F, 0.31F, 3.0F, 0.48D, 125),
    /** 杆菌弹 */
    DARK_PILL("dark_pill", "杆菌弹", "Dark Pill", 0.26F, 0.26F, 3.0F, 0.30D, 150),
    /** 星弹 */
    STAR("star", "星弹", "Star", 0.62F, 0.62F, 5.0F, 0.30D, 150),
    /** 大星弹 */
    BIG_STAR("big_star", "大星弹", "Big Star", 1.19F, 1.19F, 7.0F, 0.24D, 175),
    /** 钱币 / 铜钱 */
    COIN("coin", "钱币", "Coin", 0.62F, 0.62F, 4.0F, 0.30D, 200),
    /** 中玉 */
    MENTOS("mentos", "中玉", "Mentos", 0.79F, 0.79F, 4.0F, 0.24D, 200),
    /** 椭弹 */
    JELLYBEAN("jellybean", "椭弹", "Jellybean", 0.79F, 0.53F, 4.0F, 0.27D, 175),
    /** 刀弹 / 小刀（ナイフ弾） */
    KNIFE("knife", "刀弹", "Knife", 1.01F, 0.31F, 6.0F, 0.48D, 100),
    /** 蝶弹 */
    BUTTERFLY("butterfly", "蝶弹", "Butterfly", 0.92F, 0.70F, 4.0F, 0.27D, 175),
    /** 炎弹 / 火弹 */
    FIREBALL("fireball", "炎弹", "Fireball", 0.84F, 0.84F, 6.0F, 0.27D, 175),
    /** 心弹 */
    HEART("heart", "心弹", "Heart", 0.62F, 0.62F, 4.0F, 0.27D, 175),
    /** 箭弹 */
    ARROW("arrow", "箭弹", "Arrow", 1.01F, 0.35F, 5.0F, 0.45D, 125),
    /** 大玉 */
    BUBBLE("bubble", "大玉", "Bubble", 1.45F, 1.45F, 8.0F, 0.21D, 225),
    /** 光玉 */
    GLOWY_BALL("glowy_ball", "光玉", "Glowy Ball", 0.75F, 0.75F, 4.0F, 0.27D, 175),
    /** 滴弹（水滴弾 / 菱形弾） */
    RAINDROP("raindrop", "滴弹", "Raindrop", 0.44F, 0.70F, 3.0F, 0.30D, 150),
    /** 激光 / 短激光 */
    LASER("laser", "激光", "Laser", 2.42F, 0.35F, 6.0F, 0.60D, 75),
    /** 曲线激光 */
    CURVY_LASER("curvy_laser", "曲线激光", "Curvy Laser", 2.20F, 0.44F, 6.0F, 0.42D, 100),
    /** 音符 */
    NOTE("note", "音符", "Note", 0.75F, 0.75F, 4.0F, 0.27D, 175),
    /** 休止符 */
    REST("rest", "休止符", "Rest", 0.62F, 0.62F, 4.0F, 0.27D, 175);

    private static final DanmakuType[] VALUES = values();

    private final String id;
    private final String cnName;
    private final String enName;
    private final float width;
    private final float height;
    private final float damage;
    private final double speed;
    private final int life;

    DanmakuType(String id, String cnName, String enName,
                float width, float height, float damage, double speed, int life) {
        this.id = id;
        this.cnName = cnName;
        this.enName = enName;
        this.width = width;
        this.height = height;
        this.damage = damage;
        this.speed = speed;
        this.life = life;
    }

    public String id() {
        return id;
    }

    /** 中文显示名（datagen 生成语言文件用） */
    public String cnName() {
        return cnName;
    }

    /** 英文显示名（datagen 生成语言文件用） */
    public String enName() {
        return enName;
    }

    /** 渲染与命中判定的宽度（格） */
    public float width() {
        return width;
    }

    /** 渲染与命中判定的高度（格） */
    public float height() {
        return height;
    }

    public float damage() {
        return damage;
    }

    /** 默认初速（格/tick） */
    public double speed() {
        return speed;
    }

    /** 存活 tick */
    public int life() {
        return life;
    }

    /**
     * 是否用"多角度渲染"（十字交叉双面片 + 端面）。
     * 非圆形的平面图案（蝶/心/箭/刀/音符/休止符/札）只画单片，保持图案正对飞行方向。
     */
    public boolean multiAngle() {
        return switch (this) {
            case BUTTERFLY, HEART, ARROW, KNIFE, NOTE, REST, AMULET -> false;
            default -> true;
        };
    }

    /** 是否为"激光类"武器：不发射弹体，而是从发射点向外延伸一段射线 */
    public boolean isBeam() {
        return this == LASER || this == CURVY_LASER;
    }

    /** 激光延伸长度（格） */
    public float beamLength() {
        return 10.0F;
    }

    /** 曲线激光的起伏幅度（格）；直线激光为 0 */
    public float beamAmplitude() {
        return this == CURVY_LASER ? 1.6F : 0.0F;
    }

    public ResourceLocation texture() {
        return ResourceLocation.fromNamespaceAndPath(
                KogasaDanmaku.MODID, "textures/entity/bullet/" + id + ".png");
    }

    public static DanmakuType byIndex(int index) {
        int i = Math.floorMod(index, VALUES.length);
        return VALUES[i];
    }

    @Nullable
    public static DanmakuType byId(String id) {
        for (DanmakuType type : VALUES) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
