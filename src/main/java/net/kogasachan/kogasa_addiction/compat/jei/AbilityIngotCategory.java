package net.kogasachan.kogasa_addiction.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.ITooltipBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.kogasachan.kogasa_addiction.ability.effect.AbilityEffect;
import net.kogasachan.kogasa_addiction.ability.effect.AbilityEffectCategory;
import net.kogasachan.kogasa_addiction.ability.effect.AbilityEffectValues;
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 能力锭的 JEI 信息页：左侧锭、右侧可注入装备（多件时由 JEI 槽位循环），
 * 顶部标题，正文按通用 / 近战 / 远程 / 护甲分组；悬停条目显示具体说明。
 */
public class AbilityIngotCategory implements IRecipeCategory<AbilityIngotRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "ability_ingot_effects");
    public static final RecipeType<AbilityIngotRecipe> RECIPE_TYPE =
            RecipeType.create(KogasaAddiction.MODID, "ability_ingot_effects", AbilityIngotRecipe.class);

    private static final int WIDTH = 180;
    private static final int HEIGHT = 135;
    private static final int BODY_START_Y = 46;
    private static final int ROW_HEIGHT = 9;
    private static final int HEADER_HEIGHT = 10;
    private static final int PANEL_COLOR = 0xFF8B8B8B;
    private static final int BORDER_COLOR = 0xFFC6C6C6;

    private final IDrawable icon;

    public AbilityIngotCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.KOGASA_INGOT.get()));
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AbilityIngotRecipe recipe, @NotNull IFocusGroup focuses) {
        builder.addInputSlot(4, 22).addItemStack(recipe.ingot());
        if (!recipe.outputs().isEmpty()) {
            builder.addOutputSlot(WIDTH - 22, 22).addItemStacks(recipe.outputs());
        }
    }

    @Override
    public @NotNull RecipeType<AbilityIngotRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.kogasa_addiction.category.ability_ingot_effects");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public void draw(AbilityIngotRecipe recipe, @NotNull IRecipeSlotsView slots,
                     @NotNull GuiGraphics graphics, double mouseX, double mouseY) {
        // 深色面板：与 JEI 默认浅灰背景拉开对比
        graphics.fill(0, 0, WIDTH, HEIGHT, PANEL_COLOR);
        graphics.fill(0, 0, WIDTH, 1, BORDER_COLOR);
        graphics.fill(0, HEIGHT - 1, WIDTH, HEIGHT, BORDER_COLOR);
        graphics.fill(0, 0, 1, HEIGHT, BORDER_COLOR);
        graphics.fill(WIDTH - 1, 0, WIDTH, HEIGHT, BORDER_COLOR);

        Font font = Minecraft.getInstance().font;

        // 顶部居中：能力锭名称 + 能力名
        String title = recipe.ingot().getHoverName().getString()
                + " · " + recipe.ability().getDisplayName().getString();
        int titleWidth = font.width(title);
        graphics.drawString(font, title, Math.max(2, (WIDTH - titleWidth) / 2), 5, 0xFFFFD966, true);

        // 分类与条目
        for (Row row : buildRows(recipe)) {
            if (row.header()) {
                graphics.drawString(font, Component.translatable(row.effect().category().getNameKey()),
                        6, row.y(), categoryColor(row.effect().category()), true);
            } else {
                graphics.drawString(font,
                        Component.literal("· ").append(Component.translatable(row.effect().nameKey())),
                        10, row.y(), 0xFFFFFFFF, true);
            }
        }

    }

    @Override
    public void getTooltip(@NotNull ITooltipBuilder tooltip, @NotNull AbilityIngotRecipe recipe,
                           @NotNull IRecipeSlotsView slots, double mouseX, double mouseY) {
        for (Row row : buildRows(recipe)) {
            if (row.header()) continue;
            if (mouseX >= 6 && mouseX <= 165 && mouseY >= row.y() - 1 && mouseY < row.y() + ROW_HEIGHT - 1) {
                tooltip.add(Component.translatable(row.effect().nameKey()).withStyle(ChatFormatting.GOLD));
                tooltip.add(Component.translatable(row.effect().descriptionKey())
                        .withStyle(ChatFormatting.GRAY));
                Component current = AbilityEffectValues.get(row.effect());
                if (current != null) {
                    tooltip.add(Component.empty());
                    tooltip.add(current.copy().withStyle(ChatFormatting.YELLOW));
                }
                return;
            }
        }
    }

    private static int categoryColor(AbilityEffectCategory category) {
        return switch (category) {
            case GENERAL -> 0xFFFFD966;
            case MELEE -> 0xFFFF7070;
            case RANGED -> 0xFF70C8FF;
            case ARMOR -> 0xFFB78CFF;
        };
    }

    private static List<Row> buildRows(AbilityIngotRecipe recipe) {
        List<Row> rows = new ArrayList<>();
        int y = BODY_START_Y;
        for (AbilityEffectCategory category : AbilityEffectCategory.values()) {
            List<AbilityEffect> effects = recipe.effects().stream()
                    .filter(e -> e.category() == category).toList();
            if (effects.isEmpty()) continue;

            rows.add(new Row(y, new AbilityEffect("", category, "", ""), true));
            y += HEADER_HEIGHT;
            for (AbilityEffect effect : effects) {
                rows.add(new Row(y, effect, false));
                y += ROW_HEIGHT;
            }
        }
        return rows;
    }

    private record Row(int y, AbilityEffect effect, boolean header) {
    }
}
