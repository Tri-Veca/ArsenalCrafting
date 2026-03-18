package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.registry.ModBlocks;
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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public class AlloySmelterRecipeCategory implements IRecipeCategory<AlloyRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "alloy_smelting");
    public static final RecipeType<AlloyRecipe> RECIPE_TYPE = new RecipeType<>(UID, AlloyRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;

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

        // Clear any existing slots (just in case)
        builder.setShapeless();

        // Add input slots in a 3x3 grid
        // The alloy smelter has 9 input slots (0-8)
        for (int i = 0; i < 9; i++) {
            int row = i / 3;
            int col = i % 3;

            // Calculate position based on your GUI layout
            int xPos = 10 + col * 18;
            int yPos = 19 + row * 18;

            // If we have an ingredient for this slot, add it
            if (i < ingredients.size()) {
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, yPos)
                        .addIngredients(ingredients.get(i));
            } else {
                // Add an empty slot for visual consistency
                builder.addSlot(RecipeIngredientRole.INPUT, xPos, yPos)
                        .addItemStack(ItemStack.EMPTY);
            }
        }

        // Add fuel slot (slot 9)
        builder.addSlot(RecipeIngredientRole.INPUT, 89, 53)
                .addIngredients(Ingredient.of(Items.LAVA_BUCKET))
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.literal("Fuel Slot").withStyle(ChatFormatting.YELLOW));
                    tooltip.add(Component.literal("Place lava buckets here").withStyle(ChatFormatting.GRAY));
                });

        // Add output slot (slot 10)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 143, 35)
                .addItemStack(recipe.getResultItem(null))
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.literal("Output").withStyle(ChatFormatting.GREEN));

                    // Add recipe info to output tooltip
                    int lavaCost = recipe.getLavaCost();
                    int craftSeconds = recipe.getCraftTime() / 20;
                   // int totalFuelMb = (int) Math.ceil((double) recipe.getCraftTime() / recipe.getFuelTime()) * 100;

                    tooltip.add(Component.empty());
                    tooltip.add(Component.literal("Recipe Info:").withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.literal("  Craft Time: " + craftSeconds + " sec").withStyle(ChatFormatting.GREEN));
                    tooltip.add(Component.literal("  Lava Needed: " + lavaCost + " mb").withStyle(ChatFormatting.RED));
                });
    }

    @Override
    public List<Component> getTooltipStrings(AlloyRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        List<Component> tooltips = new ArrayList<>();

        // Add custom tooltips when hovering over specific areas
        if (mouseX >= 89 && mouseX <= 103 && mouseY >= 53 && mouseY <= 69) {
            tooltips.add(Component.translatable("tooltip.arsenalcrafting.fuel_slot")
                    .withStyle(ChatFormatting.GRAY));
        }

        return tooltips;
    }
}