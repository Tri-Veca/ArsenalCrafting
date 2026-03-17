package com.arsenalcrafting.mod;
import com.arsenalcrafting.mod.commands.ArsenalCommands;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.events.ArsenalAttributeEvents;
import com.arsenalcrafting.mod.events.ArsenalPlayerEvents;
import com.arsenalcrafting.mod.power.PowerNetworkEvents;
import com.arsenalcrafting.mod.registry.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
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
        AlloyRecipeType.register(modEventBus);
        ModRecipeSerializers.register(modEventBus);
        NeoForge.EVENT_BUS.register(ArsenalAttributeEvents.class);
        NeoForge.EVENT_BUS.register(ArsenalPlayerEvents.class);
        NeoForge.EVENT_BUS.register(PowerNetworkEvents.class);
        NeoForge.EVENT_BUS.register(ArsenalCommands.class);
        modEventBus.addListener(this::registerCapabilities);

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

    private void registerCapabilities(final RegisterCapabilitiesEvent event) {
        ModBlockEntities.registerCapabilities(event);
    }
}
