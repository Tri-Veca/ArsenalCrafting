package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.*;

public class AlloyRecipeDefinitions {

    public static final Map<String, AlloyRecipeDefinition> RECIPES = new LinkedHashMap<>();
    public static final Map<String, AlloyRecipeDefinition> RECIPES2 = new LinkedHashMap<>();

    public static void init() {
        RECIPES.clear();

        // BRONZE: 2 Copper + 1 Tin → 1000 mb Bronze
        addRecipe("bronze_from_alloy_smelting", AlloyMaterial.BRONZE,  2000, 500,
                Items.COPPER_INGOT, Items.COPPER_INGOT,
                ItemStackHelper.getAlloyStack(AlloyMaterial.TIN).getItem()
        );
        addRecipe("hydrocarbon_from_alloy_smelting", AlloyMaterial.COAL, 1000, 500,
                Items.COAL

        );


        // STEEL: 3 Iron + 3 Coal → 1000 mb Steel
        addRecipe("steel_from_alloy_smelting", AlloyMaterial.STEEL, 1000, 750,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.COAL, Items.COAL, Items.COAL
        );

        // TITANIUM: 2 Steel + 1 Diamond + 1 Obsidian → 1000 mb Titanium
        addRecipe("titanium_from_alloy_smelting", AlloyMaterial.TITANIUM, 2000,750,
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(),
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(),
                Items.DIAMOND, Items.OBSIDIAN
        );

        System.out.println("[AlloyRecipeDefinitions] Loaded " + RECIPES.size() + " recipe definitions");
    }



    /**
     * @param resultAmountMb Output fluid in millibuckets
     */
    private static void addRecipe(String name, AlloyMaterial resultMaterial,
                                  int resultAmountMb, int lavaCost, Object... ingredients) {
        List<Object> ingredientList = new ArrayList<>(List.of(ingredients));
        int calculatedCraftTime = calculateCraftTime(ingredientList);

        RECIPES.put(name, new AlloyRecipeDefinition(
                name,
                resultMaterial,
                resultAmountMb,
                lavaCost,
                ingredientList,
                calculatedCraftTime
        ));



        System.out.println("[AlloyRecipeDefinitions] Recipe '" + name + "' → " + resultAmountMb
                + "mb " + resultMaterial.getName() + " | craftTime: " + calculatedCraftTime + " ticks");
    }



    private static int calculateCraftTime(List<Object> ingredients) {
        Set<Item> uniqueIngredients = new HashSet<>();
        int totalTicks = 0;

        for (Object ingredient : ingredients) {
            if (ingredient instanceof Item item) {
                if (uniqueIngredients.contains(item)) continue;
                uniqueIngredients.add(item);

                int meltTime = 20;
                AlloyMaterial material = AlloyMaterial.fromItem(item);
                if (material != null) meltTime = material.meltTime;

                totalTicks += meltTime;
                System.out.println("  - Ingredient: " + item.getDescription().getString()
                        + " (meltTime: " + meltTime + ")");
            }
        }

        if (uniqueIngredients.isEmpty()) return 20;
        int average = totalTicks / uniqueIngredients.size();
        System.out.println("  - Average: " + totalTicks + " / " + uniqueIngredients.size()
                + " = " + average + " ticks");
        return average;
    }

    public record AlloyRecipeDefinition(
            String name,
            AlloyMaterial resultMaterial,
            int resultAmountMb,
            int lavaCost, List<Object> ingredients,
            int craftTime

    ) {
        public String getResultMaterialName() {
            return resultMaterial.getName();
        }

        public int getCraftTime()  { return craftTime; }
        public int getLavaCost()   { return lavaCost; }

        public String getIngredientsJson() {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < ingredients.size(); i++) {
                Object ing = ingredients.get(i);
                if (i > 0) json.append(",");
                if (ing instanceof Item item) {
                    String itemId = item.builtInRegistryHolder().key().location().toString();
                    json.append(String.format("{\"item\": \"%s\"}", itemId));
                } else if (ing instanceof String modItemId) {
                    json.append(String.format("{\"item\": \"arsenalcrafting:%s\"}", modItemId));
                } else {
                    throw new IllegalArgumentException("Unknown ingredient type: " + ing);
                }
            }
            json.append("]");
            return json.toString();
        }

        @Override
        public String toString() {
            return String.format("AlloyRecipeDefinition{name='%s', result=%s, amountMb=%d, craftTime=%d}",
                    name, resultMaterial.name(), resultAmountMb, lavaCost,craftTime);
        }
    }
}