package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

public class AlloyRecipeDefinitions {

    // Map to store all recipe definitions
    public static final Map<String, AlloyRecipeDefinition> RECIPES = new LinkedHashMap<>();

    // Initialize all recipes
    public static void init() {
        // Clear existing recipes
        RECIPES.clear();

        // BRONZE: 2 Copper + 1 Tin = 2 Bronze
        addRecipe("bronze_from_alloy_smelting", AlloyMaterial.BRONZE, 2,
                Items.COPPER_INGOT, Items.COPPER_INGOT,
                ItemStackHelper.getAlloyStack(AlloyMaterial.TIN).getItem()
        );


        // STEEL: 3 Iron + 3 Coal = 2 Steel
        addRecipe("steel_from_alloy_smelting", AlloyMaterial.STEEL, 2,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.COAL, Items.COAL, Items.COAL
        );

        // TITANIUM: 2 Steel + 1 Diamond + 1 Obsidian = 1 Titanium
        addRecipe("titanium_from_alloy_smelting", AlloyMaterial.TITANIUM, 1,
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(),
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(),
                Items.DIAMOND, Items.OBSIDIAN
        );

        System.out.println("[AlloyRecipeDefinitions] Loaded " + RECIPES.size() + " recipe definitions");
    }

    /**
     * Adds a recipe with variable ingredients and automatically calculates craft time
     * @param name The recipe name
     * @param resultMaterial The resulting alloy material
     * @param resultCount How many items are produced
     * @param ingredients Variable array of ingredient items
     */
    private static void addRecipe(String name, AlloyMaterial resultMaterial, int resultCount, Object... ingredients) {
        List<Object> ingredientList = new ArrayList<>();
        for (Object obj : ingredients) {
            ingredientList.add(obj);
        }

        // Calculate craft time based on unique ingredient melt times
        int calculatedCraftTime = calculateCraftTime(ingredientList);

        RECIPES.put(name, new AlloyRecipeDefinition(
                name,
                resultMaterial,
                resultCount,
                ingredientList,
                calculatedCraftTime
        ));

        System.out.println("[AlloyRecipeDefinitions] Recipe '" + name + "' craft time: " +
                calculatedCraftTime + " ticks (" + (calculatedCraftTime/20) + " seconds) - Averaged from unique ingredients");
    }

    /**
     * Calculates craft time by averaging melt times of unique ingredients
     * - If ingredient is an AlloyMaterial, use its meltTime
     * - If ingredient is any other item (vanilla/mod), default to 20 ticks
     * - Each ingredient type counted only once (by item)
     *
     * @param ingredients List of ingredient items
     * @return Average craft time in ticks
     */
    private static int calculateCraftTime(List<Object> ingredients) {
        // Use a Set to track unique items (by Item, not by quantity)
        Set<Item> uniqueIngredients = new HashSet<>();
        int totalTicks = 0;

        for (Object ingredient : ingredients) {
            if (ingredient instanceof Item item) {
                // Skip if we've already processed this item type
                if (uniqueIngredients.contains(item)) {
                    continue;
                }
                uniqueIngredients.add(item);

                int meltTime = 20; // Default for all items not in AlloyMaterial

                // Check if this item corresponds to an AlloyMaterial
                AlloyMaterial material = AlloyMaterial.fromItem(item);
                if (material != null) {
                    meltTime = material.meltTime; // Use the material's defined melt time
                }
                // If not an AlloyMaterial, keep default of 20 ticks

                totalTicks += meltTime;

                System.out.println("  - Ingredient: " + item.getDescription().getString() +
                        " (meltTime: " + meltTime + ")");
            }
        }

        // Fallback if no valid ingredients found
        if (uniqueIngredients.isEmpty()) {
            System.out.println("  - No valid ingredients found, using default 20 ticks");
            return 20;
        }

        // Return average of unique ingredient melt times
        int average = totalTicks / uniqueIngredients.size();
        System.out.println("  - Average of " + uniqueIngredients.size() + " unique ingredients: " +
                totalTicks + " / " + uniqueIngredients.size() + " = " + average + " ticks");

        return average;
    }

    /**
     * Record to hold recipe definition with all necessary data
     */
    public record AlloyRecipeDefinition(
            String name,
            AlloyMaterial resultMaterial,
            int resultCount,
            List<Object> ingredients,
            int craftTime  // Calculated craft time based on ingredient averages
    ) {

        /**
         * @return The resource location ID for the result item
         */
        public String getResultItemId() {
            return "arsenalcrafting:" + resultMaterial.name().toLowerCase() + "_alloy";
        }

        /**
         * @return The craft time in ticks (calculated from ingredients)
         */
        public int getCraftTime() {
            return craftTime;
        }

        /**
         * @return Fixed lava cost for all recipes (500mb)
         */
        public int getLavaCost() {
            return 500; // Fixed lava cost as requested
        }

        /**
         * Convert ingredient objects to JSON format for data generation
         * @return JSON string representation of ingredients
         */
        public String getIngredientsJson() {
            StringBuilder json = new StringBuilder();
            json.append("[");

            for (int i = 0; i < ingredients.size(); i++) {
                Object ing = ingredients.get(i);

                if (i > 0) {
                    json.append(",");
                }

                if (ing instanceof Item item) {
                    // Vanilla or mod item
                    String itemId = item.builtInRegistryHolder().key().location().toString();
                    json.append(String.format("""
                            {
                              "item": "%s"
                            }""", itemId));
                } else if (ing instanceof String modItemId) {
                    // Mod item reference (will be resolved later)
                    json.append(String.format("""
                            {
                              "item": "arsenalcrafting:%s"
                            }""", modItemId));
                } else {
                    throw new IllegalArgumentException("Unknown ingredient type: " + ing);
                }
            }

            json.append("]");
            return json.toString();
        }

        @Override
        public String toString() {
            return String.format("AlloyRecipeDefinition{name='%s', result=%s, count=%d, craftTime=%d ticks, ingredients=%s}",
                    name, resultMaterial.name(), resultCount, craftTime, ingredients.size());
        }
    }
}