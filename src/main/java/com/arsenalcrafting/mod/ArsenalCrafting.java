package com.arsenalcrafting.mod;

import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(ArsenalCrafting.MOD_ID)
public class ArsenalCrafting {
    public static final String MOD_ID = "arsenalcrafting";
    public ArsenalCrafting(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);

        // Register all deferred registries
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenus.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModComponents.register(modEventBus);
        TestModeConfig.setTestMode(false);
        System.out.println("[ArsenalCrafting] Constructor completed - Test Mode should be OFF");


        // IMPORTANT: The client setup (ModScreens) is handled automatically
        // by the @EventBusSubscriber annotation on the ModScreens class.
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // Here you would register networking packets, capabilities, etc.
        // This is always safe to run on both client and server.
    }
}