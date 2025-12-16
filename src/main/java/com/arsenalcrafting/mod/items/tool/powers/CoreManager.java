package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.core.augments.AugmentHandler;
import com.arsenalcrafting.mod.core.augments.AugmentHandlers;
import com.arsenalcrafting.mod.core.augments.AugmentManager;

import java.util.*;

public class CoreManager {

    // Registry of all augment definitions
    private static final Map<String, CoreManager.CoreDefinition> DEFINITIONS = new LinkedHashMap<>();

    public record CoreDefinition(
            String id,
            String name,
            String description,
            //get core material
            Class<? extends CoreHandler> handlerClass


    ) {
    }

    static {
        // Register all augments - SINGLE SOURCE OF TRUTH
       /* register(new CoreDefinition(
                "low_dur_more_speed", "Speeder", "Lower Durability = faster mining speed",
                CoreHandlers.Speeder.class, false, 1
        ));




        // Future Core Powers go here ONLY
    }

    private static void register(AugmentManager.AugmentDefinition def) {
        if (def == null || def.id() == null || def.id().isEmpty()) {
            System.err.println("[AugmentManager] Attempted to register invalid augment definition: " + def);
            return;
        }
        DEFINITIONS.put(def.id(), def);
    }

    // ========== PUBLIC API ==========

    public static Collection<AugmentManager.AugmentDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static Optional<AugmentManager.AugmentDefinition> getOptional(String id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static AugmentManager.AugmentDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static boolean exists(String id) {
        return id != null && !id.isEmpty() && DEFINITIONS.containsKey(id);
    }



        */
    }
}
