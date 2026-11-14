package net.kogasachan.kogasa_addiction.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 六种角色能力。
 *
 * <p>名称与描述走本地化键：{@code ability.kogasa_addiction.<id>}（名称）、
 * {@code ability.kogasa_addiction.<id>.desc}（描述）；
 * 锭 tooltip 的"获取方式（剧透）"键见 {@link #getObtainKeys()}。
 */
public enum KogasaAbility {
    AYA("aya"),
    CIRNO("cirno"),
    KOISHI("koishi"),
    KOGASA("kogasa"),
    TENSHI("tenshi"),
    SAKUYA("sakuya");

    private final String id;

    KogasaAbility(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    /**
     * 能力显示名（本地化）。
     */
    public Component getDisplayName() {
        return Component.translatable("ability.kogasa_addiction." + id);
    }

    /**
     * 能力描述（本地化，锭 tooltip 第二行）。
     */
    public Component getDescription() {
        return Component.translatable("ability.kogasa_addiction." + id + ".desc");
    }

    /**
     * 锭 tooltip 的"获取方式（剧透）"语言键列表。
     */
    public List<String> getObtainKeys() {
        return switch (this) {
            case KOGASA -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.kogasa.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.kogasa.2");
            case AYA -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.aya.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.aya.2",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.aya.3");
            case CIRNO -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.cirno.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.cirno.2",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.cirno.3");
            case KOISHI -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.koishi.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.koishi.2",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.koishi.3");
            case TENSHI -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.tenshi.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.tenshi.2");
            case SAKUYA -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.sakuya.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.sakuya.2");
        };
    }

    /**
     * 按 id 查找能力，未知 id 返回 null。
     */
    @Nullable
    public static KogasaAbility byId(String id) {
        for (KogasaAbility ability : values()) {
            if (ability.id.equals(id)) {
                return ability;
            }
        }
        return null;
    }

    /**
     * 按资源位置（取 path 作为 id）查找能力，未知返回 null。
     */
    @Nullable
    public static KogasaAbility byResourceLocation(@Nullable ResourceLocation loc) {
        return loc == null ? null : byId(loc.getPath());
    }
}
