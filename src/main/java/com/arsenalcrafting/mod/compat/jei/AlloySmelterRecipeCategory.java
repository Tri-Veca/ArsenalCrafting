package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModFluids;
import com.arsenalcrafting.mod.registry.ModItems;
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
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.ArrayList;
import java.util.List;

public class AlloySmelterRecipeCategory implements IRecipeCategory<AlloyRecipe> {

    public static final ResourceLocation UID =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "alloy_smelting");
    public static final RecipeType<AlloyRecipe> RECIPE_TYPE =
            new RecipeType<>(UID, AlloyRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IDrawable emptyDrawable; // invisible slot background

    private static final int FLUID_TANK_X      = 151;
    private static final int FLUID_TANK_Y      = 17;
    private static final int FLUID_TANK_WIDTH  = 16;
    private static final int FLUID_TANK_HEIGHT = 60;

    // ── OUTPUT TANK (new — adjust X/Y to match your GUI texture) ─────────────
    private static final int OUTPUT_TANK_X      = 87;  // adjust to fit your GUI layout
    private static final int OUTPUT_TANK_Y      = 19;
    private static final int OUTPUT_TANK_WIDTH  = 56;
    private static final int OUTPUT_TANK_HEIGHT = 58;


    public AlloySmelterRecipeCategory(IGuiHelper helper) {
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                "textures/gui/container/alloy_smelter_gui.png");
        this.background    = helper.createDrawable(texture, 0, 0, 176, 85);
        this.icon          = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.ALLOY_SMELTER.get()));
        this.emptyDrawable = helper.createBlankDrawable(0, 0);
    }

    @Override
    public RecipeType<AlloyRecipe> getRecipeType() { return RECIPE_TYPE; }

    @Override
    public Component getTitle() {
        return Component.translatable("block.arsenalcrafting.alloy_smelter");
    }

    @Override
    public IDrawable getBackground() { return background; }

    @Override
    public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AlloyRecipe recipe, IFocusGroup focuses) {
        List<Ingredient> ingredients = new ArrayList<>(recipe.getIngredients());

        // Input slots (0-8) in 3x3 grid
        for (int i = 0; i < 9; i++) {
            int row  = i / 3;
            int col  = i % 3;
            int xPos = 10 + col * 18;
            int yPos = 19 + row * 18;
            if (i < ingredients.size()) {
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, yPos)
                        .addIngredients(ingredients.get(i));
            }
        }

        // Dummy invisible output slots - placed outside visible area
        AlloyMaterial material = AlloyMaterial.fromName(recipe.getResultMaterial());
        if (material != null) {

            // Invisible ingot slot - placed outside visible area
            var ingotHolder = ModItems.ALLOY_ITEMS.get(material);
            if (ingotHolder != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, -100, -100)
                        .addItemStack(new ItemStack(ingotHolder.get()))
                        .setBackground(emptyDrawable, 0, 0);
            }

            // Invisible bucket slot - placed outside visible area
            var bucketHolder = ModItems.ALLOY_BUCKETS.get(material);
            if (bucketHolder != null) {
                builder.addSlot(RecipeIngredientRole.OUTPUT, -200, -200)
                        .addItemStack(new ItemStack(bucketHolder.get()))
                        .setBackground(emptyDrawable, 0, 0);
            }
        }
    }

    @Override
    public void draw(AlloyRecipe recipe, IRecipeSlotsView recipeSlotsView,
                     GuiGraphics guiGraphics, double mouseX, double mouseY) {
        drawLavaTank(guiGraphics);
        drawOutputTank(guiGraphics, recipe);
    }

    private void drawLavaTank(GuiGraphics guiGraphics) {
        var fluidExtensions   = IClientFluidTypeExtensions.of(Fluids.LAVA);
        ResourceLocation still = fluidExtensions.getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);

        int color = fluidExtensions.getTintColor();
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        float[] last = RenderSystem.getShaderColor();
        int lastTex  = RenderSystem.getShaderTexture(0);

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderColor(r, g, b, a);
        guiGraphics.blit(FLUID_TANK_X, FLUID_TANK_Y, 0,
                FLUID_TANK_WIDTH, FLUID_TANK_HEIGHT - 4, sprite);
        RenderSystem.setShaderTexture(0, lastTex);
        RenderSystem.setShaderColor(last[0], last[1], last[2], last[3]);
    }

    private void drawOutputTank(GuiGraphics guiGraphics, AlloyRecipe recipe) {
        AlloyMaterial material = AlloyMaterial.fromName(recipe.getResultMaterial());
        if (material == null) return;
        var sourceFluid = ModFluids.getSource(material);
        if (sourceFluid == null) return;

        FluidStack fluidStack  = new FluidStack(sourceFluid, recipe.getResultAmountMb());
        var fluidExtensions    = IClientFluidTypeExtensions.of(sourceFluid);
        ResourceLocation still = fluidExtensions.getStillTexture(fluidStack);
        if (still == null) still = fluidExtensions.getStillTexture();

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);

        int color = fluidExtensions.getTintColor(fluidStack);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;
        float[] last = RenderSystem.getShaderColor();
        int lastTex  = RenderSystem.getShaderTexture(0);

        RenderSystem.setShaderTexture(0, TextureAtlas.LOCATION_BLOCKS);
        RenderSystem.setShaderColor(r, g, b, a);
        guiGraphics.blit(OUTPUT_TANK_X, OUTPUT_TANK_Y, 0,
                OUTPUT_TANK_WIDTH, OUTPUT_TANK_HEIGHT - 4, sprite);
        RenderSystem.setShaderTexture(0, lastTex);
        RenderSystem.setShaderColor(last[0], last[1], last[2], last[3]);
    }

    @Override
    public List<Component> getTooltipStrings(AlloyRecipe recipe, IRecipeSlotsView recipeSlotsView,
                                             double mouseX, double mouseY) {
        List<Component> tooltips = new ArrayList<>();

        if (mouseX >= FLUID_TANK_X && mouseX <= FLUID_TANK_X + FLUID_TANK_WIDTH &&
                mouseY >= FLUID_TANK_Y && mouseY <= FLUID_TANK_Y + FLUID_TANK_HEIGHT) {
            int lavaCost     = recipe.getLavaCost();
            int craftSeconds = recipe.getCraftTime() / 20;
            int lavaBuckets  = lavaCost / 1000;
            int lavaMb       = lavaCost % 1000;
            tooltips.add(Component.literal("Lava Required")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltips.add(Component.empty());
            if (lavaBuckets > 0) {
                tooltips.add(Component.literal("• " + lavaBuckets + " bucket"
                                + (lavaBuckets > 1 ? "s" : "")
                                + (lavaMb > 0 ? " + " + lavaMb + " mb" : ""))
                        .withStyle(ChatFormatting.RED));
            } else {
                tooltips.add(Component.literal("• " + lavaCost + " mb")
                        .withStyle(ChatFormatting.RED));
            }
            tooltips.add(Component.literal("• Craft Time: " + craftSeconds + " seconds")
                    .withStyle(ChatFormatting.GREEN));
            tooltips.add(Component.empty());
            tooltips.add(Component.literal("Drawn from adjacent Lava Tanks")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        if (mouseX >= OUTPUT_TANK_X && mouseX <= OUTPUT_TANK_X + OUTPUT_TANK_WIDTH &&
                mouseY >= OUTPUT_TANK_Y && mouseY <= OUTPUT_TANK_Y + OUTPUT_TANK_HEIGHT) {
            AlloyMaterial material = AlloyMaterial.fromName(recipe.getResultMaterial());
            String matName = material != null ? material.getName() : recipe.getResultMaterial();
            tooltips.add(Component.literal("Molten " + matName + " Output")
                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
            tooltips.add(Component.empty());
            tooltips.add(Component.literal("• " + recipe.getResultAmountMb() + " mb")
                    .withStyle(ChatFormatting.YELLOW));
            tooltips.add(Component.empty());
            tooltips.add(Component.literal("Stored in internal output tank")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        return tooltips;
    }
}