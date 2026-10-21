package net.kogasachan.kogasa_addiction.ability;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum KogasaAbility {
    AYA("aya", "传统的幻想记者"),
    CIRNO("cirno", "冰之小妖精"),
    KOISHI("koishi", "紧闭的恋之瞳"),
    KOGASA("kogasa", "愉快的遗忘之伞"),
    TENSHI("tenshi", "非想非非想天之女"),
    SAKUYA("sakuya", "完美而潇洒的从者");

    public List<String> getObtainKeys() {
        return switch (this) {
            case KOGASA -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.kogasa.1");
            case AYA -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.aya.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.aya.2");
            case CIRNO -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.cirno.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.cirno.2");
            case KOISHI -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.koishi.1",
                    "tooltip.kogasa_addiction.ability_ingot.obtain.koishi.2");
            case TENSHI -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.tenshi.1");
            case SAKUYA -> List.of(
                    "tooltip.kogasa_addiction.ability_ingot.obtain.sakuya.1");
        };
    }

    private static final Map<String, KogasaAbility> BY_ID = new HashMap<>();

    static {
        for (KogasaAbility ability : values()) {
            BY_ID.put(ability.id, ability);
        }
    }

    private final String id;
    private final String displayName;

    KogasaAbility(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    @Nullable
    public static KogasaAbility byId(String id) {
        return BY_ID.get(id);
    }

    @Nullable
    public static KogasaAbility byResourceLocation(@Nullable ResourceLocation loc) {
        if (loc == null) return null;
        return byId(loc.getPath());
    }

    public String getId() {
        return id;
    }

    /**
     * 翻译键：ability.kogasa_addiction.<id>
     */
    public Component getDisplayName() {
        return Component.translatable("ability.kogasa_addiction." + id);
    }

    /**
     * 翻译键：ability.kogasa_addiction.<id>.desc
     */
    public Component getDescription() {
        return Component.translatable("ability.kogasa_addiction." + id + ".desc");
    }
}
