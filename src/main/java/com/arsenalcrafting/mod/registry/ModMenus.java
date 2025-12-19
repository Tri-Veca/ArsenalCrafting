package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.entity.ToolkitBlockEntity;
import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.arsenalcrafting.mod.client.menu.RepairKitMenu;
import com.arsenalcrafting.mod.client.menu.ToolMenu;
import com.arsenalcrafting.mod.client.menu.ToolkitMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension; // <-- CORRECTED IMPORT: Replaces MenuTypeBuilder
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, ArsenalCrafting.MOD_ID);

    // Toolkit Menu (For modular tool assembly)
    public static final DeferredHolder<MenuType<?>, MenuType<ToolkitMenu>> TOOLKIT =
            MENUS.register("toolkit", () -> IMenuTypeExtension.create(ToolkitMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<ToolMenu>> TOOL =
            MENUS.register("tool", () -> IMenuTypeExtension.create(ToolMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<RepairKitMenu>> REPAIRKIT =
            MENUS.register("repairkit", () -> IMenuTypeExtension.create(RepairKitMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AlloySmelterMenu>> ALLOY_SMELTER_MENU =
            MENUS.register("alloy_smelter_menu", () ->
                    IMenuTypeExtension.create((windowId, inv, data) ->
                            new AlloySmelterMenu(windowId, inv, data)));

    // Alloy Smelter Menu (Placeholder for liquid management/smelting GUI)
    // public static final DeferredHolder<MenuType<?>, MenuType<AlloySmelterMenu>> ALLOY_SMELTER =
    //        MENUS.register("alloy_smelter", () -> IIMenuTypeExtension.create(AlloySmelterMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}