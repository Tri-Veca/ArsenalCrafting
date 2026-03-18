package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class ArsenalCraftingJeiPlugin implements IModPlugin {

    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlloySmelterRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        RecipeType<AlloyRecipe> recipeType = AlloySmelterRecipeCategory.RECIPE_TYPE;

        List<AlloyRecipe> recipes = Minecraft.getInstance().level.getRecipeManager()
                .getAllRecipesFor(AlloyRecipeType.ALLOY_SMELTING.get())
                .stream()
                .map(RecipeHolder::value)
                .collect(Collectors.toList());

        registration.addRecipes(recipeType, recipes);
    }
}