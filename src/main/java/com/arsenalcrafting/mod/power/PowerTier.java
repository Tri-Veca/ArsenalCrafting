package com.arsenalcrafting.mod.power;

public enum PowerTier {

    COPPER(1, 100_000, 8, 100),
    IRON(2, 500_000, 16, 200),
    BRONZE(3, 1_000_000, 32, 400),
    STEEL(4, 2_000_000, 64, 800),
    TITANIUM(5, 10_000_000, 128, 2000);

    private final int level;
    private final int bankCapacity;
    private final int generatorOutput;
    private final int transferRate;

    PowerTier(int level, int bankCapacity, int generatorOutput, int transferRate) {
        this.level = level;
        this.bankCapacity = bankCapacity;
        this.generatorOutput = generatorOutput;
        this.transferRate = transferRate;
    }

    public int level() {
        return level;
    }

    public int bankCapacity() {
        return bankCapacity;
    }

    public int generatorOutput() {
        return generatorOutput;
    }

    public int transferRate() {
        return transferRate;
    }

    public static PowerTier fromLevel(int level) {
        for (PowerTier tier : values()) {
            if (tier.level == level) return tier;
        }
        return COPPER;
    }
}