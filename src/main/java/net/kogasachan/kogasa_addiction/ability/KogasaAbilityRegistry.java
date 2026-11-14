package net.kogasachan.kogasa_addiction.ability;

import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 能力注册表：集中登记每个能力与其"能力锭"物品的对应关系。
 *
 * <p>注册化之后，新增一个能力的改动清单收敛为：
 * <ol>
 *   <li>{@link KogasaAbility} 枚举加常量，并在 {@code getObtainKeys()} 里补获取途径键；</li>
 *   <li>写该能力的 handler（{@code event/common/*AbilityHandler}）；</li>
 *   <li>lang 文件补 {@code ability.kogasa_addiction.<id>} / {@code .desc} /
 *       {@code tooltip.kogasa_addiction.ability_ingot.obtain.<id>.N}；</li>
 *   <li>（可选）在 {@code ability/tooltip} 里实现该能力的 tooltip。</li>
 * </ol>
 * 能力锭的物品注册与 {@code kogasa_addiction:ability_ingots} 标签
 * 都由本注册表驱动，不再需要手改 {@code ModItems} 和 {@code ModItemTagsProvider}。
 */
public final class KogasaAbilityRegistry {

    /**
     * 能力 → 能力锭物品。用 LinkedHashMap 保持注册顺序稳定。
     */
    private static final Map<KogasaAbility, RegistryObject<Item>> INGOTS = new LinkedHashMap<>();

    private KogasaAbilityRegistry() {
    }

    public static void registerIngot(KogasaAbility ability, RegistryObject<Item> ingot) {
        if (ability == null || ingot == null) return;
        INGOTS.put(ability, ingot);
    }

    public static RegistryObject<Item> getIngot(KogasaAbility ability) {
        return INGOTS.get(ability);
    }

    /**
     * 已注册的 能力 → 能力锭 映射（不可变视图，顺序稳定）
     */
    public static Map<KogasaAbility, RegistryObject<Item>> ingots() {
        return Collections.unmodifiableMap(INGOTS);
    }
}
