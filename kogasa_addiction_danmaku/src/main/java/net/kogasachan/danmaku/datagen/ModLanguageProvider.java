package net.kogasachan.danmaku.datagen;

import net.kogasachan.danmaku.KogasaDanmaku;
import net.kogasachan.danmaku.bullet.DanmakuType;
import net.minecraft.data.PackOutput;
import net.minecraftforge.common.data.LanguageProvider;

/**
 * 语言文件：物品名直接取自 {@link DanmakuType} 的中/英显示名，避免手写 29 个键时漏改。
 */
public class ModLanguageProvider extends LanguageProvider {

    private final boolean chinese;

    public ModLanguageProvider(PackOutput output, String locale, boolean chinese) {
        super(output, KogasaDanmaku.MODID, locale);
        this.chinese = chinese;
    }

    @Override
    protected void addTranslations() {
        for (DanmakuType type : DanmakuType.values()) {
            add("item.kogasa_addiction_danmaku.danmaku_" + type.id(),
                    chinese ? "弹幕·" + type.cnName() : "Danmaku: " + type.enName());
        }
        add("item.kogasa_addiction_danmaku.danmaku_wand", chinese ? "弹幕发射器" : "Danmaku Wand");
        add("entity.kogasa_addiction_danmaku.danmaku_bullet", chinese ? "弹幕" : "Danmaku Bullet");

        add("tooltip.kogasa_addiction_danmaku.item.type", chinese ? "弹型：%s" : "Danmaku type: %s");
        add("tooltip.kogasa_addiction_danmaku.item.color", chinese ? "颜色：%s" : "Color: %s");
        add("tooltip.kogasa_addiction_danmaku.item.hint",
                chinese ? "右键发射　Alt+右键切换颜色" : "RMB: shoot / Alt+RMB: cycle color");

        add("tooltip.kogasa_addiction_danmaku.wand.type", chinese ? "弹型：%s" : "Danmaku type: %s");
        add("tooltip.kogasa_addiction_danmaku.wand.color", chinese ? "颜色：%s" : "Color: %s");
        add("tooltip.kogasa_addiction_danmaku.wand.hint",
                chinese ? "右键发射　Ctrl+右键换弹型　Alt+右键换颜色　Ctrl+Alt+右键打全弹型"
                        : "RMB: shoot / Ctrl+RMB: next type / Alt+RMB: next color / Ctrl+Alt+RMB: all types");

        add("death.attack.danmaku", chinese ? "%1$s 满身疮痍" : "%1$s was riddled with danmaku");
        add("death.attack.danmaku.item",
                chinese ? "%1$s 因 %2$s 的 %3$s 满身疮痍" : "%1$s was riddled with danmaku by %2$s using %3$s");
        add("death.attack.danmaku.player",
                chinese ? "%1$s 因 %2$s 的弹幕满身疮痍" : "%1$s was riddled with %2$s's danmaku");
    }
}
