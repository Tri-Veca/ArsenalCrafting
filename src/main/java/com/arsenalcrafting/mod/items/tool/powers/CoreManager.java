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


    // ========== PUBLIC API ==========

    public static Collection<CoreManager.CoreDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static Optional<CoreManager.CoreDefinition> getOptional(String id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static CoreManager.CoreDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static boolean exists(String id) {
        return id != null && !id.isEmpty() && DEFINITIONS.containsKey(id);
    }
}