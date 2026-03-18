package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AlloyRecipeProvider implements DataProvider {
    private final PackOutput packOutput;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final ExistingFileHelper existingFileHelper;

    public AlloyRecipeProvider(PackOutput packOutput, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper) {
        this.packOutput = packOutput;
        this.registries = registries;
        this.existingFileHelper = existingFileHelper;

        // Initialize recipe definitions
        AlloyRecipeDefinitions.init();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(provider -> {
            List<CompletableFuture<?>> futures = new ArrayList<>();

            System.out.println("[AlloyRecipeProvider] Generating " + AlloyRecipeDefinitions.RECIPES.size() + " alloy recipes...");

            // Generate each recipe
            for (AlloyRecipeDefinitions.AlloyRecipeDefinition recipe : AlloyRecipeDefinitions.RECIPES.values()) {
                futures.add(generateRecipe(cache, recipe));
            }

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        });
    }

    private CompletableFuture<?> generateRecipe(CachedOutput cache, AlloyRecipeDefinitions.AlloyRecipeDefinition recipe) {
        JsonObject json = new JsonObject();

        // Recipe type
        json.addProperty("type", "arsenalcrafting:alloy_smelting");

        // Ingredients array
        JsonArray ingredients = new JsonArray();

        // Add each ingredient
        for (Object ing : recipe.ingredients()) {
            JsonObject ingredientObj = new JsonObject();

            if (ing instanceof Item item) {
                // Resolve item ID
                ResourceLocation itemId = item.builtInRegistryHolder().key().location();
                ingredientObj.addProperty("item", itemId.toString());
            } else if (ing instanceof String modItemName) {
                // Mod item
                ingredientObj.addProperty("item", "arsenalcrafting:" + modItemName);
            } else {
                throw new IllegalArgumentException("Unknown ingredient type: " + ing);
            }

            ingredients.add(ingredientObj);
        }

        json.add("ingredients", ingredients);

        // Result object
        JsonObject result = new JsonObject();
        result.addProperty("id", recipe.getResultItemId());
        result.addProperty("count", recipe.resultCount());
        json.add("result", result);

        // Recipe parameters
        json.addProperty("lavaCost", recipe.getLavaCost());
        json.addProperty("craftTime", recipe.getCraftTime());

        // Generate file path
        Path path = packOutput.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(ArsenalCrafting.MOD_ID)
                .resolve("recipe")
                .resolve("alloy_smelting")
                .resolve(recipe.name() + ".json");

        System.out.println("[AlloyRecipeProvider] Generating: " + recipe.name() +
                " (craftTime: " + recipe.getCraftTime() + ", lavaCost: " + recipe.getLavaCost() + ")");

        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public String getName() {
        return "Alloy Smelting Recipes";
    }
}