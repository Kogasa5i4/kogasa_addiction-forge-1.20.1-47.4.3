package net.kogasachan.kogasa_addiction.item;

import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.ability.KogasaAbility;
import net.kogasachan.kogasa_addiction.util.KogasaNumberFormatHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class KogasaAbilityIngotItem extends Item {

    private final KogasaAbility ability;

    public KogasaAbilityIngotItem(KogasaAbility ability, Properties properties) {
        super(properties);
        this.ability = ability;
    }

    public KogasaAbility getAbility() {
        return ability;
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @Nullable Level level,
                                List<Component> tooltip, @NotNull TooltipFlag flag) {
        // 1. 能力名
        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.ability",
                        ability.getDisplayName())
                .withStyle(ChatFormatting.AQUA));

        // 2. 能力描述
        tooltip.add(ability.getDescription()
                .copy()
                .withStyle(ChatFormatting.GRAY));

        // 3. 空行分隔
        tooltip.add(Component.empty());

        // 4. 获取方式
        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.obtain.header")
                .withStyle(ChatFormatting.YELLOW));
        for (String key : ability.getObtainKeys()) {
            tooltip.add(createObtainLine(key));
        }

        // 5. 空行 + 使用提示
        tooltip.add(Component.empty());
        tooltip.add(Component.translatable(
                        "tooltip.kogasa_addiction.ability_ingot.usage")
                .withStyle(ChatFormatting.DARK_GRAY));

        super.appendHoverText(stack, level, tooltip, flag);
    }

    /** Build acquisition hints from the active config so the tooltip stays accurate after config changes. */
    private Component createObtainLine(String key) {
        Component result = switch (ability) {
            case AYA -> {
                String chance = formatPercent(Config.SHOOT_INGOT_CHANCE.get());
                int cooldownSeconds = Config.SHOOT_COOLDOWN_SECONDS.get();
                Component cooldown = cooldownSeconds <= 0
                        ? Component.translatable("tooltip.kogasa_addiction.cooldown.none")
                        : Component.translatable("tooltip.kogasa_addiction.cooldown.seconds", cooldownSeconds);
                yield Component.translatable(key, chance, cooldown);
            }
            case CIRNO -> Component.translatable(
                    key, formatPercent(Config.CIRNO_INGOT_DROP_CHANCE.get()));
            case KOGASA -> Component.translatable(
                    key, formatPercent(Config.KOGASA_INGOT_CHANCE.get()));
            case KOISHI -> {
                if (key.endsWith(".2")) {
                    yield Component.translatable(
                            key, Config.KOISHI_GHAST_KILLS_REQUIRED.get());
                }
                yield Component.translatable(key);
            }
            case TENSHI -> Component.translatable(key,
                    Config.TENSHI_INGOT_MIN_Y.get(), formatPercent(Config.TENSHI_INGOT_CHANCE.get()));
            case SAKUYA -> Component.translatable(key,
                    formatPercent(Config.SAKUYA_INGOT_CHANCE.get()));
        };
        return result.copy().withStyle(ChatFormatting.GRAY);
    }

    private static String formatPercent(double chance) {
        if (chance > 0.0 && chance < 0.0001) {
            return "<0.01%";
        }
        return KogasaNumberFormatHelper.formatPercentTrimmed(chance, 2);
    }
}