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
import net.kogasachan.kogasa_addiction.item.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnvilReinforcementCategory implements IRecipeCategory<AnvilReinforcementRecipe> {
    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(KogasaAddiction.MODID, "anvil_reinforcement");
    public static final RecipeType<AnvilReinforcementRecipe> RECIPE_TYPE =
            RecipeType.create(KogasaAddiction.MODID, "anvil_reinforcement", AnvilReinforcementRecipe.class);
    private final IDrawable icon;
    private final IDrawable background;
    private final IDrawable border;
    private final IDrawable outputBorder;
    private final IDrawable arrow;
    private final IDrawable plus;

    public AnvilReinforcementCategory(IGuiHelper guiHelper) {
        this.icon = guiHelper.createDrawableItemStack(new ItemStack(ModItems.KOGASA_SWORD.get()));

        // 不再引用 JEI 内部 atlas 路径（15.74 已把 textures/jei/atlas/gui/* 改为
        // textures/gui/sprites/*，写死旧路径会在新版 JEI 上丢贴图）。改用 JEI 提供的 drawable。
        this.background = guiHelper.createBlankDrawable(125, 38);
        this.border = guiHelper.getSlotDrawable();
        // 输出槽也用普通槽位框：JEI 的 getOutputSlot() 是加大的 26x26，会和 18x18 的物品格不匹配
        this.outputBorder = guiHelper.getSlotDrawable();
        this.arrow = guiHelper.getRecipeArrow();
        this.plus = guiHelper.getRecipePlusSign();
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
        builder.addInputSlot(1, 1).addItemStacks(recipe.inputs());
        builder.addInputSlot(50, 1).addItemStacks(recipe.materials());
        builder.addOutputSlot(108, 1)
                .addItemStacks(recipe.outputs())
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
                        case AMMO_YH -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.ammo_yh")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                        case AMMO_YH_CUSTOM -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.ammo_yh_custom")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                        case AMMO_SPELL -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.ammo_spell")
                                .withStyle(ChatFormatting.LIGHT_PURPLE));
                        case REPAIR -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.repair")
                                .withStyle(ChatFormatting.BLUE));
                        case CLEAR -> tooltip.add(Component.translatable(
                                        "jei.kogasa_addiction.hint.clear")
                                .withStyle(ChatFormatting.WHITE));
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

        // 经验成本
        int cost = switch (recipe.kind()) {
            case ABILITY -> Config.ABILITY_INFUSION_COST.get();
            case REINFORCEMENT -> Config.REINFORCEMENT_COST.get();
            case AMMO, AMMO_YH, AMMO_YH_CUSTOM, AMMO_SPELL -> Config.AMMO_INFUSION_COST.get();
            case REPAIR -> Config.SAKUYA_KNIFE_REPAIR_COST.get();
            case CLEAR -> Config.CLEAR_COST.get();
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
