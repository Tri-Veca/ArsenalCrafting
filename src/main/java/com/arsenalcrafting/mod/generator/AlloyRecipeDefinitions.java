package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModFluids;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.*;

public class AlloyRecipeDefinitions {

    public static final Map<String, AlloyRecipeDefinition> RECIPES = new LinkedHashMap<>();
    public static final Map<String, LiquidMixingRecipeDefinition> LIQUID_MIXING_RECIPES = new LinkedHashMap<>();

    public static void init() {
        RECIPES.clear();
        LIQUID_MIXING_RECIPES.clear();
        //System.out.println("[DEBUG] ===== AlloyRecipeDefinitions.init() START =====");
        //System.out.println("[DEBUG] LIQUID_MIXING_RECIPES cleared, size: " + LIQUID_MIXING_RECIPES.size());

        // BRONZE: 2 Copper + 1 Tin → 1000 mb Bronze
        /*addRecipe("bronze_from_alloy_smelting", AlloyMaterial.BRONZE, 2000, 500,
                Items.COPPER_INGOT, Items.COPPER_INGOT,
                ItemStackHelper.getAlloyStack(AlloyMaterial.TIN).getItem()

        );

         */
        addRecipe("hydrocarbon_from_alloy_smelting", AlloyMaterial.COAL, 1000, 200,
                Items.COAL

        );
        addRecipe("copper_from_alloy_smelting", AlloyMaterial.COPPER, 1000, 200,
                Items.COPPER_INGOT

        );
        addRecipe("tin_from_alloy_smelting", AlloyMaterial.TIN, 1000, 200,
                ItemStackHelper.getAlloyStack(AlloyMaterial.TIN).getItem()

        );

        addRecipe("bronze_from_alloy_smelting", AlloyMaterial.BRONZE, 1000, 200,
                ItemStackHelper.getAlloyStack(AlloyMaterial.BRONZE).getItem()

        );
        addRecipe("iron_from_alloy_smelting", AlloyMaterial.IRON, 1000, 200,
                Items.IRON_INGOT

        );
        addRecipe("diamond_from_alloy_smelting", AlloyMaterial.DIAMOND, 1000, 200,
                Items.DIAMOND

        );

        addRecipe("gold_from_alloy_smelting", AlloyMaterial.GOLD, 1000, 200,
                Items.GOLD_INGOT

        );

        addRecipe("netherite_from_alloy_smelting", AlloyMaterial.NETHERITE, 1000, 200,
                Items.NETHERITE_INGOT

        );
        addRecipe("rutile_from_alloy_smelting", AlloyMaterial.TITANIUM, 1000, 200,
                ModItems.RUTILE_SHARD.get()

        );

        addRecipe("steel_from_alloy_smelting", AlloyMaterial.STEEL, 1000, 200,
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem()
        );
        addRecipe("titanium_from_alloy_smelting", AlloyMaterial.TITANIUM, 1000, 200,
                ItemStackHelper.getAlloyStack(AlloyMaterial.TITANIUM).getItem()
        );
        //blocks
        addRecipe("gold_block_from_alloy_smelting", AlloyMaterial.GOLD, 9000, 800,
                Items.GOLD_BLOCK
        );
        addRecipe("iron_block_from_alloy_smelting", AlloyMaterial.IRON, 9000, 800,
                Items.IRON_BLOCK
        );

        addRecipe("copper_block_from_alloy_smelting", AlloyMaterial.COPPER, 9000, 800,
                Items.COPPER_BLOCK
        );
        addRecipe("diamond_block_from_alloy_smelting", AlloyMaterial.DIAMOND, 9000, 800,
                Items.DIAMOND_BLOCK
        );
        addRecipe("netherite_block_from_alloy_smelting", AlloyMaterial.NETHERITE, 9000, 800,
                Items.NETHERITE_BLOCK
        );


        addOreRecipe("copper_ore_smelting", AlloyMaterial.COPPER, 1000, 200,
                Items.RAW_COPPER);
        addOreRecipe("iron_ore_smelting", AlloyMaterial.IRON, 1000, 200,
                Items.RAW_IRON);
        addOreRecipe("gold_ore_smelting", AlloyMaterial.GOLD, 1000, 200,
                Items.RAW_GOLD);
        addOreRecipe("tin_ore_smelting", AlloyMaterial.TIN, 1000, 200,
                ItemStackHelper.getRawStack(AlloyMaterial.TIN).getItem());






        // STEEL: 3 Iron + 3 Coal → 1000 mb Steel
        /*addRecipe("steel_from_alloy_smelting", AlloyMaterial.STEEL, 1000, 750,
                Items.IRON_INGOT, Items.IRON_INGOT, Items.IRON_INGOT,
                Items.COAL, Items.COAL, Items.COAL
        );

         */

        // TITANIUM: 2 Steel + 1 Diamond + 1 Obsidian → 1000 mb Titanium
       /* addRecipe("titanium_from_alloy_smelting", AlloyMaterial.TITANIUM, 2000, 750,
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(),
                ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem(),
                Items.DIAMOND, Items.OBSIDIAN
        );

        */




        addLiquidMixingRecipe("bronze_from_liquids",
                ItemStackHelper.getFluidFromBucket(AlloyMaterial.BRONZE, 2000),
                10,
                ItemStackHelper.getFluidFromBucket(AlloyMaterial.COPPER, 2000),
                ItemStackHelper.getFluidFromBucket(AlloyMaterial.TIN, 1000)
        );

        addLiquidMixingRecipe("steel_from_liquids",
                ItemStackHelper.getFluidFromBucket(AlloyMaterial.STEEL, 1000),
                10,
                ItemStackHelper.getFluidFromBucket(AlloyMaterial.IRON, 3000),
                ItemStackHelper.getFluidFromBucket(AlloyMaterial.COAL, 3000)
        );


    }

    private static void addOreRecipe(String name, AlloyMaterial resultMaterial,
                                     int resultAmountMb, int lavaCost, Object... ingredients) {
        List<Object> ingredientList = new ArrayList<>(List.of(ingredients));
        int calculatedCraftTime = calculateCraftTime(ingredientList);
        RECIPES.put(name, new AlloyRecipeDefinition(
                name, resultMaterial, resultAmountMb, lavaCost,
                ingredientList, calculatedCraftTime, true // isOreRecipe = true
        ));
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
                calculatedCraftTime,
                false
        ));





        System.out.println("[AlloyRecipeDefinitions] Recipe '" + name + "' → " + resultAmountMb
                + "mb " + resultMaterial.getName() + " | craftTime: " + calculatedCraftTime + " ticks");
    }

    private static void addLiquidMixingRecipe(String name, FluidStack output,
                                              int craftTime, FluidStack... inputs) {
        LIQUID_MIXING_RECIPES.put(name, new LiquidMixingRecipeDefinition(
                name,
                output,
                Arrays.asList(inputs),
                craftTime
        ));

        StringBuilder inputStr = new StringBuilder();
        for (FluidStack input : inputs) {
            if (inputStr.length() > 0) inputStr.append(" + ");
            inputStr.append(input.getAmount()).append("mb ").append(input.getDisplayName().getString());
        }

        System.out.println("[AlloyRecipeDefinitions] Liquid recipe '" + name + "' → " +
                output.getAmount() + "mb " + output.getDisplayName().getString() +
                " | Inputs: " + inputStr.toString() +
                " | craftTime: " + craftTime + " ticks");
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


    public record LiquidMixingRecipeDefinition(
            String name,
            FluidStack output,
            List<FluidStack> inputs,
            int craftTime
    ) {
        // In AlloyRecipeDefinitions.java - update canCraft with debug
        public boolean canCraft(List<FluidStack> availableFluids) {
            //System.out.println("[DEBUG] canCraft check for recipe: " + name);
           // System.out.println("[DEBUG] Required inputs:");
            for (FluidStack req : inputs) {
               /* System.out.println("  - Required: " + req.getAmount() + "mb " +
                        req.getFluid().getFluidType().getDescription().getString());

                */
            }

            // Merge available fluids
            Map<Fluid, Integer> availableMap = new HashMap<>();
            for (FluidStack fluid : availableFluids) {
                if (fluid != null && !fluid.isEmpty()) {
                    availableMap.merge(fluid.getFluid(), fluid.getAmount(), Integer::sum);
                    /*System.out.println("[DEBUG] Available: " + fluid.getAmount() + "mb " +
                            fluid.getFluid().getFluidType().getDescription().getString());

                     */
                }
            }

            // Merge required fluids
            Map<Fluid, Integer> requiredMap = new HashMap<>();
            for (FluidStack required : inputs) {
                if (required != null && !required.isEmpty()) {
                    requiredMap.merge(required.getFluid(), required.getAmount(), Integer::sum);
                }
            }

            // Check requirements
            for (Map.Entry<Fluid, Integer> entry : requiredMap.entrySet()) {
                int available = availableMap.getOrDefault(entry.getKey(), 0);
               /* System.out.println("[DEBUG] Checking " + entry.getKey().getFluidType().getDescription().getString()
                        + ": need " + entry.getValue() + ", have " + available);

                */
                if (available < entry.getValue()) {
                    /*System.out.println("[DEBUG] FAILED: Not enough " +
                            entry.getKey().getFluidType().getDescription().getString());

                     */
                    return false;
                }
            }

            //System.out.println("[DEBUG] PASSED: All requirements met!");
            return true;
        }

        public List<FluidStack> getConsumedFluids() {
            // Merge same fluids together
            Map<Fluid, Integer> merged = new HashMap<>();
            for (FluidStack input : inputs) {
                if (input != null && !input.isEmpty()) {
                    merged.merge(input.getFluid(), input.getAmount(), Integer::sum);
                }
            }

            List<FluidStack> result = new ArrayList<>();
            for (Map.Entry<Fluid, Integer> entry : merged.entrySet()) {
                result.add(new FluidStack(entry.getKey(), entry.getValue()));
            }
            return result;
        }

        @Override
        public String toString() {
            return String.format("LiquidMixingRecipeDefinition{name='%s', output=%s, inputs=%d, craftTime=%d}",
                    name, output.getDisplayName().getString(), inputs.size(), craftTime);
        }
    }

    public record AlloyRecipeDefinition(
            String name,
            AlloyMaterial resultMaterial,
            int resultAmountMb,
            int lavaCost, List<Object> ingredients,
            int craftTime,
            boolean isOreRecipe

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

        public static List<LiquidMixingRecipeDefinition> getAllLiquidMixingRecipes() {
            return new ArrayList<>(LIQUID_MIXING_RECIPES.values());
        }




        @Override
        public String toString() {
            return String.format("AlloyRecipeDefinition{name='%s', result=%s, amountMb=%d, craftTime=%d}",
                    name, resultMaterial.name(), resultAmountMb, lavaCost,craftTime);
        }
    }
}