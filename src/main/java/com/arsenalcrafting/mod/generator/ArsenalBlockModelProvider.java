package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.Direction;
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

        // 2. Bronze Molds (Uses the gold texture + Bronze Tint)
        // 2. Bronze Molds (Manual Textures)
        ModBlocks.BRONZE_MOLD_BLOCKS.values().forEach(holder -> {
            String modelName = holder.getId().getPath();

            // This creates a flat block model (like a carpet)
            // It points to: assets/yourmod/textures/item/bronze_whatever_mold.png
            withExistingParent(modelName, "minecraft:block/carpet")
                    .texture("wool", modLoc("item/" + modelName))
                    .renderType("cutout"); // Ensures transparency in the mold hole works
        });


        // Alloy Smelter — two-layer models per tier per active state
        String[] tiers = {"unformed", "copper", "iron", "bronze", "steel", "titanium"};

        for (String tier : tiers) {
            String casingTex = tier.equals("unformed")
                    ? "arsenalcrafting:block/alloy_smelter_base"
                    : "arsenalcrafting:block/" + tier + "_casing";

            String topBottomTex = tier.equals("unformed")
                    ? "arsenalcrafting:block/basic_casing"
                    : "arsenalcrafting:block/" + tier + "_casing";

            for (String activeState : new String[]{"on", "off"}) {
                String modelName = "alloy_smelter_" + tier + "_" + activeState;
                String faceTex = "arsenalcrafting:block/alloy_smelter_" + activeState;

                var builder = withExistingParent(modelName, "minecraft:block/block")
                        .texture("particle", "arsenalcrafting:block/alloy_smelter_base")
                        .element()
                        .from(0, 0, 0).to(16, 16, 16)
                        .face(Direction.UP).uvs(0, 0, 16, 16).rotation(net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).texture("#casing").end()
                        .face(Direction.DOWN).uvs(0, 0, 16, 16).rotation(net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#casing").end()
                        .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#front").end()
                        .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#casing").end()
                        .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#casing").end()
                        .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#casing").end()
                        .end()
                        .element()
                        .from(0, 0, -0.01f).to(16, 16, 0)
                        .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#face").end()
                        .end()
                        .texture("front", casingTex)
                        .texture("casing", topBottomTex)
                        .texture("face", faceTex)
                        .renderType("cutout");

                if (activeState.equals("on")) {
                    builder.element()
                            .from(0, 0, -0.02f).to(16, 16, -0.01f)
                            .face(Direction.NORTH)
                            .uvs(0, 0, 16, 16).texture("#face")
                            .emissivity(15, 15)
                            .end()
                            .end();
                }
            }
        }

        // Lava Tank — per tier, glass window on front face
        String[] tankTiers = {"unformed", "copper", "iron", "bronze", "steel", "titanium"};

        for (String tier : tankTiers) {
            String casingTex = tier.equals("unformed")
                    ? "arsenalcrafting:block/basic_casing"
                    : "arsenalcrafting:block/" + tier + "_casing";

            String frontTex = tier.equals("unformed")
                    ? "arsenalcrafting:block/lava_tank_base"
                    : "arsenalcrafting:block/lava_tank_" + tier;

            String modelName = "lava_tank_" + tier;

            withExistingParent(modelName, "minecraft:block/block")
                    .texture("particle", "arsenalcrafting:block/lava_tank_base")
                    .element()
                    .from(0, 0, 0).to(16, 16, 16)
                    .face(Direction.UP).uvs(0, 0, 16, 16).rotation(net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).texture("#casing").end()
                    .face(Direction.DOWN).uvs(0, 0, 16, 16).rotation(net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#casing").end()
                    .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#front").end()
                    .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#casing").end()
                    .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#casing").end()
                    .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#casing").end()
                    .end()
                    .texture("casing", casingTex)
                    .texture("front", frontTex)
                    .renderType("cutout");
        }


        // Drain Casing Models - TWO layers like Alloy Smelter
        String[] casingTiers = {"copper", "iron", "bronze", "steel", "titanium"};

        for (String tier : casingTiers) {
            String casingTex = "arsenalcrafting:block/" + tier + "_casing";
            String drainFrontTex = "arsenalcrafting:block/drain_front";

            String modelName = tier + "_casing_drain";

            withExistingParent(modelName, "minecraft:block/block")
                    .texture("particle", casingTex)
                    // LAYER 1: Full casing block
                    .element()
                    .from(0, 0, 0).to(16, 16, 16)
                    .face(Direction.UP).uvs(0, 0, 16, 16).rotation(net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.COUNTERCLOCKWISE_90).texture("#casing").end()
                    .face(Direction.DOWN).uvs(0, 0, 16, 16).rotation(net.neoforged.neoforge.client.model.generators.ModelBuilder.FaceRotation.CLOCKWISE_90).texture("#casing").end()
                    .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#casing").end()
                    .face(Direction.SOUTH).uvs(0, 0, 16, 16).texture("#casing").end()
                    .face(Direction.EAST).uvs(0, 0, 16, 16).texture("#casing").end()
                    .face(Direction.WEST).uvs(0, 0, 16, 16).texture("#casing").end()
                    .end()
                    // LAYER 2: Thin drain overlay on north face (like the smelter face)
                    .element()
                    .from(0, 0, -0.01f).to(16, 16, 0)
                    .face(Direction.NORTH).uvs(0, 0, 16, 16).texture("#drain").end()
                    .end()
                    .texture("casing", casingTex)
                    .texture("drain", drainFrontTex)
                    .renderType("cutout");
        }
    }
}

