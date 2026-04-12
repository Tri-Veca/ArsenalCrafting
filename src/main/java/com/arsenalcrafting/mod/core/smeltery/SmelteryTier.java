package com.arsenalcrafting.mod.core.smeltery;

public enum SmelteryTier {

    COPPER("Copper", 1, 9, 1.0f, 1.0f, false),
    IRON("Iron", 2, 9, 1.1f, 1.2f, false),
    STEEL("Steel", 3, 9, 1.15f, 1.33f, true),
    TITANIUM("Titanium", 5, 9, 1.2f, 1.5f, true);

    private final String displayName;
    private final int maxHeight;
    private final int slotsPerHeight;
    private final float speedMultiplier;
    private final float oreMultiplier;
    private final boolean supportsPowerConverter;

    SmelteryTier(String displayName, int maxHeight, int slotsPerHeight,
                 float speedMultiplier, float oreMultiplier, boolean supportsPowerConverter) {
        this.displayName = displayName;
        this.maxHeight = maxHeight;
        this.slotsPerHeight = slotsPerHeight;
        this.speedMultiplier = speedMultiplier;
        this.oreMultiplier = oreMultiplier;
        this.supportsPowerConverter = supportsPowerConverter;
    }

    public String getDisplayName() { return displayName; }
    public int getMaxHeight() { return maxHeight; }
    public int getSlotsPerHeight() { return slotsPerHeight; }
    public float getSpeedMultiplier() { return speedMultiplier; }
    public float getOreMultiplier() { return oreMultiplier; }
    public boolean supportsPowerConverter() { return supportsPowerConverter; }

    public int getTotalSlots(int height) {
        return Math.min(height, maxHeight) * slotsPerHeight;
    }

    public int getMaxLavaTanks(int height) {
        return Math.min(height, maxHeight);
    }

    public int getCraftTime(int baseCraftTime) {
        return Math.max(1, Math.round(baseCraftTime / speedMultiplier));
    }
}