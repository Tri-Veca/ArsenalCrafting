package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.generator.AlloyRecipeDefinitions;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
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
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

/**
 * JEI category for the fluid (liquid) mixing recipes defined in
 * {@link AlloyRecipeDefinitions#LIQUID_MIXING_RECIPES}.
 *
 * Uses a dedicated background ({@code alloy_liquid_mixing_gui.png}) with two
 * equal tanks: the left holds the stacked input fluids and the right holds the
 * molten output. Both tanks draw real fluid sprites and offer hover tooltips.
 * Invisible alloy ingot/bucket output slots let item lookups (e.g. a Bronze
 * Ingot) surface the recipe.
 */
public class AlloyLiquidMixingRecipeCategory
        implements IRecipeCategory<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "alloy_liquid_mixing");
    public static final RecipeType<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> RECIPE_TYPE =
            new RecipeType<>(UID, AlloyRecipeDefinitions.LiquidMixingRecipeDefinition.class);

    // ── Crop of alloy_liquid_mixing_gui.png ──────────────────────────────────
    // The sheet is large, so we only sample the two tanks + arrow region.
    // No image resizing is done, which avoids the pixel interpolation/ghosting.
    private static final int SHEET_U = 40;
    private static final int SHEET_V = 44;
    private static final int BACKGROUND_WIDTH  = 166;
    private static final int BACKGROUND_HEIGHT = 72;

    // ── INPUT TANK (left) ────────────────────────────────────────────────────
    private static final int INPUT_TANK_X      = 7;
    private static final int INPUT_TANK_Y      = 7;
    private static final int INPUT_TANK_WIDTH  = 56;
    private static final int INPUT_TANK_HEIGHT = 58;

    // ── OUTPUT TANK (right) ──────────────────────────────────────────────────
    private static final int OUTPUT_TANK_X      = 103;
    private static final int OUTPUT_TANK_Y      = 7;
    private static final int OUTPUT_TANK_WIDTH  = 56;
    private static final int OUTPUT_TANK_HEIGHT = 58;

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable emptyDrawable;

    public AlloyLiquidMixingRecipeCategory(IGuiHelper helper) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                "textures/gui/container/alloy_liquid_mixing_gui.png");
        this.background = helper.createDrawable(texture, SHEET_U, SHEET_V,
                BACKGROUND_WIDTH, BACKGROUND_HEIGHT);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ALLOY_SMELTER.get()));
        this.emptyDrawable = helper.createBlankDrawable(0, 0);
    }

    @Override
    public RecipeType<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.literal("Alloy Liquid Mixing");
    }

    @Override
    public IDrawable getBackground() { return background; }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder,
                          AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe,
                          IFocusGroup focuses) {
        // Invisible item outputs (ingot + bucket) so focusing an alloy item shows this recipe
        AlloyMaterial material = AlloyMaterial.fromFluid(recipe.output().getFluid());
        if (material != null) {
            var ingotHolder = ModItems.ALLOY_ITEMS.get(material);
            if (ingotHolder != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, -1000, -1000)
                        .addItemStack(new ItemStack(ingotHolder.get()))
                        .setBackground(emptyDrawable, 0, 0);
            }

            var bucketHolder = ModItems.ALLOY_BUCKETS.get(material);
            if (bucketHolder != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, -1000, -1000)
                        .addItemStack(new ItemStack(bucketHolder.get()))
                        .setBackground(emptyDrawable, 0, 0);
            }
        }
    }

    @Override
    public void draw(AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe,
                     IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawInputTank(guiGraphics, recipe);
        drawOutputTank(guiGraphics, recipe);
    }

    /** Fills the left tank with the input fluids, stacked bottom-up by amount. */
    private void drawInputTank(GuiGraphics guiGraphics,
                               AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe) {
        List<FluidStack> inputs = recipe.inputs();
        int total = 0;
        for (FluidStack input : inputs) {
            if (input != null && !input.isEmpty()) total += input.getAmount();
        }
        if (total <= 0) return;

        int fillHeight = INPUT_TANK_HEIGHT;
        int currentY   = INPUT_TANK_Y + fillHeight;
        int usedHeight = 0;

        for (int i = 0; i < inputs.size(); i++) {
            FluidStack fluid = inputs.get(i);
            if (fluid == null || fluid.isEmpty()) continue;

            int fh = Math.round(fillHeight * ((float) fluid.getAmount() / total));
            if (i == inputs.size() - 1) fh = fillHeight - usedHeight;
            if (fh <= 0) fh = 1;

            renderFluid(guiGraphics, INPUT_TANK_X, currentY - fh,
                    INPUT_TANK_WIDTH, fh, fluid);
            currentY   -= fh;
            usedHeight += fh;
        }
    }

    /** Fills the right tank with the molten output fluid. */
    private void drawOutputTank(GuiGraphics guiGraphics,
                                AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe) {
        FluidStack output = recipe.output();
        if (output == null || output.isEmpty()) return;

        renderFluid(guiGraphics, OUTPUT_TANK_X, OUTPUT_TANK_Y,
                OUTPUT_TANK_WIDTH, OUTPUT_TANK_HEIGHT, output);
    }

    private void renderFluid(GuiGraphics guiGraphics, int x, int y,
                             int width, int height, FluidStack fluid) {
        var ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = ext.getStillTexture(fluid);
        if (still == null) still = ext.getStillTexture();
        if (still == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(still);

        int color = ext.getTintColor(fluid);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        // Use GuiGraphics color, not RenderSystem: raw shader color state can leak
        // into JEI's subsequent item rendering and double-tint alloy items.
        guiGraphics.setColor(r, g, b, a);
        guiGraphics.blit(x, y, 0, width, height, sprite);
        guiGraphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    @Override
    public List<Component> getTooltipStrings(AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe,
                                             IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        List<Component> tooltips = new ArrayList<>();

        if (isHovering(INPUT_TANK_X, INPUT_TANK_Y, INPUT_TANK_WIDTH, INPUT_TANK_HEIGHT, mouseX, mouseY)) {
            tooltips.add(Component.literal("Fluid Inputs")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltips.add(Component.empty());
            for (FluidStack input : recipe.inputs()) {
                if (input == null || input.isEmpty()) continue;
                tooltips.add(Component.literal("• " + input.getAmount() + " mb "
                                + input.getFluid().getFluidType().getDescription().getString())
                        .withStyle(ChatFormatting.YELLOW));
            }
        }

        if (isHovering(OUTPUT_TANK_X, OUTPUT_TANK_Y, OUTPUT_TANK_WIDTH, OUTPUT_TANK_HEIGHT, mouseX, mouseY)) {
            FluidStack output = recipe.output();
            tooltips.add(Component.literal("Molten Output")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltips.add(Component.empty());
            tooltips.add(Component.literal("• " + output.getAmount() + " mb "
                            + output.getFluid().getFluidType().getDescription().getString())
                    .withStyle(ChatFormatting.YELLOW));
        }

        return tooltips;
    }

    private static boolean isHovering(int x, int y, int width, int height,
                                      double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
