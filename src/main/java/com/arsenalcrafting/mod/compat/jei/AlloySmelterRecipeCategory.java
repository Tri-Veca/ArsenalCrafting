package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.mojang.blaze3d.systems.RenderSystem;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

import java.util.ArrayList;
import java.util.List;

public class AlloySmelterRecipeCategory implements IRecipeCategory<AlloyRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "alloy_smelting");
    public static final RecipeType<AlloyRecipe> RECIPE_TYPE = new RecipeType<>(UID, AlloyRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

    // Fluid tank area coordinates (matching your screen)
    private static final int FLUID_TANK_X = 151;
    private static final int FLUID_TANK_Y = 17;
    private static final int FLUID_TANK_WIDTH = 16;
    private static final int FLUID_TANK_HEIGHT = 60;

    public AlloySmelterRecipeCategory(IGuiHelper helper) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                "textures/gui/container/alloy_smelter_gui.png");
        this.background = helper.createDrawable(texture, 0, 0, 176, 85);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ALLOY_SMELTER.get()));
    }

    @Override
    public RecipeType<AlloyRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.arsenalcrafting.alloy_smelter");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlloyRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());

        // Input slots (0-8) in 3x3 grid
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;
            int xPos = 10 + col * 18;
            int yPos = 19 + row * 18;

            if (i < ingredients.size()) {
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, yPos)
                        .addIngredients(ingredients.get(i));
            }
        }

        // Output slot (slot 9) at position (143,35)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 143, 35)
                .addItemStack(recipe.getResultItem(null))
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.literal("Output").withStyle(ChatFormatting.GREEN));

                    int craftTime = recipe.getCraftTime();
                    int lavaCost = recipe.getLavaCost();
                    int craftSeconds = craftTime / 20;
                    int lavaBuckets = lavaCost / 1000;
                    int lavaMb = lavaCost % 1000;

                    tooltip.add(Component.empty());
                    tooltip.add(Component.literal("Recipe Info:").withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.literal("  ⏱ Craft Time: " + craftSeconds + " sec").withStyle(ChatFormatting.GREEN));

                    if (lavaCost > 0) {
                        if (lavaBuckets > 0) {
                            tooltip.add(Component.literal("  🔥 Lava: " + lavaBuckets + " bucket" + (lavaBuckets > 1 ? "s" : "") +
                                    (lavaMb > 0 ? " + " + lavaMb + " mb" : "")).withStyle(ChatFormatting.RED));
                        } else {
                            tooltip.add(Component.literal("  🔥 Lava: " + lavaCost + " mb").withStyle(ChatFormatting.RED));
                        }
                    }
                });
    }

    @Override
    public void draw(AlloyRecipe recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        // Draw the fluid tank full regardless of lava cost
        drawFluidTank(guiGraphics);
    }

    private void drawFluidTank(GuiGraphics guiGraphics) {
        // Always fill the entire tank for visual consistency
        var fluidExtensions = IClientFluidTypeExtensions.of(Fluids.LAVA);
        ResourceLocation stillTexture = fluidExtensions.getStillTexture();
        if (stillTexture == null) stillTexture = fluidExtensions.getStillTexture();

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTexture);

        int fluidY = FLUID_TANK_Y; // Full tank starts at the top

        int color = fluidExtensions.getTintColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        // Save state
        float lastR = RenderSystem.getShaderColor()[0];
        float lastG = RenderSystem.getShaderColor()[1];
        float lastB = RenderSystem.getShaderColor()[2];
        float lastA = RenderSystem.getShaderColor()[3];
        int lastTexture = RenderSystem.getShaderTexture(0);

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderColor(r, g, b, a);

        // Draw full tank height
        guiGraphics.blit(
                FLUID_TANK_X,
                fluidY,
                0,
                FLUID_TANK_WIDTH,
                FLUID_TANK_HEIGHT -4,
                sprite
        );

        // Restore
        RenderSystem.setShaderTexture(0, lastTexture);
        RenderSystem.setShaderColor(lastR, lastG, lastB, lastA);
    }

    @Override
    public List<Component> getTooltipStrings(AlloyRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        List<Component> tooltips = new ArrayList<>();

        if (mouseX >= FLUID_TANK_X && mouseX <= FLUID_TANK_X + FLUID_TANK_WIDTH &&
                mouseY >= FLUID_TANK_Y && mouseY <= FLUID_TANK_Y + FLUID_TANK_HEIGHT) {

            int lavaCost = recipe.getLavaCost();
            int craftTime = recipe.getCraftTime();
            int craftSeconds = craftTime / 20;
            int lavaBuckets = lavaCost / 1000;
            int lavaMb = lavaCost % 1000;

            tooltips.add(Component.literal("Lava Required").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltips.add(Component.empty());

            if (lavaBuckets > 0) {
                tooltips.add(Component.literal("• " + lavaBuckets + " bucket" + (lavaBuckets > 1 ? "s" : "") +
                        (lavaMb > 0 ? " + " + lavaMb + " mb" : "")).withStyle(ChatFormatting.RED));
            } else {
                tooltips.add(Component.literal("• " + lavaCost + " mb").withStyle(ChatFormatting.RED));
            }

            tooltips.add(Component.literal("• Craft Time: " + craftSeconds + " seconds").withStyle(ChatFormatting.GREEN));
            tooltips.add(Component.empty());
            tooltips.add(Component.literal("Drawn from adjacent Lava Tanks").withStyle(ChatFormatting.DARK_GRAY));
        }

        return tooltips;
    }
}