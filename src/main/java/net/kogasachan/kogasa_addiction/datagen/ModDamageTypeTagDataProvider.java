package net.kogasachan.kogasa_addiction.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

public class ModDamageTypeTagDataProvider implements DataProvider {
    private final PackOutput output;

    public ModDamageTypeTagDataProvider(PackOutput output) {
        this.output = output;
    }

    @Override
    public @NotNull CompletableFuture<?> run(@NotNull CachedOutput cache) {
        Path bypasses_armor = this.output.getOutputFolder()
                .resolve("data/minecraft/tags/damage_type/bypasses_armor.json");
        Path bypasses_effects = this.output.getOutputFolder()
                .resolve("data/minecraft/tags/damage_type/bypasses_effects.json");
        Path bypasses_enchantments = this.output.getOutputFolder()
                .resolve("data/minecraft/tags/damage_type/bypasses_enchantments.json");
        Path bypasses_resistance = this.output.getOutputFolder()
                .resolve("data/minecraft/tags/damage_type/bypasses_resistance.json");
        Path bypasses_shield = this.output.getOutputFolder()
                .resolve("data/minecraft/tags/damage_type/bypasses_shield.json");
        Path always_hurts_ender_dragons = this.output.getOutputFolder()
                .resolve("data/minecraft/tags/damage_type/always_hurts_ender_dragons.json");

        // 刻意不挂 bypasses_invulnerability / bypasses_cooldown：
        // 前者会让本模组远程伤害打穿创造模式与无敌帧，后者会让同一目标失去受击冷却，
        // 两者叠加等于无限制穿甲连击，平衡与 PVP 风险过大。
        return CompletableFuture.allOf(
                save(cache, bypasses_armor),
                save(cache, bypasses_effects),
                save(cache, bypasses_enchantments),
                save(cache, bypasses_resistance),
                save(cache, bypasses_shield),
                save(cache, always_hurts_ender_dragons)
        );
    }

    private CompletableFuture<?> save(CachedOutput cache, Path path) {
        JsonObject json = new JsonObject();
        json.addProperty("replace", false);
        JsonArray values = new JsonArray();
        values.add("kogasa_addiction:koishi_ranged");
        json.add("values", values);
        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public @NotNull String getName() {
        return "Mod Damage Type Tags";
    }
}