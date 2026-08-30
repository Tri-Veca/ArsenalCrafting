package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.core.alloy.PrefilledPackageRegistry;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.LiquidBlock;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Map;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsenalCrafting.MOD_ID);

    // Main Arsenal Crafting Tab — registered first
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARSENAL_TAB =
            CREATIVE_MODE_TABS.register("0_arsenal_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ItemStackHelper.getAlloyStack(AlloyMaterial.TITANIUM).getItem()))
                    .title(Component.translatable("itemGroup." + ArsenalCrafting.MOD_ID + ".0_arsenal_tab"))
                    .displayItems((parameters, output) -> {
                        // Augments
                        ModItems.UNIVERSAL_AUGMENTS.forEach(item -> output.accept(item.get()));

                        // Alloy Items
                        ModItems.ALLOY_ITEMS.forEach((key, value) -> output.accept(value.get()));
                        // Raw Items
                        ModItems.RAW_ITEMS.forEach((key, value) -> output.accept(value.get()));

                        // Blocks (skip fluid blocks)
                        ModBlocks.BLOCKS.getEntries().forEach(block -> {
                            if (block.get() instanceof LiquidBlock) { return; }
                            output.accept(block.get());
                        });

                        // Tools and Weapons
                        output.accept(ModItems.DRILL.get());
                        output.accept(ModItems.SAW.get());
                        output.accept(ModItems.ARSENAL_PICKAXE.get());
                        output.accept(ModItems.ARSENAL_SHOVEL.get());
                        output.accept(ModItems.ARSENAL_AXE.get());
                        output.accept(ModItems.ARSENAL_KATANA.get());
                        output.accept(ModItems.ARSENAL_BROADSWORD.get());
                        output.accept(ModItems.ARSENAL_DAGGER.get());

                        // Utilities
                        output.accept(ModItems.REPAIR_KIT.get());
                        output.accept(ModItems.BLUEPRINT.get());
                        output.accept(ModItems.BASIC_MATERIAL.get());


                        // ========== CASTING PACKAGES (Pre-filled) ==========
                        // Add both standard and bronze versions for all package types
                        for (PackageType type : PackageType.values()) {
                            output.accept(PrefilledPackageRegistry.getStandardPackage(type));
                            //output.accept(PrefilledPackageRegistry.getBronzePackage(type));
                        }
                    })
                    .build());

    // Components tab — registered second
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARSENAL_COMPONENTS_TAB =
            CREATIVE_MODE_TABS.register("1_arsenal_components_tab", () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ItemStackHelper.getToolRod(AlloyMaterial.BRONZE).getItem()))
                    .title(Component.translatable("itemGroup." + ArsenalCrafting.MOD_ID + ".1_arsenal_components_tab"))
                    .displayItems((parameters, output) -> {
                        for (ToolComponentType type : ToolComponentType.values()) {
                            ItemStackHelper.getAllComponentsOfType(type)
                                    .forEach(item -> output.accept(item.getItem()));
                        }
                    })
                    .build());

    // Fluids tab — registered third
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARSENAL_FLUIDS_TAB =
            CREATIVE_MODE_TABS.register("2_arsenal_fluids_tab", () -> CreativeModeTab.builder()
                    .icon(() -> {
                        // Use the first bucket as the tab icon — Copper
                        var copperBucket = ModItems.ALLOY_BUCKETS.get(AlloyMaterial.COPPER);
                        return copperBucket != null
                                ? new ItemStack(copperBucket.get())
                                : new ItemStack(Items.BUCKET);
                    })
                    .title(Component.translatable("itemGroup." + ArsenalCrafting.MOD_ID + ".2_arsenal_fluids_tab"))
                    .displayItems((parameters, output) -> {
                        // One bucket per material in AlloyMaterial order
                        for (AlloyMaterial material : AlloyMaterial.values()) {
                            var bucket = ModItems.ALLOY_BUCKETS.get(material);
                            if (bucket != null) output.accept(bucket.get());
                        }
                    })
                    .build());


    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}