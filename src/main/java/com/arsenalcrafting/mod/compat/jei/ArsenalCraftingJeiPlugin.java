package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.gui.AlloySmelterScreen;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@JeiPlugin
public class ArsenalCraftingJeiPlugin implements IModPlugin {

    private static final ResourceLocation ID =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() { return ID; }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new AlloySmelterRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new ToolkitRecipeCategory(
                registration.getJeiHelpers().getGuiHelper()));
        registration.addRecipeCategories(new AugmentInfoCategory(
                registration.getJeiHelpers().getGuiHelper()));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
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

        List<ToolkitRecipeCategory.ToolkitRecipe> toolkitRecipes =
                ToolkitRecipeCategory.ToolkitRecipe.createTemplates();
        System.out.println("[JEI] Registering " + toolkitRecipes.size() + " toolkit recipe templates");
        registration.addRecipes(ToolkitRecipeCategory.RECIPE_TYPE, toolkitRecipes);

        List<AugmentManager.AugmentDefinition> augmentRecipes =
                new ArrayList<>(AugmentManager.getAll());
        System.out.println("[JEI] Registering " + augmentRecipes.size() + " augment info recipes");
        registration.addRecipes(AugmentInfoCategory.RECIPE_TYPE, augmentRecipes);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        registration.addGuiContainerHandler(AlloySmelterScreen.class, new AlloySmelterGuiHandler());
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        // Alloy smelter block
        registration.addRecipeCatalyst(
                new ItemStack(ModBlocks.ALLOY_SMELTER.get()),
                AlloySmelterRecipeCategory.RECIPE_TYPE);

        // All alloy items (ingots and buckets) - R on any shows the recipe
        for (AlloyMaterial material : AlloyMaterial.values()) {
            var ingotHolder = ModItems.ALLOY_ITEMS.get(material);
            if (ingotHolder != null) {
                registration.addRecipeCatalyst(
                        new ItemStack(ingotHolder.get()),
                        AlloySmelterRecipeCategory.RECIPE_TYPE);
            }

            var bucketHolder = ModItems.ALLOY_BUCKETS.get(material);
            if (bucketHolder != null) {
                registration.addRecipeCatalyst(
                        new ItemStack(bucketHolder.get()),
                        AlloySmelterRecipeCategory.RECIPE_TYPE);
            }
        }
    }

    @Override
    public void registerRecipeTransferHandlers(IRecipeTransferRegistration registration) {
        registration.addRecipeTransferHandler(new ToolkitRecipeTransferInfo());
    }
}