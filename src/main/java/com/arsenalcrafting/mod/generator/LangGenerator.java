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
        entries.put("itemGroup.arsenalcrafting.2_arsenal_fluids_tab", "Arsenal Fluids");

        // 2. Blocks
        entries.put("block.arsenalcrafting.alloy_smelter_core", "Alloy Smelter Core");
        entries.put("block.arsenalcrafting.toolkit", "Tool Kit");
        entries.put("block.arsenalcrafting.casting_station", "Casting Station");
        entries.put("block.arsenalcrafting.foundry_basin", "Foundry Basin");
        entries.put("block.arsenalcrafting.void_casing", "Void Casing");
        entries.put("block.arsenalcrafting.basic_casing", "Basic Casing");
        entries.put("block.arsenalcrafting.iron_casing", "Iron Casing");
        entries.put("block.arsenalcrafting.copper_casing", "Copper Casing");
        entries.put("block.arsenalcrafting.bronze_casing", "Bronze Casing");
        entries.put("block.arsenalcrafting.titanium_casing", "Titanium Casing");
        entries.put("block.arsenalcrafting.steel_casing", "Steel Casing");
        entries.put("block.arsenalcrafting.lava_tank", "Lava Tank");
        entries.put("block.arsenalcrafting.alloy_smelter", "Alloy Smelter");
        entries.put("container.arsenalcrafting.alloy_smelter", "Alloy Smelter");
        entries.put("block.arsenalcrafting.ingot_mold", "Ingot Mold");
        entries.put("block.arsenalcrafting.bronze_ingot_mold", "Bronze Ingot Mold");
        entries.put("block.arsenalcrafting.blueprint_casting_table", "Blueprint Casting Table");
        entries.put("block.arsenalcrafting.valve_faucet", "Valve Faucet");
        entries.put("block.arsenalcrafting.rutile_budding", "Rutile Budding Block");
        entries.put("block.arsenalcrafting.small_rutile_bud", "Small Rutile Bud");
        entries.put("block.arsenalcrafting.medium_rutile_bud", "Medium Rutile Bud");
        entries.put("block.arsenalcrafting.large_rutile_bud", "Large Rutile Bud");
        entries.put("block.arsenalcrafting.rutile_cluster", "Rutile Cluster");


        // 3. Basic Items & Parts (Auto-generated Alloys)
        System.out.println("[LangGenerator] Adding alloy entries...");
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String materialName = material.getName();
            String materialId = material.name().toLowerCase();

            // Alloy ingot/item
            entries.put("item.arsenalcrafting." + materialId + "_alloy", materialName + " Alloy");
            System.out.println("[LangGenerator] Added: " + materialId + "_alloy");

            // Molten bucket
            entries.put("item.arsenalcrafting." + materialId + "_bucket", "Bucket of Molten " + materialName);

            // Fluid type name (shows in fluid tooltips/JEI)
            entries.put("fluid_type.arsenalcrafting." + materialId + "_fluid_type", "Molten " + materialName);

            if (material.allowRaw()) {
                entries.put("item.arsenalcrafting.raw_" + materialId, "Raw " + materialName);
            }


            // Tool parts ONLY if shouldGenerateParts is true
            if (material.shouldGenerateParts()) {
                entries.put("item.arsenalcrafting." + materialId + "_pickaxe_head", materialName + " Pickaxe Head");
                entries.put("item.arsenalcrafting." + materialId + "_shovel_head", materialName + " Shovel Head");
                entries.put("item.arsenalcrafting." + materialId + "_axe_head", materialName + " Axe Head");
                entries.put("item.arsenalcrafting." + materialId + "_tool_rod", materialName + " Tool Rod");
                entries.put("item.arsenalcrafting." + materialId + "_shovel_tool_rod", materialName + " Shovel Tool Rod");
                entries.put("item.arsenalcrafting." + materialId + "_core", materialName + " Core");
                entries.put("item.arsenalcrafting." + materialId + "_katana_blade_head", materialName + " Katana Blade");
                entries.put("item.arsenalcrafting." + materialId + "_katana_hilt_rod", materialName + " Katana Hilt");
                entries.put("item.arsenalcrafting." + materialId + "_broadsword_blade_head", materialName + " Broadsword Blade");
                entries.put("item.arsenalcrafting." + materialId + "_broadsword_hilt_rod", materialName + " Broadsword Hilt");
                entries.put("item.arsenalcrafting." + materialId + "_dagger_blade_head", materialName + " Dagger Blade");
                entries.put("item.arsenalcrafting." + materialId + "_dagger_hilt_rod", materialName + " Dagger Hilt");
            }
        }

        // Other items

        entries.put("item.arsenalcrafting.arsenal_pickaxe", "Arsenal Pickaxe");
        entries.put("item.arsenalcrafting.arsenal_shovel", "Arsenal Shovel");
        entries.put("item.arsenalcrafting.arsenal_axe", "Arsenal Axe");
        entries.put("item.arsenalcrafting.arsenal_katana", "Arsenal Katana");
        entries.put("item.arsenalcrafting.arsenal_broadsword", "Arsenal Broadsword");
        entries.put("item.arsenalcrafting.arsenal_dagger", "Arsenal Dagger");
        entries.put("item.arsenalcrafting.repair_kit", "Tool Repair Kit");
        entries.put("item.arsenalcrafting.chromium", "Chromium");
        entries.put("item.arsenalcrafting.rutile_shard", "Rutile Shard");
        entries.put("item.arsenalcrafting.blueprint", "Blueprint");
        entries.put("item.arsenalcrafting.basic_augment", "Basic Augment");
        entries.put("item.arsenalcrafting.basic_material", "Basic Material");
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
        entries.put("block.arsenalcrafting.fossil_generator", "Fossil Generator");
        entries.put("jei.arsenalcrafting.augments", "Arsenal Augments");
        entries.put("jei.arsenalcrafting.toolkit", "Tool Assembly");
        entries.put("tooltip.arsenalcrafting.fluid.capacity", "Capacity: %s mb");
        entries.put("tooltip.arsenalcrafting.fluid.amount", "%s / %s mb (%s%%)");
        entries.put("tooltip.arsenalcrafting.fluid.tanks", "Connected tanks: %s");
        entries.put("tooltip.arsenalcrafting.fluid_tank.connected", "Connected to %s tank(s)");
        entries.put("tooltip.arsenalcrafting.fluid_tank.no_tanks", "No lava tanks connected!");
        entries.put("tooltip.arsenalcrafting.fluid_tank.empty", "Empty");
        entries.put("tooltip.arsenalcrafting.fluid_tank.capacity", "Capacity: %s mb");
        entries.put("tooltip.arsenalcrafting.fluid_tank.amount", "%s / %s mb (%s%%)");
        entries.put("tooltip.arsenalcrafting.fluid_tank.lava_temperature", "§cBurning hot! §7Used as fuel");
        entries.put("tooltip.arsenalcrafting.fluid_tank.burn_time", "§cFuel: §7%s seconds (%s cycles)");
        entries.put("fluid_type.arsenalcrafting.coal_fluid_type", "Hydrocarbon");
        entries.put("item.arsenalcrafting.coal_bucket", "Bucket of Hydrocarbon");





        // 6. AUTOMATED CORE POWERS
        //  Pulls directly from CoreManager so you never have to manually add IDs here again
        System.out.println("[LangGenerator] Adding core power entries...");
        for (var coreDef : com.arsenalcrafting.mod.items.tool.powers.CoreManager.getAll()) {
            entries.put("core.arsenalcrafting." + coreDef.id(), coreDef.name());

            entries.put("core.arsenalcrafting." + coreDef.id() + ".desc", coreDef.toolDescription());
            entries.put("core.arsenalcrafting." + coreDef.id() + ".desc.weapon", coreDef.weaponDescription());

        }
        for (var coreDef : com.arsenalcrafting.mod.items.tool.powers.CoreManager.getAllSteelVariants()) {
            entries.put("core.arsenalcrafting." + coreDef.id(), coreDef.name());
            entries.put("core.arsenalcrafting." + coreDef.id() + ".desc", coreDef.toolDescription());
            entries.put("core.arsenalcrafting." + coreDef.id() + ".desc.weapon", coreDef.weaponDescription());
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

        // 9. Alloy properties for tooltips (optional) ONLY if shouldGenerateParts is true
        for (AlloyMaterial material : AlloyMaterial.values()) {
            if (material.shouldGenerateParts()) {
                String materialId = material.name().toLowerCase();
                entries.put("tooltip.arsenalcrafting." + materialId + "_durability", "Base Durability: " + material.getDurability());
                entries.put("tooltip.arsenalcrafting." + materialId + "_efficiency", "Mining Speed: " + material.getEfficiency());
                entries.put("tooltip.arsenalcrafting." + materialId + "_damage", "Attack Damage: " + material.getDamage());

            }
        }
        // 10. Casting Packages

        for (com.arsenalcrafting.mod.core.alloy.PackageType type : com.arsenalcrafting.mod.core.alloy.PackageType.values()) {
            entries.put("item.arsenalcrafting." + type.getId(), type.getDisplayName());
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