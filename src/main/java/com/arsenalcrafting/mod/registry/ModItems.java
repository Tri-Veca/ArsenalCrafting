package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyItem;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.augments.UniversalAugmentItem;
import com.arsenalcrafting.mod.items.gadgets.ArsenalDrillItem;
import com.arsenalcrafting.mod.items.gadgets.ArsenalSawItem;
import com.arsenalcrafting.mod.items.tool.*;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
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
    public static final Map<AlloyMaterial, DeferredHolder<Item, BucketItem>> ALLOY_BUCKETS =
            new HashMap<>();

    public static final DeferredHolder<Item, ArsenalSawItem> SAW =
            ITEMS.register("saw", () -> new ArsenalSawItem(new Item.Properties()));
    public static final DeferredHolder<Item, ArsenalDrillItem> DRILL =
            ITEMS.register("drill", () -> new ArsenalDrillItem(new Item.Properties()));


    public static final DeferredHolder<Item, RepairKitItem> REPAIR_KIT = ITEMS.register("repair_kit",
            () -> new RepairKitItem(new Item.Properties()));




    // --- Tools & Weapons (Base items use the lightest alloy by default) ---
    public static final DeferredHolder<Item, ArsenalPickaxeItem> ARSENAL_PICKAXE = ITEMS.register("arsenal_pickaxe",
            () -> new ArsenalPickaxeItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));

    public static final DeferredHolder<Item, ArsenalShovelItem> ARSENAL_SHOVEL = ITEMS.register("arsenal_shovel",
            () -> new ArsenalShovelItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));

    public static final DeferredHolder<Item, ArsenalAxeItem> ARSENAL_AXE = ITEMS.register("arsenal_axe",
            () -> new ArsenalAxeItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));

    public static final DeferredHolder<Item, ArsenalKatanaItem> ARSENAL_KATANA = ITEMS.register("arsenal_katana",
            () -> new ArsenalKatanaItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));
    public static final DeferredHolder<Item, ArsenalBroadSwordItem> ARSENAL_BROADSWORD = ITEMS.register("arsenal_broadsword",
            () -> new ArsenalBroadSwordItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));
    public static final DeferredHolder<Item, ArsenalDaggerItem> ARSENAL_DAGGER = ITEMS.register("arsenal_dagger",
            () -> new ArsenalDaggerItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));
    // --- UNIVERSAL AUGMENT ITEMS (Auto-generated from AugmentManager) ---
    // These are registered in a static block to ensure AugmentManager is initialized first
    static {
        // Wait until AugmentManager is loaded, then register universal items
        // Note: Static blocks run when class is loaded, which happens before registration

        // We'll use a helper method that gets called during mod construction
    }

    public static void registerAlloys() {
        System.out.println("[ModItems] Registering alloy items...");

        try {
            for (AlloyMaterial material : AlloyMaterial.values()) {
                System.out.println("[ModItems] Processing material: " + material.name());

                // skip vanilla materials
                if (!material.allowsAlloy()) {
                    System.out.println("[ModItems] Skipping vanilla material: " + material.name());
                    continue;
                }

                String itemId = material.name().toLowerCase() + "_alloy";
                System.out.println("[ModItems] Registering alloy: " + itemId);

                DeferredHolder<Item, Item> holder = ITEMS.register(
                        itemId,
                        () -> new AlloyItem(material, new Item.Properties())
                );

                ALLOY_ITEMS.put(material, holder);
                System.out.println("[ModItems] Successfully registered: " + itemId);
            }
        } catch (Exception e) {
            System.err.println("[ModItems] ERROR in registerAlloys!");
            e.printStackTrace();
            throw e;
        }
    }


    public static void registerToolComponents() {
        System.out.println("[ModItems] Registering tool components...");

        try {
            // Initialize maps for each material
            for (AlloyMaterial material : AlloyMaterial.values()) {
                TOOL_COMPONENTS.put(material, new HashMap<>());
            }

            // Register components for each material and type
            for (AlloyMaterial material : AlloyMaterial.values()) {
                System.out.println("[ModItems] Processing components for material: " + material.name());
                // Skip materials that don't generate parts (like coal)
                if (!material.shouldGenerateParts()) {
                    System.out.println("[ModItems] Skipping parts generation for: " + material.name());
                    continue;
                }

                for (ToolComponentType componentType : ToolComponentType.values()) {
                    String materialName = material.name().toLowerCase();
                    String componentId = componentType.getId();
                    String itemId = materialName + "_" + componentId;

                    System.out.println("[ModItems] Registering component: " + itemId);

                    try {
                        DeferredHolder<Item, ToolComponentItem> holder = ITEMS.register(
                                itemId,
                                () -> new ToolComponentItem(material, componentType, new Item.Properties())
                        );

                        TOOL_COMPONENTS.get(material).put(componentType, holder);
                        System.out.println("[ModItems] Successfully registered: " + itemId);
                    } catch (Exception e) {
                        System.err.println("[ModItems] ERROR registering component: " + itemId);
                        e.printStackTrace();
                        throw e;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[ModItems] ERROR in registerToolComponents!");
            e.printStackTrace();
            throw e;
        }
    }


    public static void registerAlloyBuckets() {
        System.out.println("[ModItems] Registering alloy buckets...");
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String id = material.getName().toLowerCase() + "_bucket";
            final AlloyMaterial mat = material;

            DeferredHolder<Item, BucketItem> holder = ITEMS.register(id,
                    () -> new BucketItem(
                            ModFluids.getSource(mat),
                            new Item.Properties()
                                    .stacksTo(1)
                                    .craftRemainder(Items.BUCKET)
                    )
            );
            ALLOY_BUCKETS.put(material, holder);
            System.out.println("[ModItems] Registered bucket: " + id);
        }
    }

    // Helper method to register all universal augment items
    public static void registerUniversalAugments() {
        System.out.println("[ModItems] Registering universal augment items...");

        try {
            var allAugments = AugmentManager.getAll();
            System.out.println("[ModItems] Found " + allAugments.size() + " augments to register");

            for (var def : allAugments) {
                System.out.println("[ModItems] Processing augment: " + def.id());

                try {
                    if (def.hasTiers()) {
                        // Register multiple tiers
                        for (int tier = 1; tier <= def.maxTier(); tier++) {
                            final int finalTier = tier;
                            String itemId = def.id() + "_augment.tier" + tier;

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
                        String itemId = def.id() + "_augment";

                        DeferredHolder<Item, UniversalAugmentItem> holder = ITEMS.register(
                                itemId,
                                () -> new UniversalAugmentItem(def.id(), 0,
                                        new Item.Properties().stacksTo(1))
                        );

                        UNIVERSAL_AUGMENTS.add(holder);
                        System.out.println("[ModItems] Registered: " + itemId);
                    }
                } catch (Exception e) {
                    System.err.println("[ModItems] ERROR registering augment: " + def.id());
                    e.printStackTrace();
                    throw e;
                }
            }

            System.out.println("[ModItems] Total universal augments registered: " + UNIVERSAL_AUGMENTS.size());
        } catch (Exception e) {
            System.err.println("[ModItems] ERROR in registerUniversalAugments!");
            e.printStackTrace();
            throw e;
        }
    }

    // TODO add seperate gadgets registry
    public static void register(IEventBus eventBus) {
        try {
            System.out.println("[ModItems] ============ STARTING ITEM REGISTRATION ============");

            System.out.println("[ModItems] Step 1: Registering universal augments...");
            registerUniversalAugments();
            System.out.println("[ModItems] Step 1: COMPLETE");

            System.out.println("[ModItems] Step 2: Registering alloys...");
            registerAlloys();
            System.out.println("[ModItems] Step 2: COMPLETE");

            System.out.println("[ModItems] Step 2b: Registering alloy buckets...");
            registerAlloyBuckets();
            System.out.println("[ModItems] Step 2b: COMPLETE");

            System.out.println("[ModItems] Step 3: Registering tool components...");
            registerToolComponents();
            System.out.println("[ModItems] Step 3: COMPLETE");

            System.out.println("[ModItems] Step 4: Registering with event bus...");
            ITEMS.register(eventBus);
            System.out.println("[ModItems] Step 4: COMPLETE");

            System.out.println("[ModItems] ============ ITEM REGISTRATION COMPLETE ============");
        } catch (Exception e) {
            System.err.println("[ModItems] !!!! FATAL ERROR DURING REGISTRATION !!!!");
            System.err.println("[ModItems] Error message: " + e.getMessage());
            System.err.println("[ModItems] Error class: " + e.getClass().getName());
            e.printStackTrace();
            throw new RuntimeException("Failed to register mod items", e);
        }
    }
}