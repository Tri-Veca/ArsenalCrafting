package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

public class ModCapabilities {

    public static void register(RegisterCapabilitiesEvent event) {
        // Alloy Smelter fluid capability (display tank)
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.ALLOY_SMELTER.get(),
                (be, side) -> be.DISPLAY_TANK
        );

        // Alloy Smelter item capability
        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                ModBlockEntities.ALLOY_SMELTER.get(),
                (be, side) -> be.getItemHandler()
        );

        // Lava Tank fluid capability
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                ModBlockEntities.LAVA_TANK.get(),
                (be, side) -> be.fluidTank
        );
    }
}