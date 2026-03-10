package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.core.Direction;
import net.minecraft.data.PackOutput;
import net.minecraft.world.item.ItemDisplayContext;
import net.neoforged.neoforge.client.model.generators.ItemModelProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.Locale;

/**
 * Generates item models for:
 *  - The Arsenal Pickaxe (3-layer model: head + core+ rod)
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
        generateArsenalShovelModel();
        generateArsenalAxeModel();
        generateArsenalKatanaModel();
        generateToolPartModels();
        generateAlloyItemModels();
        generateAugmentModels();
        generateMoldItemModels();

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
                .texture("layer1", modLoc("item/tool_rod_base"))
                .texture("layer2", modLoc("item/core_base"));
    }

    private void generateArsenalShovelModel() {
        getBuilder("arsenal_shovel")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/shovel_head_base"))
                .texture("layer1", modLoc("item/tool_rod_base_shovel"))
                .texture("layer2", modLoc("item/core_base_shovel_1"));
    }

    private void generateArsenalAxeModel() {
        getBuilder("arsenal_axe")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/axe_head_base"))
                .texture("layer1", modLoc("item/tool_rod_base_axe"))
                .texture("layer2", modLoc("item/core_base_axe"));
    }

    private void generateArsenalKatanaModel() {
        getBuilder("arsenal_katana")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/katana_blade_base"))
                .texture("layer1", modLoc("item/katana_hilt_base"))
                .texture("layer2", modLoc("item/core_base_katana"));
    }

    /**
     * Item models for all tool part items (heads + core + rods) for every AlloyMaterial.
     *
     * Each part item uses the same grayscale texture:
     *  - Heads -> pickaxe_head_base / shovel_head_base
     *  - Rods  -> tool_rod_base / shovel_tool_rod_base
     *  - Cores -> core_base
     *
     * Colour is applied in ModItemColors.colorToolPart based on the item's AlloyMaterial.
     *
     * This expects your item registry names to follow:
     *  - "<material>_pickaxe_head"
     *  - "<material>_shovel_head"
     *  - "<material>_tool_rod"
     *  - "<material>_shovel_tool_rod"
     *  - "<material>_pickaxe_tool_rod"
     *  - "<material>_core"
     *
     * e.g. "bronze_pickaxe_head", "titanium_tool_rod", "diamond_core"
     */
    private void generateToolPartModels() {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String key = material.getName().toLowerCase(Locale.ROOT); // e.g. "bronze"

            // PICKAXE HEAD
            getBuilder(key + "_pickaxe_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/pickaxe_head_item_base"));

            // SHOVEL HEAD
            getBuilder(key + "_shovel_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/shovel_head_item_base"));
            //AXE HEAD
            getBuilder(key + "_axe_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/axe_head_item_base"));

            // GENERIC TOOL ROD (for pickaxe, or fallback)
            getBuilder(key + "_tool_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/tool_rod_item_base"));


            // SHOVEL-SPECIFIC TOOL ROD
            getBuilder(key + "_shovel_tool_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/tool_rod_base_shovel")); // Reuse regular rod texture

            // CORE
            getBuilder(key + "_core")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/core_item_base"));

            // KATANA BLADE
            getBuilder(key + "_katana_blade_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/katana_blade_item_base"));

            // KATANA HILT
            getBuilder(key + "_katana_hilt_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/katana_hilt_item_base"));
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

    private void generateAugmentModels() {
        // Loop through all registered augments in the Manager
        for (var def : AugmentManager.getAll()) {
            String id = def.id();

            if (def.hasTiers()) {
                // TIERED AUGMENTS (e.g. Efficiency)
                for (int i = 1; i <= def.maxTier(); i++) {
                    String itemName = id + "_augment.tier" + i;

                    // Logic to match your texture naming convention:
                    // Tier 1 -> "efficiency"
                    // Tier 2 -> "efficiency_t2"
                    String textureName = (i == 1) ? id : id + "_t" + i;

                    getBuilder(itemName)
                            .parent(new ModelFile.UncheckedModelFile("item/generated"))
                            .texture("layer0", modLoc("item/" + textureName));
                }
            } else {
                // SINGLE AUGMENTS (e.g. Auto Smelt)
                String itemName = id + "_augment";

                getBuilder(itemName)
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", modLoc("item/" + id));
            }
        }
    }

    private void generateMoldItemModels() {
        ModBlocks.MOLD_BLOCKS.values().forEach(holder -> {
            String name = holder.getId().getPath();
            withExistingParent(name, "item/generated")
                    .texture("layer0", modLoc("item/" + name));
        });

        // Bronze Item Models (Manual Textures)
        ModBlocks.BRONZE_MOLD_BLOCKS.values().forEach(holder -> {
            String name = holder.getId().getPath();

            // Points the item icon directly to your manual texture
            // No tinting, no layers.
            withExistingParent(name, "item/generated")
                    .texture("layer0", modLoc("item/" + name));
        });

    }


    @Override
    public String getName() {
        return "ArsenalCrafting Item Models";
    }
}