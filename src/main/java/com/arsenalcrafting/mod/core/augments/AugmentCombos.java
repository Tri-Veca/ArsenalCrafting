package com.arsenalcrafting.mod.core.augments;

/**
 * Central configuration for augment combos and interactions.
 * Change these flags to enable/disable features during development.
 */
public class AugmentCombos {

    // ==================== MINING COMBOS ====================

    /**
     * When true: Ore Miner + Auto-Smelt will smelt entire veins
     * When false: Ore Miner always uses vanilla drops (even with Auto-Smelt)
     */
    public static final boolean AUTO_SMELT_WITH_ORE_MINER = true;

    /**
     * When true: Auto-Smelt + Fortune work together (smelt after fortune)
     * When false: They conflict (need to test which priority wins)
     */
    public static final boolean AUTO_SMELT_WITH_FORTUNE = true;

    // ==================== EXCLUSIVITY RULES ====================

    /**
     * When true: Silk Touch blocks Fortune from running
     * When false: Both can run (would be weird but useful for testing)
     */
    public static final boolean SILK_TOUCH_BLOCKS_FORTUNE = true;

    /**
     * When true: Ore Miner blocks Fortune from running (after vein mining)
     * When false: Fortune runs after Ore Miner (applies to entire vein)
     */
    public static final boolean ORE_MINER_BLOCKS_FORTUNE = false;

    // ==================== UTILITY COMBOS ====================

    /**
     * When true: Light and Magnetism can both be active
     * When false: They conflict (pick one)
     */
    public static final boolean LIGHT_WITH_MAGNETISM = true;

    /**
     * When true: Efficiency stacks with core powers (like Greedy)
     * When false: Only one speed modifier active at a time
     */
    public static final boolean EFFICIENCY_STACKS_WITH_CORES = true;

    // ==================== FUTURE COMBOS ====================

    /**
     * Placeholder for future weapon combos
     */
    public static final boolean FIRE_ASPECT_WITH_LIFE_STEAL = true;

    // ==================== HELPER METHODS ====================

    /**
     * Check if two augments should work together.
     * This is the central place to check combo compatibility.
     */
    public static boolean allowCombo(String augment1, String augment2) {
        // Auto-Smelt + Ore Miner
        if ((augment1.equals("auto_smelt") && augment2.equals("ore_miner")) ||
                (augment1.equals("ore_miner") && augment2.equals("auto_smelt"))) {
            return AUTO_SMELT_WITH_ORE_MINER;
        }

        // Auto-Smelt + Fortune
        if ((augment1.equals("auto_smelt") && augment2.equals("fortune")) ||
                (augment1.equals("fortune") && augment2.equals("auto_smelt"))) {
            return AUTO_SMELT_WITH_FORTUNE;
        }

        // Light + Magnetism
        if ((augment1.equals("light") && augment2.equals("magnetism")) ||
                (augment1.equals("magnetism") && augment2.equals("light"))) {
            return LIGHT_WITH_MAGNETISM;
        }

        // Default: allow combo
        return true;
    }

    /**
     * Check if an augment should block others from running.
     */
    public static boolean isExclusiveAgainst(String augment, String target) {
        // Silk Touch blocks Fortune
        if (augment.equals("silk_touch") && target.equals("fortune")) {
            return SILK_TOUCH_BLOCKS_FORTUNE;
        }

        // Ore Miner blocks Fortune
        if (augment.equals("ore_miner") && target.equals("fortune")) {
            return ORE_MINER_BLOCKS_FORTUNE;
        }

        return false;
    }
}