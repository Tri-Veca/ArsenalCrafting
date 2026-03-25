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
import net.neoforged.neoforge.fluids.FluidStack;

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
                    + AlloyRecipeDefinitions.RECIPES.size() + " solid recipes and "
                    + AlloyRecipeDefinitions.LIQUID_MIXING_RECIPES.size() + " liquid mixing recipes...");

            // Generate solid recipes
            for (AlloyRecipeDefinitions.AlloyRecipeDefinition recipe : AlloyRecipeDefinitions.RECIPES.values()) {
                futures.add(generateRecipe(cache, recipe));
            }

            // Generate liquid mixing recipes
            for (AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe : AlloyRecipeDefinitions.LIQUID_MIXING_RECIPES.values()) {
                futures.add(generateLiquidMixingRecipe(cache, recipe));
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

    private CompletableFuture<?> generateLiquidMixingRecipe(CachedOutput cache,
                                                            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe) {
        JsonObject json = new JsonObject();
        json.addProperty("type", "arsenalcrafting:liquid_mixing");

        // Input fluids
        JsonArray inputs = new JsonArray();
        for (FluidStack input : recipe.inputs()) {
            JsonObject inputObj = new JsonObject();
            inputObj.addProperty("fluid", input.getFluid().builtInRegistryHolder().key().location().toString());
            inputObj.addProperty("amount", input.getAmount());
            inputs.add(inputObj);
        }
        json.add("inputs", inputs);

        // Output fluid
        JsonObject output = new JsonObject();
        output.addProperty("fluid", recipe.output().getFluid().builtInRegistryHolder().key().location().toString());
        output.addProperty("amount", recipe.output().getAmount());
        json.add("output", output);

        json.addProperty("craftTime", recipe.craftTime());

        Path path = packOutput.getOutputFolder(PackOutput.Target.DATA_PACK)
                .resolve(ArsenalCrafting.MOD_ID)
                .resolve("recipe")
                .resolve("liquid_mixing")
                .resolve(recipe.name() + ".json");

        System.out.println("[AlloyRecipeProvider] Generating liquid recipe: " + recipe.name()
                + " → " + recipe.output().getAmount() + "mb " + recipe.output().getDisplayName().getString());

        return DataProvider.saveStable(cache, json, path);
    }

    @Override
    public String getName() { return "Alloy Smelting Recipes"; }
}