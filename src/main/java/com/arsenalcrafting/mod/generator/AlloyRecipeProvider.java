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
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class AlloyRecipeProvider implements DataProvider {
    private final PackOutput packOutput;
    private final CompletableFuture<HolderLookup.Provider> registries;
    private final ExistingFileHelper existingFileHelper;

    public AlloyRecipeProvider(PackOutput packOutput,
                               CompletableFuture<HolderLookup.Provider> registries,
                               ExistingFileHelper existingFileHelper) {
        this.packOutput         = packOutput;
        this.registries         = registries;
        this.existingFileHelper = existingFileHelper;
        AlloyRecipeDefinitions.init();
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(provider -> {
            List<CompletableFuture<?>> futures = new ArrayList<>();
            System.out.println("[AlloyRecipeProvider] Generating "
                    + AlloyRecipeDefinitions.RECIPES.size() + " alloy recipes...");
            for (AlloyRecipeDefinitions.AlloyRecipeDefinition recipe : AlloyRecipeDefinitions.RECIPES.values()) {
                futures.add(generateRecipe(cache, recipe));
            }
            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        });
    }

    private CompletableFuture<?> generateRecipe(CachedOutput cache,
                                                AlloyRecipeDefinitions.AlloyRecipeDefinition recipe) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "arsenalcrafting:alloy_smelting");

        // Ingredients
        JsonArray ingredients = new JsonArray();
        for (Object ing : recipe.ingredients()) {
            JsonObject ingredientObj = new JsonObject();
            if (ing instanceof Item item) {
                ingredientObj.addProperty("item",
                        item.builtInRegistryHolder().key().location().toString());
            } else if (ing instanceof String modItemName) {
                ingredientObj.addProperty("item", "arsenalcrafting:" + modItemName);
            } else {
                throw new IllegalArgumentException("Unknown ingredient type: " + ing);
            }
            ingredients.add(ingredientObj);
        }
        json.add("ingredients", ingredients);

        // ── FLUID RESULT (new) ────────────────────────────────────────────────
        json.addProperty("resultMaterial", recipe.getResultMaterialName());
        json.addProperty("resultAmountMb", recipe.resultAmountMb());

        // ── OLD ITEM RESULT (commented out — uncomment to revert) ────────────
        // JsonObject result = new JsonObject();
        // result.addProperty("id", recipe.getResultItemId());
        // result.addProperty("count", recipe.resultCount());
        // json.add("result", result);

        json.addProperty("lavaCost",   recipe.getLavaCost());
        json.addProperty("craftTime",  recipe.getCraftTime());

        Path path = packOutput.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(ArsenalCrafting.MOD_ID)
                .resolve("recipe")
                .resolve("alloy_smelting")
                .resolve(recipe.name() + ".json");

        System.out.println("[AlloyRecipeProvider] Generating: " + recipe.name()
                + " → " + recipe.resultAmountMb() + "mb " + recipe.getResultMaterialName()
                + " (craftTime: " + recipe.getCraftTime() + ", lavaCost: " + recipe.getLavaCost() + ")");

        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public String getName() { return "Alloy Smelting Recipes"; }
}