package com.arsenalcrafting.mod;

import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.LevelEvent;

@Mod(ArsenalCrafting.MOD_ID)
public class ArsenalCrafting {
    public static final String MOD_ID = "arsenalcrafting";
    public ArsenalCrafting(IEventBus modEventBus) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(ArsenalCraftingClient::onClientSetup);

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
        NeoForge.EVENT_BUS.addListener(LevelEvent.Load.class, event -> {
            // When world loads, tell the Light handler to clean up
            if (event.getLevel() instanceof net.minecraft.world.level.Level level) {
                if (!level.isClientSide()) {
                    com.arsenalcrafting.mod.core.augments.AugmentHandlers.Light.cleanupOnWorldLoad(level);
                }
            }
        });
    }




    private void commonSetup(final FMLCommonSetupEvent event) {

        // Here you would register networking packets, capabilities, etc.
        // This is always safe to run on both client and server.
    }
}