package com.arsenalcrafting.mod.config;

public class TestModeConfig {
    private static boolean TEST_MODE_ENABLED = false;
    public static boolean AUTO_SMELT_ENABLED = false;
    public static boolean ORE_MINER_ENABLED = false;
    public static boolean FORTUNE_ENABLED = false;


    public static void setTestMode(boolean enabled) {
        TEST_MODE_ENABLED = enabled;
        AUTO_SMELT_ENABLED = enabled;
        ORE_MINER_ENABLED = enabled;
        FORTUNE_ENABLED = enabled;

        System.out.println("[TestMode] Configuration Updated:");
        System.out.println("  - Master Test Mode: " + TEST_MODE_ENABLED);
        System.out.println("  - Auto-Smelt: " + AUTO_SMELT_ENABLED);
        System.out.println("  - Ore-Miner: " + ORE_MINER_ENABLED);
        System.out.println("  - Fortune: " + FORTUNE_ENABLED);
    }

    public static boolean isTestModeEnabled() {
        return TEST_MODE_ENABLED;
    }

    public static boolean isAutoSmeltEnabled() {
        return TEST_MODE_ENABLED && AUTO_SMELT_ENABLED;
    }
    public static boolean isOreMinerEnabled() {
        return TEST_MODE_ENABLED && ORE_MINER_ENABLED;
    }
    public static boolean isFortuneEnabled() {
        return TEST_MODE_ENABLED && FORTUNE_ENABLED;
    }


    // Optional: Keep these for manual debugging if needed
    public static void debugPrintStatus() {
        System.out.println("[TestMode] Current Status - Master: " + TEST_MODE_ENABLED + ", Auto-Smelt: " + AUTO_SMELT_ENABLED
                + ", Ore-Miner: " + ORE_MINER_ENABLED
                + ", Fortune: " + FORTUNE_ENABLED);
    }
}
