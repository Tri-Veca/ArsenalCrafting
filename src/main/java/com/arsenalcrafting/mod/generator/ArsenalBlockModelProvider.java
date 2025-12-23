package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockModelProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/**
 * Generates the 3D model files for blocks.
 * For Molds, we use the 'carpet' parent to give them that flat, placed look.
 */
public class ArsenalBlockModelProvider extends BlockModelProvider {

    public ArsenalBlockModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, ArsenalCrafting.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        System.out.println("[BlockModelProvider] Generating automated mold models...");

        ModBlocks.MOLD_BLOCKS.values().forEach(holder -> {
            String name = holder.getId().getPath();

            // withExistingParent creates the JSON file
            // We use 'minecraft:block/carpet' as the base shape
            // We map the 'wool' texture key to our mold's item texture
            withExistingParent(name, "minecraft:block/carpet")
                    .texture("wool", modLoc("item/" + name))
                    .renderType("translucent");
        });
    }
}