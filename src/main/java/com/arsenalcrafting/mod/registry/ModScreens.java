package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.client.gui.RepairKitScreen;
import com.arsenalcrafting.mod.client.gui.ToolScreen;
import com.arsenalcrafting.mod.client.gui.ToolkitScreen;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/**
 * Client-side event handler for registering custom GUI screens.
 * This class must be registered to the MOD event bus in the main mod file.
 */
@EventBusSubscriber(value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ModScreens {

    /**
     * Registers the screen factory, linking our menu (ToolkitMenu) to our screen (ToolkitScreen).
     * @param event The NeoForge event triggered for registering menu screens.
     */
    @SubscribeEvent
    public static void registerScreens(RegisterMenuScreensEvent event) {
        // Link the server-side menu type to the client-side screen factory.
        event.register(ModMenus.TOOLKIT.get(), ToolkitScreen::new);
        event.register(ModMenus.TOOL.get(), ToolScreen::new);
        event.register(ModMenus.REPAIRKIT.get(), RepairKitScreen::new);
    }
}