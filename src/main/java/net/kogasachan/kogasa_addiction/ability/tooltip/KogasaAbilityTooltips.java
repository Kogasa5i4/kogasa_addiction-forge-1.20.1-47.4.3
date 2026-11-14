package net.kogasachan.kogasa_addiction.ability.tooltip;

import net.kogasachan.kogasa_addiction.ability.KogasaAbility;

import java.util.EnumMap;
import java.util.Map;

/**
 * 能力 tooltip 渲染器的注册表与分发入口。
 *
 * <p>tooltip 里的 6 段"能力专属描述"原先是在 KogasaTooltipHandler 里逐个 if 判断的；
 * 注册化后新增能力只需：
 * <ol>
 *   <li>实现 {@link KogasaAbilityTooltip}；</li>
 *   <li>在下面静态块里注册一行。</li>
 * </ol>
 * 未注册的能力静默跳过，不影响其它段落。
 */
public final class KogasaAbilityTooltips {

    private static final Map<KogasaAbility, KogasaAbilityTooltip> RENDERERS =
            new EnumMap<>(KogasaAbility.class);

    static {
        RENDERERS.put(KogasaAbility.AYA, new AyaAbilityTooltip());
        RENDERERS.put(KogasaAbility.CIRNO, new CirnoAbilityTooltip());
        RENDERERS.put(KogasaAbility.KOISHI, new KoishiAbilityTooltip());
        RENDERERS.put(KogasaAbility.KOGASA, new KogasaUmbrellaAbilityTooltip());
        RENDERERS.put(KogasaAbility.SAKUYA, new SakuyaAbilityTooltip());
        RENDERERS.put(KogasaAbility.TENSHI, new TenshiAbilityTooltip());
    }

    private KogasaAbilityTooltips() {
    }

    /**
     * 若该能力注册过渲染器，则把其描述追加进 ctx
     */
    public static void append(KogasaAbility ability, KogasaAbilityTooltipContext ctx) {
        KogasaAbilityTooltip renderer = RENDERERS.get(ability);
        if (renderer != null) {
            renderer.append(ctx);
        }
    }
}
