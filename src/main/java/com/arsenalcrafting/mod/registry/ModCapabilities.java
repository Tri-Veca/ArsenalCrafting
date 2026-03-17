package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;

import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.capabilities.Capabilities;

public class ModCapabilities {

    public static void register(RegisterCapabilitiesEvent event) {

        // ✅ Fluid capability
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                com.arsenalcrafting.mod.registry.ModBlockEntities.ALLOY_SMELTER.get(),
                (be, side) -> be.FLUID_TANK
        );

        // ✅ Item capability
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                com.arsenalcrafting.mod.registry.ModBlockEntities.ALLOY_SMELTER.get(),
                (be, side) -> be.getItemHandler()
        );
    }
}