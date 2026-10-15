package net.kogasachan.kogasa_addiction.compat.jei;

import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.kogasachan.kogasa_addiction.Config;
import net.kogasachan.kogasa_addiction.KogasaAddiction;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnvilReinforcementCategory implements IRecipeCategory<AnvilReinforcementRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "anvil_reinforcement");
    public static final RecipeType<AnvilReinforcementRecipe> RECIPE_TYPE =
            RecipeType.create(KogasaAddiction.MODID, "anvil_reinforcement", AnvilReinforcementRecipe.class);
    private static final ResourceLocation RECIPE_GUI_VANILLA =
            ResourceLocation.fromNamespaceAndPath("jei", "textures/gui/gui_vanilla.png");
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable border;
    private final IDrawable outputBorder;
    private final IDrawable arrow;
    private final IDrawable plus;

    public AnvilReinforcementCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(Items.ANVIL));

        // 背景
        this.background = guiHelper.createDrawable(
                ResourceLocation.fromNamespaceAndPath("jei", "textures/jei/atlas/gui/gui_background.png"),
                0, 168, 125, 18);

        // 槽位边框（18×18）
        this.border = guiHelper.drawableBuilder(
                        ResourceLocation.fromNamespaceAndPath("jei", "textures/jei/atlas/gui/slot.png"),
                        0, 0, 18, 18)
                .setTextureSize(18, 18)   // 关键：告诉 JEI 纹理实际尺寸
                .build();

        // 输出槽位边框（18×18）
        this.outputBorder = guiHelper.drawableBuilder(
                        ResourceLocation.fromNamespaceAndPath("jei", "textures/jei/atlas/gui/output_slot.png"),
                        0, 0, 18, 18)
                .setTextureSize(18, 18)
                .build();

        // 箭头（22×16）
        this.arrow = guiHelper.drawableBuilder(
                        ResourceLocation.fromNamespaceAndPath("jei", "textures/jei/atlas/gui/recipe_arrow.png"),
                        0, 0, 22, 16)
                .setTextureSize(22, 16)
                .build();

        // 加号（13×13）
        this.plus = guiHelper.drawableBuilder(
                        ResourceLocation.fromNamespaceAndPath("jei", "textures/jei/atlas/gui/recipe_plus_sign.png"),
                        0, 0, 13, 13)
                .setTextureSize(13, 13)
                .build();
    }

    /**
     * 检查玩家是否有足够的经验等级
     */
    private static boolean playerHasEnoughLevels(int cost) {
        var player = Minecraft.getInstance().player;
        if (player == null) return false;

        // 创造模式玩家不需要经验
        if (player.getAbilities().instabuild) return true;

        return player.experienceLevel >= cost;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AnvilReinforcementRecipe recipe,
                          @NotNull IFocusGroup focuses) {
        builder.addInputSlot(1, 1).addItemStack(recipe.input());
        builder.addInputSlot(50, 1).addItemStacks(recipe.materials());
        builder.addOutputSlot(108, 1)
                .addItemStack(recipe.output())
                .addRichTooltipCallback((slotView, tooltip) -> {
                    switch (recipe.kind()) {
                        case ABILITY -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.ability")
                                .withStyle(ChatFormatting.AQUA));
                        case REINFORCEMENT -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.reinforcement")
                                .withStyle(ChatFormatting.GOLD));
                        case AMMO -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.ammo")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                    }
                });
    }

    @Override
    public @NotNull RecipeType<AnvilReinforcementRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public @NotNull Component getTitle() {
        return Component.translatable("jei.kogasa_addiction.category.anvil_reinforcement");
    }

    @Override
    public @Nullable IDrawable getIcon() {
        return icon;
    }

    @Override
    public int getWidth() {
        return 125; // 配方界面的宽度（像素）
    }

    @Override
    public int getHeight() {
        return 38;  // 配方界面的高度（像素）
    }

    @Override
    public void draw(@NotNull AnvilReinforcementRecipe recipe, @NotNull IRecipeSlotsView recipeSlotsView,
                     @NotNull GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // 背景
        background.draw(guiGraphics, 0, 0);

        // 三个槽位的边框
        border.draw(guiGraphics, 0, 0);        // 输入
        border.draw(guiGraphics, 49, 0);       // 材料
        outputBorder.draw(guiGraphics, 107, 0); // 输出

        // 加号
        plus.draw(guiGraphics, 27, 3);

        // 箭头
        arrow.draw(guiGraphics, 76, 1);

        // 经验消耗
        // 经验成本根据类型选择
        int cost = switch (recipe.kind()) {
            case ABILITY -> Config.ABILITY_INFUSION_COST.get();
            case REINFORCEMENT -> Config.REINFORCEMENT_COST.get();
            case AMMO -> Config.AMMO_INFUSION_COST.get();
        };
        String experienceString = Component.translatable(
                "jei.kogasa_addiction.anvil.experience", cost).getString();

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        int width = font.width(experienceString);
        int mainColor = playerHasEnoughLevels(cost) ? 0xFF80FF20 : 0xFFFF6060;
        // 右下角坐标：配方宽度 - 文字宽度 - 边距，配方高度 - 文字高度 - 边距
        guiGraphics.drawString(font, experienceString,
                getWidth() - width - 2, getHeight() - 11, mainColor, true);
    }

//    @Deprecated
//    @Override
//    public @Nullable IDrawable getBackground() {
//        return background;
//    }

    @Override
    public @Nullable ResourceLocation getRegistryName(AnvilReinforcementRecipe recipe) {
        ResourceLocation inputId = ForgeRegistries.ITEMS.getKey(recipe.input().getItem());
        if (inputId == null) return null;

        String materialPart = "none";
        if (!recipe.materials().isEmpty()) {
            ItemStack first = recipe.materials().get(0);
            ResourceLocation matId = ForgeRegistries.ITEMS.getKey(first.getItem());
            if (matId != null) {
                materialPart = matId.getPath();
                if (first.getCount() > 1) {
                    materialPart += "_x" + first.getCount();
                }
            }
        }

        return ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID,
                "anvil/" + recipe.kind().name().toLowerCase()
                        + "/" + inputId.getPath()
                        + "/" + materialPart);
    }
}
