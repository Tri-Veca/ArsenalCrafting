package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, ArsenalCrafting.MOD_ID);

    // Main Arsenal Crafting Tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ARSENAL_TAB = CREATIVE_MODE_TABS.register("arsenal_tab", () -> CreativeModeTab.builder()
            .icon(() -> new ItemStack(ItemStackHelper.getAlloyStack(AlloyMaterial.TITANIUM).getItem())) // Placeholder icon
            .title(Component.translatable("itemGroup." + ArsenalCrafting.MOD_ID + ".arsenal_tab"))
            .displayItems((parameters, output) -> {
                // Add all registered items to the tab
                ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                ModBlocks.BLOCKS.getEntries().forEach(block -> output.accept(block.get()));

            })
            .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}