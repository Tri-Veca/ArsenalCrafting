package com.arsenalcrafting.mod.items.tool.powers;

import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;

/**
 * Sturdy core power for Bronze.
 * 40% chance to not consume durability.
 */
public final class Sturdy {
    private Sturdy() {}

    private static final float SAVE_CHANCE = 0.4f;

    /**
     * Check if durability should be saved this time.
     */
    public static boolean trySaveDurability(RandomSource random) {
        return random.nextFloat() < SAVE_CHANCE;
    }

    public static float getSaveChance() {
        return SAVE_CHANCE;
    }

    public static String getSavePercentage() {
        return "40%";
    }
}