package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.augments.AutoSmeltAugmentItem;
import com.arsenalcrafting.mod.items.augments.FortuneAugmentItem;
import com.arsenalcrafting.mod.items.augments.OreMinerAugmentItem;
import com.arsenalcrafting.mod.items.augments.UniversalAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolHeadItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolRodItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.List;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, ArsenalCrafting.MOD_ID);

    // Keep track of all universal augment items for reference
    public static final List<DeferredHolder<Item, UniversalAugmentItem>> UNIVERSAL_AUGMENTS = new ArrayList<>();

    // --- Core Alloy Parts (Phase 1: Copper/Bronze for MVP) ---
    public static final DeferredHolder<Item, Item> BRONZE_INGOT_PART = ITEMS.register("bronze_ingot_part",
            () -> new Item(new Item.Properties()));
    public static final DeferredHolder<Item, Item> DRILL = ITEMS.register("drill",
            () -> new Item(new Item.Properties()));

    // --- Modular Tool Parts (Bronze Alloy) ---
    public static final DeferredHolder<Item, ToolHeadItem> BRONZE_PICKAXE_HEAD = ITEMS.register("bronze_pickaxe_head",
            () -> new ToolHeadItem(AlloyMaterial.BRONZE, new Item.Properties()));
    public static final DeferredHolder<Item, ToolRodItem> BRONZE_TOOL_ROD = ITEMS.register("bronze_tool_rod",
            () -> new ToolRodItem(AlloyMaterial.BRONZE, new Item.Properties()));
    public static final DeferredHolder<Item, ToolHeadItem> TITANIUM_PICKAXE_HEAD = ITEMS.register("titanium_pickaxe_head",
            () -> new ToolHeadItem(AlloyMaterial.TITANIUM, new Item.Properties()));
    // Add Bronze Binding, Axe Head, Shovel Head, etc. later

    // --- Tools & Weapons (Base items use the lightest alloy by default) ---
    public static final DeferredHolder<Item, ArsenalPickaxeItem> ARSENAL_PICKAXE = ITEMS.register("arsenal_pickaxe",
            () -> new ArsenalPickaxeItem(AlloyMaterial.BRONZE, new Item.Properties().durability(1000)));

    // --- OLD AUGMENT ITEMS (Keep for backwards compatibility during migration) ---
    /*
    public static final DeferredHolder<Item, AutoSmeltAugmentItem> AUTO_SMELT_AUGMENT = ITEMS.register("auto_smelt_augment",
            () -> new AutoSmeltAugmentItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, OreMinerAugmentItem> ORE_MINER_AUGMENT = ITEMS.register("ore_miner_augment",
            () -> new OreMinerAugmentItem(new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, FortuneAugmentItem> FORTUNE_T1_AUGMENT = ITEMS.register("fortune_t1_augment",
            () -> new FortuneAugmentItem(1, new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, FortuneAugmentItem> FORTUNE_T2_AUGMENT = ITEMS.register("fortune_t2_augment",
            () -> new FortuneAugmentItem(2, new Item.Properties().stacksTo(1)));

    public static final DeferredHolder<Item, FortuneAugmentItem> FORTUNE_T3_AUGMENT = ITEMS.register("fortune_t3_augment",
            () -> new FortuneAugmentItem(3, new Item.Properties().stacksTo(1)));

     */

    // --- UNIVERSAL AUGMENT ITEMS (Auto-generated from AugmentManager) ---
    // These are registered in a static block to ensure AugmentManager is initialized first
    static {
        // Wait until AugmentManager is loaded, then register universal items
        // Note: Static blocks run when class is loaded, which happens before registration

        // We'll use a helper method that gets called during mod construction
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

        // Register all items with NeoForge
        ITEMS.register(eventBus);
    }
}