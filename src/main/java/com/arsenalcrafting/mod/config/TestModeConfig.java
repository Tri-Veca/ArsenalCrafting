package com.arsenalcrafting.mod.config;

public class TestModeConfig {
    private static boolean TEST_MODE_ENABLED = false;
    public static boolean AUTO_SMELT_ENABLED = false;

    public static void setTestMode(boolean enabled) {
        TEST_MODE_ENABLED = enabled;
        AUTO_SMELT_ENABLED = enabled;

        // Only log when the mode actually changes (not every call)
        System.out.println("[TestMode] Configuration Updated:");
        System.out.println("  - Master Test Mode: " + TEST_MODE_ENABLED);
        System.out.println("  - Auto-Smelt: " + AUTO_SMELT_ENABLED);
    }

    public static boolean isTestModeEnabled() {
        return TEST_MODE_ENABLED;
    }

    public static boolean isAutoSmeltEnabled() {
        return TEST_MODE_ENABLED && AUTO_SMELT_ENABLED;
    }

    // Optional: Keep these for manual debugging if needed
    public static void debugPrintStatus() {
        System.out.println("[TestMode] Current Status - Master: " + TEST_MODE_ENABLED + ", Auto-Smelt: " + AUTO_SMELT_ENABLED);
    }
}
