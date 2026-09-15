package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.concurrent.CompletableFuture;

// Standard crafting-table recipes (shaped/shapeless), separate from
// AlloyRecipeProvider which handles the custom alloy-smelting recipe type.
public class ArsenalRecipeProvider extends RecipeProvider {

    public ArsenalRecipeProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        buildBasicCasingRecipe(output);
        buildUpgradedCasingRecipes(output);
        buildLavaTankRecipe(output);
        buildAlloySmelterRecipe(output);
        buildValveFaucetRecipe(output);
        buildBlueprintRecipe(output);
        buildBlueprintCastingTable(output);
        buildBasicAugmentRecipe(output);
        buildFortuneAugmentT1(output);
        buildFortuneAugmentT2(output);
        buildFortuneAugmentT3(output);
    }

    // Core loop for crafting multiblock Smeltery Blocks
    private void buildBasicCasingRecipe(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BASIC_CASING.get())
                .pattern("MM")
                .pattern("MM")
                .define('M', Ingredient.of(ModItems.BASIC_MATERIAL.get()))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_MATERIAL.get()))
                .save(output);
    }

    private void buildValveFaucetRecipe(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.VALVE_FAUCET.get())
                .pattern("   ")
                .pattern("M M")
                .pattern(" M ")
                .define('M', Ingredient.of(ModItems.BASIC_MATERIAL.get()))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_MATERIAL.get()))
                .save(output);
    }

    private void buildLavaTankRecipe(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.LAVA_TANK.get())
                .pattern("MGM")
                .pattern("MGM")
                .pattern("MCM")
                .define('M', Ingredient.of(ModItems.BASIC_MATERIAL.get()))
                .define('G', Ingredient.of(net.minecraft.world.item.Items.GLASS))
                .define('C', Ingredient.of(ModBlocks.BASIC_CASING.get()))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_MATERIAL.get()))
                .save(output);
    }

    private void buildAlloySmelterRecipe(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.ALLOY_SMELTER.get())
                .pattern("MRM")
                .pattern("MFM")
                .pattern("MCM")
                .define('M', Ingredient.of(ModItems.BASIC_MATERIAL.get()))
                .define('F', Ingredient.of(Items.FURNACE))
                .define('R', Ingredient.of(Items.REDSTONE))
                .define('C', Ingredient.of(ModBlocks.BASIC_CASING.get()))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_MATERIAL.get()))
                .save(output);
    }

    // Blueprint Casting Table

    private void buildBlueprintRecipe(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BLUEPRINT.get())
                .pattern("PL")
                .pattern("I ")
                .define('P', Ingredient.of(Items.PAPER))
                .define('L', Ingredient.of(Items.LAPIS_LAZULI))
                .define('I', Ingredient.of(Items.IRON_NUGGET))
                .unlockedBy("has_basic_material", has(Items.PAPER))
                .save(output);
    }

    private void buildBlueprintCastingTable(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModBlocks.BLUEPRINT_CASTING_TABLE.get())
                .pattern("MBM")
                .pattern("MTM")
                .pattern("MCM")
                .define('M', Ingredient.of(ModItems.BASIC_MATERIAL.get()))
                .define('T', Ingredient.of(Items.CRAFTING_TABLE))
                .define('B', Ingredient.of(ModItems.BLUEPRINT.get()))
                .define('C', Ingredient.of(ModBlocks.BASIC_CASING.get()))
                .unlockedBy("has_basic_material", has(ModItems.BLUEPRINT.get()))
                .save(output);
    }

    // Toolkit and Augments

    private void buildBasicAugmentRecipe(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.BASIC_AUGMENT.get())
                .pattern(" M ")
                .pattern("M M")
                .pattern(" M ")
                .define('M', Ingredient.of(ModItems.BASIC_MATERIAL.get()))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_MATERIAL.get()))
                .save(output);
    }

    private void buildFortuneAugmentT1(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemStackHelper.getUniversalAugmentStack("fortune", 1))
                .pattern(" R ")
                .pattern("LAL")
                .pattern(" L ")
                .define('L', Ingredient.of(Items.LAPIS_BLOCK))
                .define('A', Ingredient.of(ModItems.BASIC_AUGMENT.get()))
                .define('R', Ingredient.of(Items.REDSTONE_BLOCK))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_AUGMENT.get()))
                .save(output);
    }
    private void buildFortuneAugmentT2(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemStackHelper.getUniversalAugmentStack("fortune", 2))
                .pattern("SGS")
                .pattern("LAL")
                .pattern("SLS")
                .define('L', Ingredient.of(Items.LAPIS_BLOCK))
                .define('A', Ingredient.of(ItemStackHelper.getUniversalAugmentStack("fortune", 1)))
                .define('S', Ingredient.of(ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL)))
                .define('G', Ingredient.of(Items.GOLD_BLOCK))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_AUGMENT.get()))
                .save(output);
    }

    private void buildFortuneAugmentT3(RecipeOutput output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ItemStackHelper.getUniversalAugmentStack("fortune", 3))
                .pattern("TDT")
                .pattern("LAL")
                .pattern("TLT")
                .define('L', Ingredient.of(Items.LAPIS_BLOCK))
                .define('A', Ingredient.of(ItemStackHelper.getUniversalAugmentStack("fortune", 2)))
                .define('T', Ingredient.of(ItemStackHelper.getAlloyStack(AlloyMaterial.TITANIUM)))
                .define('D', Ingredient.of(Items.DIAMOND_BLOCK))
                .unlockedBy("has_basic_material", has(ModItems.BASIC_AUGMENT.get()))
                .save(output);
    }





    // empty), yields 2 upgraded casings. One recipe per tier in SmelteryTier
    // (Copper, Iron, Bronze, Steel, Titanium — Tin has no casing tier).
    private void buildUpgradedCasingRecipes(RecipeOutput output) {
        casingRecipe(output, AlloyMaterial.COPPER, Ingredient.of(Items.COPPER_INGOT), ModBlocks.COPPER_CASING.get());
        casingRecipe(output, AlloyMaterial.IRON, Ingredient.of(Items.IRON_INGOT), ModBlocks.IRON_CASING.get());
        casingRecipe(output, AlloyMaterial.BRONZE, Ingredient.of(ModItems.ALLOY_ITEMS.get(AlloyMaterial.BRONZE).get()), ModBlocks.BRONZE_CASING.get());
        casingRecipe(output, AlloyMaterial.STEEL, Ingredient.of(ModItems.ALLOY_ITEMS.get(AlloyMaterial.STEEL).get()), ModBlocks.STEEL_CASING.get());
        casingRecipe(output, AlloyMaterial.TITANIUM, Ingredient.of(ModItems.ALLOY_ITEMS.get(AlloyMaterial.TITANIUM).get()), ModBlocks.TITANIUM_CASING.get());
    }

    private void casingRecipe(RecipeOutput output, AlloyMaterial material, Ingredient tierIngot, net.minecraft.world.level.block.Block resultCasing) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, resultCasing, 2)
                .pattern("TT")
                .pattern("CT")
                .define('T', tierIngot)
                .define('C', ModBlocks.BASIC_CASING.get())
                .unlockedBy("has_basic_casing", has(ModBlocks.BASIC_CASING.get()))
                .save(output);
    }
}