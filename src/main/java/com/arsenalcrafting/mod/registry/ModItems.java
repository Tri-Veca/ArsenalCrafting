package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyItem;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.augments.UniversalAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolHeadItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolRodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ArsenalCrafting.MOD_ID);

    // Keep track of all universal augment items for reference
    public static final List<DeferredHolder<Item, UniversalAugmentItem>> UNIVERSAL_AUGMENTS = new ArrayList<>();
    public static final Map<AlloyMaterial, DeferredHolder<Item, Item>> ALLOY_ITEMS = new HashMap<>();
    public static final Map<AlloyMaterial, Map<ToolComponentType, DeferredHolder<Item, ToolComponentItem>>> TOOL_COMPONENTS = new HashMap<>();

    // --- Core Alloy Parts (Phase 1: Copper/Bronze for MVP) ---
    /*public static final DeferredHolder<Item, Item> BRONZE_ALLOY = ITEMS.register("bronze_alloy",
            () -> new AlloyItem(AlloyMaterial.BRONZE, new Item.Properties()));

     */

    public static final DeferredHolder<Item, Item> DRILL = ITEMS.register("drill",
            () -> new Item(new Item.Properties()));

    // --- Modular Tool Parts (Bronze Alloy) ---
    /*
    public static final DeferredHolder<Item, ToolHeadItem> BRONZE_PICKAXE_HEAD = ITEMS.register("bronze_pickaxe_head",
            () -> new ToolHeadItem(AlloyMaterial.BRONZE, new Item.Properties()));
    public static final DeferredHolder<Item, ToolRodItem> BRONZE_TOOL_ROD = ITEMS.register("bronze_tool_rod",
            () -> new ToolRodItem(AlloyMaterial.BRONZE, new Item.Properties()));

    public static final DeferredHolder<Item, ToolHeadItem> TITANIUM_PICKAXE_HEAD = ITEMS.register("titanium_pickaxe_head",
            () -> new ToolHeadItem(AlloyMaterial.TITANIUM, new Item.Properties()));
    // Add Bronze Binding, Axe Head, Shovel Head, etc. later

     */

    // --- Tools & Weapons (Base items use the lightest alloy by default) ---
    public static final DeferredHolder<Item, ArsenalPickaxeItem> ARSENAL_PICKAXE = ITEMS.register("arsenal_pickaxe",
            () -> new ArsenalPickaxeItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));



    // --- UNIVERSAL AUGMENT ITEMS (Auto-generated from AugmentManager) ---
    // These are registered in a static block to ensure AugmentManager is initialized first
    static {
        // Wait until AugmentManager is loaded, then register universal items
        // Note: Static blocks run when class is loaded, which happens before registration

        // We'll use a helper method that gets called during mod construction
    }

    public static void registerAlloys() {
        System.out.println("[ModItems] Registering alloy items...");

        for (AlloyMaterial material : AlloyMaterial.values()) {
            System.out.println("[ModItems] Processing alloy: " + material.name());

            String itemId = material.name().toLowerCase() + "_alloy";

            DeferredHolder<Item, Item> holder = ITEMS.register(
                    itemId,
                    () -> new AlloyItem(material, new Item.Properties())
            );

            ALLOY_ITEMS.put(material, holder);
            System.out.println("[ModItems] Registered alloy item: " + itemId);
        }
    }

    // In ModItems.java - corrected

    public static void registerToolComponents() {
        System.out.println("[ModItems] Registering tool components...");

        // Initialize maps for each material
        for (AlloyMaterial material : AlloyMaterial.values()) {
            TOOL_COMPONENTS.put(material, new HashMap<>());
        }

        // Register components for each material and type
        for (AlloyMaterial material : AlloyMaterial.values()) {
            for (ToolComponentType componentType : ToolComponentType.values()) {
                String materialName = material.name().toLowerCase();
                String componentId = componentType.getId();

                String itemId = materialName + "_" + componentId;

                DeferredHolder<Item, ToolComponentItem> holder = ITEMS.register(
                        itemId,
                        () -> new ToolComponentItem(material, componentType, new Item.Properties())
                );

                TOOL_COMPONENTS.get(material).put(componentType, holder);
                System.out.println("[ModItems] Registered: " + itemId);
            }
        }
    }

    // Helper method to register all universal augment items
    public static void registerUniversalAugments() {
        System.out.println("[ModItems] Registering universal augment items...");

        for (var def : AugmentManager.getAll()) {
            System.out.println("[ModItems] Processing augment: " + def.id());

            if (def.hasTiers()) {
                // Register multiple tiers
                for (int tier = 1; tier <= def.maxTier(); tier++) {
                    final int finalTier = tier;
                    String itemId = def.id() + "_universal_augment_tier" + tier;

                    DeferredHolder<Item, UniversalAugmentItem> holder = ITEMS.register(
                            itemId,
                            () -> new UniversalAugmentItem(def.id(), finalTier,
                                    new Item.Properties().stacksTo(1))
                    );

                    UNIVERSAL_AUGMENTS.add(holder);
                    System.out.println("[ModItems] Registered: " + itemId + " (Tier " + tier + ")");
                }
            } else {
                // Register single tier (tier = 0)
                String itemId = def.id() + "_universal_augment";

                DeferredHolder<Item, UniversalAugmentItem> holder = ITEMS.register(
                        itemId,
                        () -> new UniversalAugmentItem(def.id(), 0,
                                new Item.Properties().stacksTo(1))
                );

                UNIVERSAL_AUGMENTS.add(holder);
                System.out.println("[ModItems] Registered: " + itemId);
            }
        }

        System.out.println("[ModItems] Total universal augments registered: " + UNIVERSAL_AUGMENTS.size());
    }



    // --- Other Key Items ---
    public static final DeferredHolder<Item, Item> REPAIR_KIT = ITEMS.register("repair_kit",
            () -> new Item(new Item.Properties()));

    public static void register(IEventBus eventBus) {
        // Register universal augment items
        registerUniversalAugments();
        registerAlloys();
        registerToolComponents();

        // Register all items with NeoForge
        ITEMS.register(eventBus);
    }
}