package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;

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
        entries.put("itemGroup.arsenalcrafting.0_arsenal_tab", "Arsenal Crafting");
        entries.put("itemGroup.arsenalcrafting.main", "Arsenal Crafting");
        entries.put("itemGroup.arsenalcrafting.1_arsenal_components_tab", "Arsenal Crafting Components");

        // 2. Blocks
        entries.put("block.arsenalcrafting.alloy_smelter_core", "Alloy Smelter Core");
        entries.put("block.arsenalcrafting.toolkit", "Tool Kit");
        entries.put("block.arsenalcrafting.void_casing", "Void Casing");
        entries.put("block.arsenalcrafting.iron_casing", "Iron Casing");
        entries.put("block.arsenalcrafting.lava_tank", "Lava Tank");
        entries.put("block.arsenalcrafting.alloy_smelter", "Alloy Smelter");
        entries.put("container.arsenalcrafting.alloy_smelter", "Alloy Smelter");

        // 3. Basic Items & Parts (Auto-generated Alloys)
        System.out.println("[LangGenerator] Adding alloy entries...");
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String materialName = material.getName();
            String materialId = material.name().toLowerCase();

            // Alloy ingot/item
            entries.put("item.arsenalcrafting." + materialId + "_alloy", materialName + " Alloy");
            System.out.println("[LangGenerator] Added: " + materialId + "_alloy");

            // Tool parts (optional - you can auto-generate these too if needed)
            entries.put("item.arsenalcrafting." + materialId + "_pickaxe_head", materialName + " Pickaxe Head");
            entries.put("item.arsenalcrafting." + materialId + "_shovel_head", materialName + " Shovel Head");
            entries.put("item.arsenalcrafting." + materialId + "_axe_head", materialName + " Axe Head");
            entries.put("item.arsenalcrafting." + materialId + "_tool_rod", materialName + " Tool Rod");
            entries.put("item.arsenalcrafting." + materialId + "_shovel_tool_rod", materialName + " Shovel Tool Rod");
            entries.put("item.arsenalcrafting." + materialId + "_core", materialName + " Core");
            entries.put("item.arsenalcrafting." + materialId + "_katana_blade_head", materialName + " Katana Blade");
            entries.put("item.arsenalcrafting." + materialId + "_katana_hilt_rod", materialName + " Katana Hilt");
        }

        // Other items

        entries.put("item.arsenalcrafting.arsenal_pickaxe", "Arsenal Pickaxe");
        entries.put("item.arsenalcrafting.arsenal_shovel", "Arsenal Shovel");
        entries.put("item.arsenalcrafting.arsenal_axe", "Arsenal Axe");
        entries.put("item.arsenalcrafting.arsenal_katana", "Arsenal Katana");
        entries.put("item.arsenalcrafting.repair_kit", "Tool Repair Kit");
        // TODO add seperate gadgets section
        entries.put("item.arsenalcrafting.drill", "Drill");
        entries.put("item.arsenalcrafting.saw", "Saw");


        System.out.println("[LangGenerator] Adding automated mold entries...");

// 1. Standard Molds (Gold appearance)
        for (var entry : ModBlocks.MOLD_BLOCKS.entrySet()) {
            ToolComponentType type = entry.getKey();
            Block block = entry.getValue().get();

            // Result: "Pickaxe Head Mold"
            entries.put(block.getDescriptionId(), type.getDisplayName() + " Mold");
        }

// 2. Bronze Molds (Tinted bronze)
        for (var entry : ModBlocks.BRONZE_MOLD_BLOCKS.entrySet()) {
            ToolComponentType type = entry.getKey();
            Block block = entry.getValue().get();

            // Result: "Bronze Pickaxe Head Mold"
            entries.put(block.getDescriptionId(), "Bronze " + type.getDisplayName() + " Mold");
        }

        // 4. Alloy material names (for tooltips)
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String materialName = material.getName();
            entries.put("material.arsenalcrafting." + material.name().toLowerCase(), materialName);
        }

        // 5. Tooltips & GUI
        entries.put("item.arsenalcrafting.tool_parts", "--- Assembled Parts ---");
        entries.put("item.arsenalcrafting.head_material", "Head: %s");
        entries.put("item.arsenalcrafting.rod_material", "Rod: %s");
        entries.put("item.arsenalcrafting.tool_material", "Material: %s");
        entries.put("item.arsenalcrafting.core_material", "Core: %s");
        entries.put("item.arsenalcrafting.durability", "Durability");
        entries.put("item.arsenalcrafting.efficiency", "Efficiency");
        entries.put("item.arsenalcrafting.repairability", "Repairability: %s");
        entries.put("item.arsenalcrafting.augment_slots", "Augment Slots: %s");
        entries.put("item.arsenalcrafting.alloy_color", "Alloy Color: %s");
        entries.put("item.arsenalcrafting.tin_alloy", "Tin");



        // 6. AUTOMATED CORE POWERS
        //  Pulls directly from CoreManager so you never have to manually add IDs here again
        System.out.println("[LangGenerator] Adding core power entries...");
        for (var coreDef : com.arsenalcrafting.mod.items.tool.powers.CoreManager.getAll()) {
            entries.put("core.arsenalcrafting." + coreDef.id(), coreDef.name());

            entries.put("core.arsenalcrafting." + coreDef.id() + ".desc", coreDef.description());
        }

        entries.put("item.arsenalcrafting.current_durability", "Remaining: %s Max: %s");
        entries.put("screen.arsenalcrafting.toolkit_title", "Tool Kit");
        entries.put("gui.arsenalcrafting.toolkit.title", "Tool Kit");
        entries.put("gui.arsenalcrafting.augment.title", "Augment Configuration");
        entries.put("tooltip.arsenalcrafting.click_to_configure", "§7§oShift-click to configure§r");
        entries.put( "gui.arsenalcrafting.tool.instructions", "Enable / Disable Augments Here");
        entries.put("gui.arsenalcrafting.status.enabled", "Enabled");
        entries.put("gui.arsenalcrafting.status.disabled", "Disabled");
        entries.put( "gui.arsenalcrafting.click_to_toggle", "Click to Toggle");
        entries.put( "gui.arsenalcrafting.tool", "Augment Menu");
        entries.put("tooltip.arsenalcrafting.installed_augments", "Installed Augments");

        // 7. Messages & Instructions
        entries.put("message.arsenalcrafting.augment_installed", "§a%s augment installed!");
        entries.put("message.arsenalcrafting.augment_removed", "§7%s augment removed.");
        entries.put("message.arsenalcrafting.incompatible_augment", "§cCannot install %s with %s already installed!");
        entries.put("message.arsenalcrafting.no_augment_slots", "§cNo augment slots available!");
        entries.put("message.arsenalcrafting.alloy_created", "§aCreated %s Alloy!");

        // 8. AUGMENTS - Generated from AugmentManager
        System.out.println("[LangGenerator] Adding augment entries...");
        for (var def : AugmentManager.getAll()) {
            String augmentName = def.name();

            // Augment item names
            if (def.hasTiers()) {
                for (int tier = 1; tier <= def.maxTier(); tier++) {
                    String itemKey = "item.arsenalcrafting." + def.id() + "_augment.tier" + tier;
                    String displayName = String.format("Tier %d %s Augment", tier, augmentName);
                    entries.put(itemKey, displayName);
                    System.out.println("[LangGenerator] Added: " + itemKey);

                    // Augment name for tooltips
                    String augmentKey = "augment.arsenalcrafting." + def.id() + "_augment.tier" + tier;
                    String tierRoman = switch(tier) {
                        case 1 -> "I";
                        case 2 -> "II";
                        case 3 -> "III";
                        case 4 -> "IV";
                        default -> String.valueOf(tier);
                    };
                    entries.put(augmentKey, augmentName + " " + tierRoman);

                    // Augment descriptions
                    entries.put("augment.arsenalcrafting." + def.id() + "_augment.tier" + tier + ".description",  def.description());
                }
            } else {
                String itemKey = "item.arsenalcrafting." + def.id() + "_augment";
                String displayName = augmentName + " Augment";
                entries.put(itemKey, displayName);
                System.out.println("[LangGenerator] Added: " + itemKey);

                // Augment name for tooltips
                String augmentKey = "augment.arsenalcrafting." + def.id();
                entries.put(augmentKey, augmentName);

                // Augment descriptions
                entries.put("augment.arsenalcrafting." + def.id() + ".description", def.description());
            }


        }

        // 9. Alloy properties for tooltips (optional)
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String materialId = material.name().toLowerCase();
            entries.put("tooltip.arsenalcrafting." + materialId + "_durability", "Base Durability: " + material.getDurability());
            entries.put("tooltip.arsenalcrafting." + materialId + "_efficiency", "Mining Speed: " + material.getEfficiency());
            entries.put("tooltip.arsenalcrafting." + materialId + "_damage", "Attack Damage: " + material.getDamage());
        }

        // ============ END OF ENTRIES ============

        System.out.println("[LangGenerator] Total entries: " + entries.size());
        System.out.println("[LangGenerator] Alloys processed: " + AlloyMaterial.values().length);
        System.out.println("[LangGenerator] Augments processed: " + AugmentManager.getAll().size());

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