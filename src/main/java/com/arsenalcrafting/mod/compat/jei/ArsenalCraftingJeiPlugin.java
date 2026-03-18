package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.core.alloy.ToolType;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IRecipeTransferRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
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
        // Register Alloy Smelter category
        registration.addRecipeCategories(new AlloySmelterRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));

        // Register Toolkit category
        registration.addRecipeCategories(new ToolkitRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));

        // Register Augment Info category
        registration.addRecipeCategories(new AugmentInfoCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // --- ALLOY SMELTER RECIPES ---
        List<AlloyRecipe> alloyRecipes = new ArrayList<>();

        if (Minecraft.getInstance().level != null) {
            alloyRecipes = Minecraft.getInstance().level.getRecipeManager()
                    .getAllRecipesFor(AlloyRecipeType.ALLOY_SMELTING.get())
                    .stream()
                    .map(RecipeHolder::value)
                    .collect(Collectors.toList());
        }

        System.out.println("[JEI] Registering " + alloyRecipes.size() + " alloy smelter recipes");
        registration.addRecipes(AlloySmelterRecipeCategory.RECIPE_TYPE, alloyRecipes);

        // --- TOOLKIT RECIPES (Templates - one per tool type) ---
        List<ToolkitRecipeCategory.ToolkitRecipe> toolkitRecipes = ToolkitRecipeCategory.ToolkitRecipe.createTemplates();
        System.out.println("[JEI] Registering " + toolkitRecipes.size() + " toolkit recipe templates");
        registration.addRecipes(ToolkitRecipeCategory.RECIPE_TYPE, toolkitRecipes);

        // --- AUGMENT INFO RECIPES ---
        List<AugmentManager.AugmentDefinition> augmentRecipes = new ArrayList<>(AugmentManager.getAll());
        System.out.println("[JEI] Registering " + augmentRecipes.size() + " augment info recipes");
        registration.addRecipes(AugmentInfoCategory.RECIPE_TYPE, augmentRecipes);
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        // Transfer handler for Toolkit
        registration.addRecipeTransferHandler(new ToolkitRecipeTransferInfo());

        // You can add transfer handler for Alloy Smelter here if needed
        // registration.addRecipeTransferHandler(new AlloySmelterRecipeTransferInfo());
    }
}