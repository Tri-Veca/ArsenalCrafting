package com.arsenalcrafting.mod.items.tool.powers;

public class RepairReduction {

    private RepairReduction() {} // Utility class

    // CONTROL VALUES HERE
    private static final float BONUS_MULTIPLIER = 1.20f; // 20% bonus
    private static final float VALUE_SAVING = 0.833f; // Inverse of 1.20

    public static float getBonusMultiplier() {
        return BONUS_MULTIPLIER;
    }

    public static float getValueSaving() {
        return VALUE_SAVING;
    }

    public static float getBonusPercentage() {
        return (BONUS_MULTIPLIER - 1) * 100;
    }

    public static float calculateRepairPercent(float baseEfficiency) {
        float baseRepair = 15.0f / baseEfficiency;
        return baseRepair * BONUS_MULTIPLIER;
    }

    public static int calculateValueUsed(float percentRepaired, float efficiency, int storedValue) {
        int baseValueUsed = Math.max(1, (int) Math.ceil((percentRepaired * efficiency) / 15.0f));
        int bonusValueUsed = (int)(baseValueUsed * VALUE_SAVING);
        return Math.min(bonusValueUsed, storedValue);
    }
}