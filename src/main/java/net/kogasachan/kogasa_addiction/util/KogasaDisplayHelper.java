package net.kogasachan.kogasa_addiction.util;

import net.kogasachan.kogasa_addiction.ability.KogasaAbilityData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

/**
 * 物品/药水/烟花显示名与描述行解析。放在 common 侧，供命令与客户端 Tooltip 共用，
 * 避免服务端代码（/kogasa info）引用只存在于客户端的 Tooltip 处理类。
 */
public final class KogasaDisplayHelper {

    private KogasaDisplayHelper() {
    }

    /**
     * 把 "minecraft:iron_sword" 或 "arrow:minecraft:arrow" 转成当前语言下的显示名。
     */
    public static Component getItemDisplayName(String id) {
        String actualId = id;
        String variant = null;
        int hash = actualId.indexOf('#');
        if (hash >= 0) {
            variant = actualId.substring(hash + 1);
            actualId = actualId.substring(0, hash);
        }

        boolean isArrow = false;
        if (actualId.startsWith("arrow:")) {
            actualId = actualId.substring("arrow:".length());
            isArrow = true;
        }

        ResourceLocation loc = ResourceLocation.tryParse(actualId);
        if (loc == null) return Component.literal(id);

        var item = ForgeRegistries.ITEMS.getValue(loc);
        if (item == null || item == Items.AIR) return Component.literal(id);

        Component name = new ItemStack(item).getHoverName();
        if (isArrow) {
            // 显示"16× 箭"而不是"箭"，提示消耗数量
            return Component.translatable(
                    "tooltip.kogasa_addiction.reinforcement.entry.arrow", name);
        }
        if (variant != null && !variant.isBlank()) {
            // SlashBlade 的变体键是翻译键，显示真实刀名而不是内部去重键。
            if (variant.startsWith("item.") || variant.startsWith("block.")
                    || variant.startsWith("entity.")) {
                return Component.translatable(variant);
            }
            return Component.literal(variant);
        }
        return name;
    }

    /**
     * 按弹药类型（普通/喷溅/滞留）构造药水显示名。
     */
    public static Component getPotionDisplayName(ResourceLocation potionId,
                                                 KogasaAbilityData.PotionKind kind) {
        var potion = ForgeRegistries.POTIONS.getValue(potionId);
        if (potion == null) return Component.literal(potionId.toString());

        ItemStack potionStack = switch (kind) {
            case REGULAR -> new ItemStack(Items.POTION);
            case SPLASH -> new ItemStack(Items.SPLASH_POTION);
            case LINGERING -> new ItemStack(Items.LINGERING_POTION);
        };
        PotionUtils.setPotion(potionStack, potion);
        return potionStack.getHoverName();
    }

    /**
     * 解析烟花火箭的 NBT，返回多行描述
     */
    public static List<Component> getFireworkInfo(ItemStack fireworkStack) {
        List<Component> lines = new ArrayList<>();
        CompoundTag nbt = fireworkStack.getTag();
        if (nbt == null || !nbt.contains("Fireworks", Tag.TAG_COMPOUND)) {
            return lines;
        }

        CompoundTag fireworks = nbt.getCompound("Fireworks");

        // 飞行时间
        if (fireworks.contains("Flight", Tag.TAG_BYTE)) {
            int flight = fireworks.getByte("Flight");
            lines.add(Component.translatable(
                            "tooltip.kogasa_addiction.ammo.firework.flight", flight)
                    .withStyle(ChatFormatting.GRAY));
        }

        // 爆炸列表
        ListTag explosions = fireworks.getList("Explosions", Tag.TAG_COMPOUND);
        if (explosions.isEmpty()) {
            lines.add(Component.translatable(
                            "tooltip.kogasa_addiction.ammo.firework.empty")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return lines;
        }

        for (int i = 0; i < explosions.size(); i++) {
            CompoundTag exp = explosions.getCompound(i);
            lines.add(buildExplosionLine(exp, i + 1));
        }

        return lines;
    }

    /**
     * 药水效果行："  名称 [等级]"，灰色。
     */
    public static Component buildEffectLine(MobEffectInstance effect) {
        Component name = effect.getEffect().getDisplayName();
        int level = effect.getAmplifier() + 1;

        MutableComponent line = Component.literal("  ").append(name);

        if (level > 1) {
            line.append(Component.literal(" ")
                    .append(Component.translatable("enchantment.level." + level)));
        }

        return line.withStyle(ChatFormatting.GRAY);
    }

    private static Component buildExplosionLine(CompoundTag exp, int index) {
        // 形状
        int type = exp.getByte("Type");
        String shapeKey = switch (type) {
            case 1 -> "item.minecraft.firework_star.shape.large_ball";
            case 2 -> "item.minecraft.firework_star.shape.star";
            case 3 -> "item.minecraft.firework_star.shape.creeper";
            case 4 -> "item.minecraft.firework_star.shape.burst";
            default -> "item.minecraft.firework_star.shape.small_ball";
        };

        MutableComponent line = Component.literal("  " + index + ". ")
                .append(Component.translatable(shapeKey));

        // 颜色（主色 + 渐变色）
        int[] colors = exp.getIntArray("Colors");
        int[] fadeColors = exp.getIntArray("FadeColors");

        if (colors.length > 0) {
            line.append(Component.literal(" "));
            for (int i = 0; i < colors.length; i++) {
                if (i > 0) line.append(Component.literal("/"));
                line.append(colorComponent(colors[i]));
            }
        }
        if (fadeColors.length > 0) {
            line.append(Component.literal(" → "));
            for (int i = 0; i < fadeColors.length; i++) {
                if (i > 0) line.append(Component.literal("/"));
                line.append(colorComponent(fadeColors[i]));
            }
        }

        // 拖尾 / 闪烁
        List<Component> flags = new ArrayList<>();
        if (exp.getBoolean("Trail")) {
            flags.add(Component.translatable(
                    "tooltip.kogasa_addiction.ammo.firework.trail"));
        }
        if (exp.getBoolean("Flicker")) {
            flags.add(Component.translatable(
                    "tooltip.kogasa_addiction.ammo.firework.flicker"));
        }
        if (!flags.isEmpty()) {
            line.append(Component.literal(" ["));
            for (int i = 0; i < flags.size(); i++) {
                if (i > 0) line.append(Component.literal(", "));
                line.append(flags.get(i));
            }
            line.append(Component.literal("]"));
        }

        return line.withStyle(ChatFormatting.LIGHT_PURPLE);
    }

    /**
     * int RGB → 颜色名 Component（优先匹配原版染料色）
     */
    private static Component colorComponent(int rgb) {
        for (DyeColor dye : DyeColor.values()) {
            if (dye.getFireworkColor() == rgb) {
                return Component.translatable(
                                "item.minecraft.firework_star." + dye.getName())
                        .withStyle(style -> style.withColor(dye.getTextColor()));
            }
        }
        // 非原版颜色：显示十六进制
        return Component.literal(String.format("#%06X", rgb))
                .withStyle(ChatFormatting.GRAY);
    }
}
