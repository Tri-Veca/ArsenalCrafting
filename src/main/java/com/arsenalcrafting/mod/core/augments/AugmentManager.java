package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.function.BooleanSupplier;

public class AugmentManager {
    // Registry of all augment definitions
    private static final Map<String, AugmentDefinition> DEFINITIONS = new LinkedHashMap<>();

    public record AugmentDefinition(
            String id,
            String name,
            String description,
            Class<? extends AugmentHandler> handlerClass,
            boolean hasTiers,
            int maxTier
    ) {}

    static {
        // Register all augments - SINGLE SOURCE OF TRUTH
        register(new AugmentDefinition(
                "auto_smelt", "Auto Smelt", "Smelts ores automatically",
                AugmentHandlers.AutoSmelt.class, false, 1
        ));

        register(new AugmentDefinition(
                "ore_miner", "Ore Miner", "Vein mines ores",
                AugmentHandlers.OreMiner.class, false, 1
        ));

        register(new AugmentDefinition(
                "fortune", "Fortune", "Multiplies ore drops",
                AugmentHandlers.Fortune.class, true, 3
        ));

        register(new AugmentDefinition(
                "silk_touch", "Silk Touch", "Get the exact block",
                AugmentHandlers.SilkTouch.class, false, 1
        ));

        // Future augments go here ONLY
    }

    private static void register(AugmentDefinition def) {
        if (def == null || def.id() == null || def.id().isEmpty()) {
            System.err.println("[AugmentManager] Attempted to register invalid augment definition: " + def);
            return;
        }
        DEFINITIONS.put(def.id(), def);
    }

    // ========== PUBLIC API ==========

    public static Collection<AugmentDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static Optional<AugmentDefinition> getOptional(String id) {
        return Optional.ofNullable(DEFINITIONS.get(id));
    }

    public static AugmentDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static boolean exists(String id) {
        return id != null && !id.isEmpty() && DEFINITIONS.containsKey(id);
    }

    /**
     * Check if augment installed/enabled on a tool.
     * Safe if the dynamic component is missing — returns false.
     */
    public static boolean isInstalled(String augmentId, ItemStack tool) {
        if (augmentId == null || augmentId.isEmpty() || tool == null) return false;

        DataComponentType<Boolean> comp = getEnabledComponent(augmentId);
        if (comp == null) return false;

        try {
            return tool.getOrDefault(comp, false);
        } catch (Exception e) {
            // Defensive: if component access throws, treat as not installed
            System.err.println("[AugmentManager] isInstalled access error for " + augmentId + ": " + e.getMessage());
            return false;
        }
    }

    /**
     * Get tier for augment. Returns 1 by default (or 0 if not installed via getActiveTier).
     * Safe if component missing or definition missing.
     */
    public static int getTier(String augmentId, ItemStack tool) {
        if (augmentId == null || augmentId.isEmpty() || tool == null) return 1;
        AugmentDefinition def = get(augmentId);
        if (def == null) return 1;
        if (!def.hasTiers()) return 1;

        DataComponentType<Integer> comp = getTierComponent(augmentId);
        if (comp == null) return 1;
        try {
            return tool.getOrDefault(comp, 1);
        } catch (Exception e) {
            System.err.println("[AugmentManager] getTier access error for " + augmentId + ": " + e.getMessage());
            return 1;
        }
    }

    /**
     * Install augment on tool — safe no-op when components/definitions are missing.
     */
    public static void install(String augmentId, ItemStack tool, int tier) {
        if (augmentId == null || augmentId.isEmpty() || tool == null) return;

        DataComponentType<Boolean> enabledComp = getEnabledComponent(augmentId);
        if (enabledComp != null) {
            try {
                tool.set(enabledComp, true);
            } catch (Exception e) {
                System.err.println("[AugmentManager] Failed to set enabled component for " + augmentId + ": " + e.getMessage());
            }
        } else {
            System.err.println("[AugmentManager] Missing enabled component for " + augmentId + " — install skipped for boolean state");
        }

        AugmentDefinition def = get(augmentId);
        if (def != null && def.hasTiers()) {
            DataComponentType<Integer> tierComp = getTierComponent(augmentId);
            if (tierComp != null) {
                try {
                    tool.set(tierComp, Math.max(1, Math.min(tier, def.maxTier())));
                } catch (Exception e) {
                    System.err.println("[AugmentManager] Failed to set tier component for " + augmentId + ": " + e.getMessage());
                }
            } else {
                System.err.println("[AugmentManager] Missing tier component for " + augmentId + " — tier not recorded");
            }
        }
    }

    /**
     * Uninstall augment from tool — safe no-op when components missing.
     */
    public static void uninstall(String augmentId, ItemStack tool) {
        if (augmentId == null || augmentId.isEmpty() || tool == null) return;

        DataComponentType<Boolean> enabledComp = getEnabledComponent(augmentId);
        if (enabledComp != null) {
            try {
                tool.remove(enabledComp);
            } catch (Exception e) {
                System.err.println("[AugmentManager] Failed to remove enabled component for " + augmentId + ": " + e.getMessage());
            }
        }

        AugmentDefinition def = get(augmentId);
        if (def != null && def.hasTiers()) {
            DataComponentType<Integer> tierComp = getTierComponent(augmentId);
            if (tierComp != null) {
                try {
                    tool.remove(tierComp);
                } catch (Exception e) {
                    System.err.println("[AugmentManager] Failed to remove tier component for " + augmentId + ": " + e.getMessage());
                }
            }
        }
    }

    public static DataComponentType<Boolean> getEnabledComponent(String augmentId) {
        return DynamicComponents.getEnabledComponent(augmentId);
    }

    public static DataComponentType<Integer> getTierComponent(String augmentId) {
        return DynamicComponents.getTierComponent(augmentId);
    }

    /**
     * Instantiate the handler for an augment definition.
     * Returns null if definition missing or instantiation failed.
     */
    public static AugmentHandler getHandler(String augmentId) {
        AugmentDefinition def = get(augmentId);
        if (def == null) return null;

        try {
            return def.handlerClass().getDeclaredConstructor().newInstance();
        } catch (InstantiationException | IllegalAccessException |
                 InvocationTargetException | NoSuchMethodException e) {
            System.err.println("[AugmentManager] Failed to instantiate handler for " + augmentId + ": " + e.getMessage());
            return null;
        }
    }

    public static String getTranslationKey(String augmentId) {
        return "augment.arsenalcrafting." + augmentId;
    }

    public static BooleanSupplier getTestModeSupplier(String augmentId) {
        return () -> com.arsenalcrafting.mod.config.TestModeConfig.isAugmentEnabled(augmentId);
    }

    // ========== NEW METHODS FOR ARSENAL TOOL SYSTEM ==========

    /**
     * Check if an augment is installed AND enabled
     */
    public static boolean isActive(String augmentId, ItemStack tool) {
        return isInstalled(augmentId, tool);
    }

    /**
     * Get all active (installed and enabled) augments on a tool
     */
    public static List<String> getActiveAugments(ItemStack tool) {
        List<String> active = new ArrayList<>();
        if (tool == null) return active;

        if (tool.getItem() instanceof ArsenalTool) {
            var slots = ((ArsenalTool) tool.getItem()).getAugmentSlots(tool);
            if (slots != null) {
                for (int i = 0; i < 4; i++) {
                    String augmentId = slots.getAugmentInSlot(i);
                    if (augmentId != null && !augmentId.isEmpty() && isActive(augmentId, tool)) {
                        active.add(augmentId);
                    }
                }
            }
        }

        return active;
    }

    /**
     * Get the tier of an active augment (returns 0 if not active)
     */
    public static int getActiveTier(String augmentId, ItemStack tool) {
        if (isActive(augmentId, tool)) {
            return getTier(augmentId, tool);
        }
        return 0;
    }

    /**
     * Get all augments that support a specific tool type
     */
    public static List<AugmentDefinition> getAugmentsForToolType(ArsenalToolType toolType) {
        List<AugmentDefinition> result = new ArrayList<>();
        for (AugmentDefinition def : getAll()) {
            if (def == null || def.id() == null) continue;
            AugmentHandler handler = getHandler(def.id());
            if (handler != null) {
                try {
                    if (handler.supportsToolType(toolType)) result.add(def);
                } catch (Exception e) {
                    System.err.println("[AugmentManager] handler check failed for " + def.id() + ": " + e.getMessage());
                }
            }
        }
        return result;
    }

    /**
     * Check if an augment can be installed on a specific tool type
     */
    public static boolean canInstallOnToolType(String augmentId, ArsenalToolType toolType) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        AugmentHandler handler = getHandler(augmentId);
        return handler != null && handler.supportsToolType(toolType);
    }
}
