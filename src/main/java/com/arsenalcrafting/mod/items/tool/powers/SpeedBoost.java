package com.arsenalcrafting.mod.items.tool.powers;

import net.minecraft.world.item.ItemStack;

/**
 * SpeedBoost core power
 *
 * This core dynamically increases a tool's mining efficiency
 * as durability gets lower.
 *
 * IMPORTANT:
 * - This does NOT store state
 * - Efficiency is calculated LIVE from durability
 * - Repairing the tool automatically reduces the bonus
 *
 * This should be called from getDestroySpeed(...)
 */
public final class SpeedBoost {

    private SpeedBoost() {} // Utility class

    /**
     * Returns additional mining speed based on durability thresholds.
     *
     * This value should be ADDED to the tool's base destroy speed.
     *
     * @param tool The tool ItemStack
     * @return bonus mining efficiency (float)
     */
    public static float getMiningSpeedBonus(ItemStack tool) {
        // Tool must be damageable
        if (tool == null || !tool.isDamageableItem()) {
            return 0.0f;
        }

        int maxDamage = tool.getMaxDamage();
        if (maxDamage <= 0) {
            return 0.0f;
        }

        // Minecraft stores "damage taken", not durability remaining
        float damagePercent =
                (float) tool.getDamageValue() / (float) maxDamage;

        /*
         * Durability thresholds:
         *
         * 50% damaged  → +0.5 speed
         * 70% damaged  → +1.0 speed
         * 85% damaged  → +1.0 speed
         *
         * These stack progressively.
         */
        float bonus = 0.0f;

        if (damagePercent >= 0.5f) {
            bonus += 0.5f;
        }

        if (damagePercent >= 0.7f) {
            bonus += 1f;
        }

        if (damagePercent >= 0.85f) {
            bonus += 1f;
        }

        return bonus;
    }
}
