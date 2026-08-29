package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.PackageType;
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
        generateArsenalBroadswordModel();
        generateArsenalDaggerModel();
        generateToolPartModels();
        generateAlloyItemModels();
        generateAugmentModels();
        generateMoldItemModels();
        generateCastingPackageModels();
        generateAlloyBucketModels();
        generateAlloySmelterModel();
    }

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

    private void generateArsenalBroadswordModel() {
        getBuilder("arsenal_broadsword")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/broadsword_blade_base"))
                .texture("layer1", modLoc("item/broadsword_hilt_base"))
                .texture("layer2", modLoc("item/core_base_broadsword"));
    }

    private void generateArsenalDaggerModel() {
        getBuilder("arsenal_dagger")
                .parent(new ModelFile.UncheckedModelFile(mcLoc("item/handheld")))
                .texture("layer0", modLoc("item/dagger_blade_base"))
                .texture("layer1", modLoc("item/dagger_hilt_base"))
                .texture("layer2", modLoc("item/core_base_dagger"));
    }

    private void generateToolPartModels() {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String key = material.getName().toLowerCase(Locale.ROOT);

            getBuilder(key + "_pickaxe_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/pickaxe_head_item_base"));

            getBuilder(key + "_shovel_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/shovel_head_item_base"));

            getBuilder(key + "_axe_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/axe_head_item_base"));

            getBuilder(key + "_tool_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/tool_rod_item_base"));

            getBuilder(key + "_shovel_tool_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/tool_rod_base_shovel"));

            getBuilder(key + "_core")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/core_item_base"));

            // KATANA
            getBuilder(key + "_katana_blade_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/katana_blade_item_base"));

            getBuilder(key + "_katana_hilt_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/katana_hilt_item_base"));

            // BROADSWORD
            getBuilder(key + "_broadsword_blade_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/broadsword_blade_item_base"));

            getBuilder(key + "_broadsword_hilt_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/broadsword_hilt_item_base"));

            // DAGGER
            getBuilder(key + "_dagger_blade_head")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/dagger_blade_item_base"));

            getBuilder(key + "_dagger_hilt_rod")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/dagger_hilt_item_base"));
        }
    }

    private void generateAlloyItemModels() {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String key = material.getName().toLowerCase(Locale.ROOT);
            getBuilder(key + "_alloy")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/alloy_base"));
        }
    }

    private void generateAugmentModels() {
        for (var def : AugmentManager.getAll()) {
            String id = def.id();
            if (def.hasTiers()) {
                for (int i = 1; i <= def.maxTier(); i++) {
                    String itemName = id + "_augment.tier" + i;
                    String textureName = (i == 1) ? id : id + "_t" + i;
                    getBuilder(itemName)
                            .parent(new ModelFile.UncheckedModelFile("item/generated"))
                            .texture("layer0", modLoc("item/" + textureName));
                }
            } else {
                getBuilder(id + "_augment")
                        .parent(new ModelFile.UncheckedModelFile("item/generated"))
                        .texture("layer0", modLoc("item/" + id));
            }
        }
    }

    private void generateAlloyBucketModels() {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String key = material.getName().toLowerCase(Locale.ROOT);
            getBuilder(key + "_bucket")
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", mcLoc("arsenalcrafting:item/bucket_base"))          // vanilla iron bucket shape
                    .texture("layer1", modLoc("item/molten_bucket_fluid")); // grayscale fluid fill
        }
    }

    private void generateCastingPackageModels() {
        for (PackageType type : PackageType.values()) {
            getBuilder(type.getId())
                    .parent(new ModelFile.UncheckedModelFile(mcLoc("item/generated")))
                    .texture("layer0", modLoc("item/" + type.getId()));
        }
    }

    private void generateMoldItemModels() {
        ModBlocks.MOLD_BLOCKS.values().forEach(holder -> {
            String name = holder.getId().getPath();
            withExistingParent(name, "item/generated")
                    .texture("layer0", modLoc("item/" + name));
        });


        ModBlocks.BRONZE_MOLD_BLOCKS.values().forEach(holder -> {
            String name = holder.getId().getPath();
            withExistingParent(name, "item/generated")
                    .texture("layer0", modLoc("item/" + name));
        });
    }
    private void generateAlloySmelterModel() {
        getBuilder("alloy_smelter")
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/alloy_smelter_unformed_off")));
        getBuilder("lava_tank")
                .parent(new ModelFile.UncheckedModelFile(modLoc("block/lava_tank_unformed")));
    }



    @Override
    public String getName() {
        return "ArsenalCrafting Item Models";
    }
}