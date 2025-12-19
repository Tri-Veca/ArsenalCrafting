package com.arsenalcrafting.mod.items.tool.powers;

/**
 * Greedy core power
 *Change values here:
 */
public final class Greedy {

    private Greedy() {} // Utility class

    public static float getMiningSpeedBonus() {
        return 2.0f; // +2 Efficiency
    }

    public static float getDurabilityMultiplier() {
        return 0.85f; // -15% Total Durability
    }


}