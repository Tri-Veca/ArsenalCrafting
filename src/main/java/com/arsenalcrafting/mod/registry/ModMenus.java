package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension; // <-- CORRECTED IMPORT: Replaces MenuTypeBuilder
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

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

    public static final DeferredHolder<MenuType<?>, MenuType<PowerBankMenu>> POWER_BANK =
            MENUS.register("power_bank", () ->
                    IMenuTypeExtension.create(PowerBankMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FossilGeneratorMenu>> FOSSIL_GENERATOR =
            MENUS.register("fossil_generator", () ->
                    IMenuTypeExtension.create(FossilGeneratorMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<BlueprintCastingTableMenu>> BLUEPRINT_CASTING_TABLE_MENU =
            MENUS.register("blueprint_casting_table", () ->
                    IMenuTypeExtension.create(BlueprintCastingTableMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CastingPackageMenu>> CASTING_PACKAGE_MENU =
            MENUS.register("casting_package_menu", () ->
                    IMenuTypeExtension.create(CastingPackageMenu::new));

    // Alloy Smelter Menu (Placeholder for liquid management/smelting GUI)
    // public static final DeferredHolder<MenuType<?>, MenuType<AlloySmelterMenu>> ALLOY_SMELTER =
    //        MENUS.register("alloy_smelter", () -> IIMenuTypeExtension.create(AlloySmelterMenu::new));

    public static void register(IEventBus eventBus) {
        MENUS.register(eventBus);
    }
}