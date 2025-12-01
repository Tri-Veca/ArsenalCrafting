package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.augments.AutoSmeltAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolHeadItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolRodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ArsenalCrafting.MOD_ID);

    // --- Core Alloy Parts (Phase 1: Copper/Bronze for MVP) ---
    public static final DeferredHolder<Item, Item> COPPER_INGOT_PART = ITEMS.register("copper_ingot_part",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> BRONZE_INGOT_PART = ITEMS.register("bronze_ingot_part",
            () -> new Item(new Item.Properties()));

    // --- Modular Tool Parts (Bronze Alloy) ---
    public static final DeferredHolder<Item, ToolHeadItem> BRONZE_PICKAXE_HEAD = ITEMS.register("bronze_pickaxe_head",
            () -> new ToolHeadItem(new Item.Properties()));
    public static final DeferredHolder<Item, ToolRodItem> BRONZE_TOOL_ROD = ITEMS.register("bronze_tool_rod",
            () -> new ToolRodItem(new Item.Properties()));
    // Add Bronze Binding, Axe Head, Shovel Head, etc. later

    // --- Tools & Weapons (Base items use the lightest alloy by default) ---
    public static final DeferredHolder<Item, ArsenalPickaxeItem> ARSENAL_PICKAXE = ITEMS.register("arsenal_pickaxe",
            () -> new ArsenalPickaxeItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));

    public static final DeferredHolder<Item, AutoSmeltAugmentItem> AUTO_SMELT_AUGMENT = ITEMS.register("auto_smelt_augment",
            () -> new AutoSmeltAugmentItem(new Item.Properties()));

    // --- Other Key Items ---
    public static final DeferredHolder<Item, Item> REPAIR_KIT = ITEMS.register("repair_kit",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}