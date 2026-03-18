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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AlloyRecipeDefinitions {

    // Map to store all recipe definitions
    public static final Map<String, AlloyRecipeDefinition> RECIPES = new LinkedHashMap<>();

    // Initialize all recipes
    public static void init() {
        // Clear existing recipes
        RECIPES.clear();

        // BRONZE: 3 Copper + 1 Tin = 4 Bronze
        addRecipe("bronze_from_alloy_smelting", AlloyMaterial.BRONZE, 4,
                Items.COPPER_INGOT, Items.COPPER_INGOT, Items.COPPER_INGOT,
                ItemStackHelper.getAlloyStack(AlloyMaterial.TIN).getItem()
        );


        // STEEL: 3 Iron + 3 Coal = 2 Steel
        addRecipe("steel_from_alloy_smelting", AlloyMaterial.STEEL, 2,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.COAL, Items.COAL, Items.COAL
        );

        // TITANIUM: 2 Steel + 1 Diamond + 1 Obsidian = 1 Titanium
        addRecipe("titanium_from_alloy_smelting", AlloyMaterial.TITANIUM, 1,
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(), ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(),
                Items.DIAMOND, Items.OBSIDIAN
        );

        // Example: HARDENED_STEEL (if you add it later)
        // addRecipe("hardened_steel_from_alloy_smelting", AlloyMaterial.HARDENED_STEEL, 1,
        //     getModItem("steel_alloy"), getModItem("steel_alloy"),
        //     Items.NETHERITE_INGOT, Items.BLAZE_ROD
        // );

        System.out.println("[AlloyRecipeDefinitions] Loaded " + RECIPES.size() + " recipe definitions");
    }

    // Helper to add a recipe with variable ingredients
    private static void addRecipe(String name, AlloyMaterial resultMaterial, int resultCount, Object... ingredients) {
        List<Object> ingredientList = new ArrayList<>();
        for (Object obj : ingredients) {
            ingredientList.add(obj);
        }

        RECIPES.put(name, new AlloyRecipeDefinition(
                name,
                resultMaterial,
                resultCount,
                ingredientList
        ));
    }

    // Helper to get mod item by name
    private static Item getModItem(String name) {
        // This will be resolved at runtime by the recipe provider
        // We'll use a special marker that gets replaced later
        return null; // Handled specially in the provider
    }

    // Record to hold recipe definition
    public record AlloyRecipeDefinition(
            String name,
            AlloyMaterial resultMaterial,
            int resultCount,
            List<Object> ingredients
    ) {

        public String getResultItemId() {
            return "arsenalcrafting:" + resultMaterial.name().toLowerCase() + "_alloy";
        }

        public int getCraftTime() {
            return resultMaterial.meltTime;
        }

        public int getLavaCost() {
            return 500; // Fixed lava cost as requested
        }

        // Convert ingredient objects to JSON strings
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
    }
}