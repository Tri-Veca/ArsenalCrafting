package com.arsenalcrafting.mod.config;

import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;

import java.util.HashMap;
import java.util.Map;
//VERY OLD Class. Have not used since refactoring.
public class TestModeConfig {
    // ========== CORE STATE ==========
    private static boolean testModeEnabled = false;
    private static final Map<String, Boolean> augmentFlags = new HashMap<>();

    static {
        // Initialize all augment flags from AugmentManager
        for (var def : AugmentManager.getAll()) {
            augmentFlags.put(def.id(), false);
        }
    }

    // ========== PUBLIC API ==========

    /**
     * Enable/disable test mode for ALL augments
     */
    public static void setTestMode(boolean enabled) {
        testModeEnabled = enabled;

        // Set all augment flags
        for (String augmentId : augmentFlags.keySet()) {
            augmentFlags.put(augmentId, enabled);
        }

        logStatus();
    }

    /**
     * Check if test mode is globally enabled
     */
    public static boolean isTestModeEnabled() {
        return testModeEnabled;
    }

    /**
     * Enable/disable specific augment in test mode
     */
    public static void setAugmentEnabled(String augmentId, boolean enabled) {
        if (augmentFlags.containsKey(augmentId)) {
            augmentFlags.put(augmentId, enabled);
            System.out.println("[TestMode] " + augmentId + ": " + enabled);
        } else {
            System.out.println("[TestMode] WARNING: Unknown augment ID: " + augmentId);
        }
    }

    /**
     * Check if a specific augment is enabled in test mode
     */
    public static boolean isAugmentEnabled(String augmentId) {
        return testModeEnabled && augmentFlags.getOrDefault(augmentId, false);
    }

    // ========== BULK OPERATIONS ==========

    /**
     * Enable all augments that support a specific tool type
     */
    public static void enableAllForToolType(ArsenalToolType toolType) {
        for (var def : AugmentManager.getAll()) {
            var handler = AugmentManager.getHandler(def.id());
            if (handler != null && handler.supportsToolType(toolType)) {
                setAugmentEnabled(def.id(), true);
            }
        }
    }

    /**
     * Disable all augments
     */
    public static void disableAllAugments() {
        for (String augmentId : augmentFlags.keySet()) {
            augmentFlags.put(augmentId, false);
        }
    }

    /**
     * Reset to default state (test mode off, all augments disabled)
     */
    public static void reset() {
        testModeEnabled = false;
        disableAllAugments();
        System.out.println("[TestMode] Reset to default state");
    }

    // ========== DEBUG/STATUS ==========

    private static void logStatus() {
        System.out.println("[TestMode] === Current Status ===");
        System.out.println("  - Test Mode: " + testModeEnabled);

        for (var def : AugmentManager.getAll()) {
            String augmentId = def.id();
            boolean enabled = augmentFlags.getOrDefault(augmentId, false);
            System.out.println("  - " + augmentId + ": " + enabled);
        }
        System.out.println("[TestMode] =======================");
    }

    public static void debugPrintStatus() {
        logStatus();
    }
}