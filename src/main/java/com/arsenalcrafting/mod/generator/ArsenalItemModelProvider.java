package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Locale;

/**
 * Generates item models for:
 *  - The Arsenal Pickaxe (2-layer model: head + rod)
 *  - All tool part items (heads and rods), using shared grayscale base textures
 *
 * The actual colors are applied at runtime via ModItemColors / AlloyMaterial.getItemColor().
 */
public class ArsenalItemModelProvider extends ItemModelProvider {

    private final PackOutput packOutput;


    public ArsenalItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ArsenalCrafting.MOD_ID, existingFileHelper);
        this.packOutput = output;
    }


    @Override
    protected void registerModels() {
        generateArsenalPickaxeModel();
        generateToolPartModels();
        generateAlloyItemModels();

        // If you have other items, you can add them here as well.
    }

    /**
     * Item model for the assembled Arsenal Pickaxe.
     *
     * File: assets/arsenalcrafting/models/item/arsenal_pickaxe.json
     *
     * Uses two grayscale base textures:
     *  - layer0: head (tintIndex 0)
     *  - layer1: rod  (tintIndex 1)
     *
     * The tint colors come from ModItemColors.colorArsenalPickaxe.
     */
    private void generateArsenalPickaxeModel() {
        getBuilder("arsenal_pickaxe")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/pickaxe_head_base"))
                .texture("layer1", modLoc("item/tool_rod_base"));
    }

    /**
     * Item models for all tool part items (heads + rods) for every AlloyMaterial.
     *
     * Each part item uses the same grayscale texture:
     *  - Heads -> pickaxe_head_base
     *  - Rods  -> tool_rod_base
     *
     * Colour is applied in ModItemColors.colorToolPart based on the item's AlloyMaterial.
     *
     * This expects your item registry names to follow:
     *  - "<material>_pickaxe_head"
     *  - "<material>_tool_rod"
     *
     * e.g. "bronze_pickaxe_head", "titanium_tool_rod"
     *
     * If your actual registry names differ, just tweak the strings below.
     */
    private void generateToolPartModels() {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String key = material.getName().toLowerCase(Locale.ROOT); // e.g. "bronze"

            // HEAD ITEMS
            getBuilder(key + "_pickaxe_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/pickaxe_head_item_base"));

            // ROD ITEMS
            getBuilder(key + "_tool_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/tool_rod_item_base"));

            getBuilder(key + "_core")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/tool_rod_item_base"));
            //TODO add base core image
        }
    }

    private void generateAlloyItemModels() {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String key = material.getName().toLowerCase(Locale.ROOT); // e.g. "bronze"

            // This will generate models like:
            //  - bronze_alloy.json
            //  - titanium_alloy.json
            //
            // If your registry names are different (e.g. "bronze_ingot"),
            // change "_alloy" to "_ingot" or whatever you use.
            getBuilder(key + "_alloy")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/alloy_base"));
        }
    }


    @Override
    public String getName() {
        return "ArsenalCrafting Item Models";
    }
}
