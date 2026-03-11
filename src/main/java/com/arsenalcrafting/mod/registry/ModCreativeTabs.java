package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
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
                        ModItems.UNIVERSAL_AUGMENTS.forEach(item -> output.accept(item.get()));
                        ModItems.ALLOY_ITEMS.forEach((key, value) -> output.accept(value.get()));
                        ModBlocks.BLOCKS.getEntries().forEach(block -> output.accept(block.get()));
                        output.accept(ModItems.DRILL.get());
                        output.accept(ModItems.SAW.get());
                        output.accept(ModItems.ARSENAL_PICKAXE.get());
                        output.accept(ModItems.ARSENAL_SHOVEL.get());
                        output.accept(ModItems.ARSENAL_AXE.get());
                        output.accept(ModItems.ARSENAL_KATANA.get());
                        output.accept(ModItems.ARSENAL_BROADSWORD.get());
                        output.accept(ModItems.REPAIR_KIT.get());
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



    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}