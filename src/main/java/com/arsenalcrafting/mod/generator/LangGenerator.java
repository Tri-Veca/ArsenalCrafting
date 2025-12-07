package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class LangGenerator implements DataProvider {
    private final PackOutput packOutput;

    public LangGenerator(PackOutput packOutput) {
        this.packOutput = packOutput;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        System.out.println("[LangGenerator] Generating complete language file...");

        Map<String, String> entries = new LinkedHashMap<>();

        // ============ ADD EVERYTHING HERE ============

        // 1. Mod & Creative Tab
        entries.put("mod.arsenalcrafting", "Arsenal Crafting");
        entries.put("itemGroup.arsenalcrafting.arsenal_tab", "Arsenal Crafting");
        entries.put("itemGroup.arsenalcrafting.main", "Arsenal Crafting");

        // 2. Blocks
        entries.put("block.arsenalcrafting.alloy_smelter_core", "Alloy Smelter Core");
        entries.put("block.arsenalcrafting.toolkit", "Tool Kit");
        entries.put("block.arsenalcrafting.void_casing", "Void Casing");
        entries.put("block.arsenalcrafting.iron_casing", "Iron Casing");
        entries.put("block.arsenalcrafting.alloy_smelter", "Alloy Smelter");

        // 3. Basic Items & Parts
        entries.put("item.arsenalcrafting.copper_ingot_part", "Copper Ingot");
        entries.put("item.arsenalcrafting.bronze_ingot_part", "Bronze Ingot");
        entries.put("item.arsenalcrafting.bronze_pickaxe_head", "Bronze Pickaxe Head");
        entries.put("item.arsenalcrafting.bronze_tool_rod", "Bronze Tool Rod");
        entries.put("item.arsenalcrafting.titanium_pickaxe_head", "Titanium Pickaxe Head");
        entries.put("item.arsenalcrafting.arsenal_pickaxe", "Arsenal Pickaxe");
        entries.put("item.arsenalcrafting.repair_kit", "Tool Repair Kit");
        entries.put("item.arsenalcrafting.drill", "Drill");

        // 4. Tooltips & GUI
        entries.put("item.arsenalcrafting.tool_parts", "--- Assembled Parts ---");
        entries.put("item.arsenalcrafting.head_material", "Head: %s");
        entries.put("item.arsenalcrafting.rod_material", "Rod: %s");
        entries.put("item.arsenalcrafting.tool_material", "Material: %s");
        entries.put("item.arsenalcrafting.durability", "Durability: %s");
        entries.put("item.arsenalcrafting.efficiency", "Efficiency: %s");
        entries.put("item.arsenalcrafting.repairability", "Repairability: %s");
        entries.put("item.arsenalcrafting.augment_slots", "Augment Slots: %s");

        entries.put("screen.arsenalcrafting.toolkit_title", "Tool Kit");
        entries.put("gui.arsenalcrafting.toolkit.title", "Tool Kit");
        entries.put("gui.arsenalcrafting.augment.title", "Augment Configuration");
        entries.put("tooltip.arsenalcrafting.click_to_configure", "§7§oShift-click to configure§r");

        // 5. Messages & Instructions
        entries.put("message.arsenalcrafting.augment_installed", "§a%s augment installed!");
        entries.put("message.arsenalcrafting.augment_removed", "§7%s augment removed.");
        entries.put("message.arsenalcrafting.incompatible_augment", "§cCannot install %s with %s already installed!");
        entries.put("message.arsenalcrafting.no_augment_slots", "§cNo augment slots available!");

        // 6. AUGMENTS - Generated from AugmentManager
        System.out.println("[LangGenerator] Adding augment entries...");
        for (var def : AugmentManager.getAll()) {
            String augmentName = def.name();

            // Augment item names
            if (def.hasTiers()) {
                for (int tier = 1; tier <= def.maxTier(); tier++) {
                    String itemKey = "item.arsenalcrafting." + def.id() + "_universal_augment_tier" + tier;
                    String displayName = String.format("Tier %d %s Augment", tier, augmentName);
                    entries.put(itemKey, displayName);
                    System.out.println("[LangGenerator] Added: " + itemKey);

                    // Augment name for tooltips
                    String augmentKey = "augment.arsenalcrafting." + def.id() + ".tier" + tier;
                    String tierRoman = switch(tier) {
                        case 1 -> "I";
                        case 2 -> "II";
                        case 3 -> "III";
                        case 4 -> "IV";
                        default -> String.valueOf(tier);
                    };
                    entries.put(augmentKey, augmentName + " " + tierRoman);
                }
            } else {
                String itemKey = "item.arsenalcrafting." + def.id() + "_universal_augment";
                String displayName = augmentName + " Augment";
                entries.put(itemKey, displayName);
                System.out.println("[LangGenerator] Added: " + itemKey);

                // Augment name for tooltips
                String augmentKey = "augment.arsenalcrafting." + def.id();
                entries.put(augmentKey, augmentName);
            }
        }

        // 7. Add placeholder for future augments (optional)

        // ============ END OF ENTRIES ============

        System.out.println("[LangGenerator] Total entries: " + entries.size());

        // Create JSON object
        JsonObject langJson = new JsonObject();

        // Sort alphabetically for readability
        entries.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> langJson.addProperty(entry.getKey(), entry.getValue()));

        // Get output path
        Path langPath = packOutput.getOutputFolder(PackOutput.Target.RESOURCE_PACK)
                .resolve(ArsenalCrafting.MOD_ID + "/lang/en_us.json");

        System.out.println("[LangGenerator] Writing complete lang file to: " + langPath.toAbsolutePath());

        return DataProvider.saveStable(cache, langJson, langPath);
    }

    @Override
    public String getName() {
        return "Arsenal Crafting Complete Language Generator";
    }
}