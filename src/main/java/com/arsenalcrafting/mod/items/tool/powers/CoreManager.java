package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.world.item.ItemStack;

import javax.annotation.Nullable;
import java.util.*;

public class CoreManager {

    private static final Map<AlloyMaterial, CoreDefinition> DEFINITIONS = new EnumMap<>(AlloyMaterial.class);

    public record CoreDefinition(
            String id,
            String name,
            String description,
            CoreHandler handler
    ) {}

    static {
        register(AlloyMaterial.COPPER, new CoreDefinition(
                "speed_boost",
                "Durability Speed",
                "Higher mining speed when durability is low",
                CoreHandlers.SPEED_BOOST
        ));
        register(AlloyMaterial.IRON, new CoreDefinition(
                "repair_reduction",
                "Repair Reduction",
                "Decreases the repair cost slightly",
                CoreHandlers.REPAIR_REDUCTION
        ));
        register(AlloyMaterial.GOLD, new CoreDefinition(
                "greedy",
                "Greedy",
                "+2 Efficiency, -15% Durability",
                CoreHandlers.GREEDY
        ));
        register(AlloyMaterial.TIN, new CoreDefinition(
                "momentum",
                "Momentum",
                "Consecutive mining grants increasing Haste levels",
                CoreHandlers.MOMENTUM
        ));
        register(AlloyMaterial.NETHERITE, new CoreDefinition(
                "burn_protection",
                "Burn Protection",
                "Grants Item Burn Protection",
                CoreHandlers.BURN_PROTECTION
        ));
        register(AlloyMaterial.BRONZE, new CoreDefinition(
                "sturdy",
                "Sturdy",
                "40% chance to not use durability",
                CoreHandlers.STURDY
        ));
        register(AlloyMaterial.STEEL, new CoreDefinition(
                "workhorse",
                "WorkHorse",
                "Instantly breaks hard blocks regardless of tool tier",
                CoreHandlers.WORKHORSE
        ));
    }

    private static void register(AlloyMaterial material, CoreDefinition def) {
        DEFINITIONS.put(material, def);
    }

    @Nullable
    public static CoreDefinition getForMaterial(AlloyMaterial material) {
        return DEFINITIONS.get(material);
    }

    // NEW: Get core definition by material name string
    @Nullable
    public static CoreDefinition getForMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return null;
        }
        AlloyMaterial material = AlloyMaterial.fromName(materialName);
        return material != null ? DEFINITIONS.get(material) : null;
    }

    // NEW: Get core definition from item stack
    @Nullable
    public static CoreDefinition getFromTool(ItemStack tool) {
        ToolAssemblyData assemblyData = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (assemblyData == null) {
            return null;
        }
        String materialName = assemblyData.coreMaterial();
        return getForMaterialName(materialName);
    }

    // ========== PUBLIC API ==========

    public static Collection<CoreDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    @Nullable
    public static CoreDefinition get(String id) {
        // This searches by definition ID (e.g., "workhorse"), not material name
        for (CoreDefinition def : DEFINITIONS.values()) {
            if (def.id().equals(id)) {
                return def;
            }
        }
        return null;
    }

    public static boolean exists(String id) {
        return get(id) != null;
    }
}