package net.kogasachan.danmaku.bullet;

import javax.annotation.Nullable;

/**
 * 弹幕颜色：直接采用原版 16 种染料色（RGB 与染料物品一致），
 * 这样颜色与染料天然对应，后续做配方/贴图配色都省事。
 */
public enum DanmakuColor {

    WHITE("white", 0xF9FFFE),
    ORANGE("orange", 0xF9801D),
    MAGENTA("magenta", 0xC74EBD),
    LIGHT_BLUE("light_blue", 0x3AB3DA),
    YELLOW("yellow", 0xFED83D),
    LIME("lime", 0x80C71F),
    PINK("pink", 0xF38BAA),
    GRAY("gray", 0x474F52),
    LIGHT_GRAY("light_gray", 0x9D9D97),
    CYAN("cyan", 0x169C9C),
    PURPLE("purple", 0x8932B8),
    BLUE("blue", 0x3C44AA),
    BROWN("brown", 0x835432),
    GREEN("green", 0x5E7C16),
    RED("red", 0xB02E26),
    BLACK("black", 0x1D1D21);

    private static final DanmakuColor[] VALUES = values();

    private final String id;
    private final int rgb;

    DanmakuColor(String id, int rgb) {
        this.id = id;
        this.rgb = rgb;
    }

    public String id() {
        return id;
    }

    public int rgb() {
        return rgb;
    }

    public float red() {
        return ((rgb >> 16) & 0xFF) / 255.0F;
    }

    public float green() {
        return ((rgb >> 8) & 0xFF) / 255.0F;
    }

    public float blue() {
        return (rgb & 0xFF) / 255.0F;
    }

    public DanmakuColor next() {
        return VALUES[(ordinal() + 1) % VALUES.length];
    }

    public static DanmakuColor byIndex(int index) {
        int i = Math.floorMod(index, VALUES.length);
        return VALUES[i];
    }

    @Nullable
    public static DanmakuColor byId(String id) {
        for (DanmakuColor color : VALUES) {
            if (color.id.equals(id)) {
                return color;
            }
        }
        return null;
    }
}
