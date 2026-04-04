package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;

import java.util.HashMap;
import java.util.Map;

/**
 * Single source of truth for casting costs and setting times.
 * Tune ingot costs and time multipliers here without touching block logic.
 */
public class CastingDatabase {

    // ── INGOT COSTS PER MOLD ──────────────────────────────────────────────────
    // Mold component ID → how many ingots worth of fluid (1000mb each) it needs
    public static final Map<String, Integer> INGOT_COSTS = new HashMap<>();

    // ── TIME SCALE PER INGOT COUNT ────────────────────────────────────────────
    // How many ingots → time multiplier applied to base setting time
    public static final Map<Integer, Float> TIME_SCALE = new HashMap<>();

    static {
        // ── TOOL HEADS (3 ingots) ─────────────────────────────────────────────
        INGOT_COSTS.put("pickaxe_head",       3);
        INGOT_COSTS.put("axe_head",           3);
        INGOT_COSTS.put("shovel_head",        3);
        INGOT_COSTS.put("katana_blade_head",  3);

        // ── BLADES (2 ingots) ─────────────────────────────────────────────────
        INGOT_COSTS.put("broadsword_blade_head", 2);

        // ── RODS / HILTS (2 ingots) ───────────────────────────────────────────
        INGOT_COSTS.put("tool_rod",           2);
        INGOT_COSTS.put("shovel_tool_rod",    2);
        INGOT_COSTS.put("katana_hilt_rod",    2);
        INGOT_COSTS.put("broadsword_hilt_rod",2);

        // ── SMALL PARTS (1 ingot) ─────────────────────────────────────────────
        INGOT_COSTS.put("core",               1);
        INGOT_COSTS.put("dagger_blade_head",  1);
        INGOT_COSTS.put("dagger_hilt_rod",    1);

        // ── INGOT MOLD (1 ingot) ──────────────────────────────────────────────
        INGOT_COSTS.put("ingot",              1);

        // ── TIME SCALE BY INGOT COUNT ─────────────────────────────────────────
        // Base time = (material.meltTime * 1.5f) / 2f seconds
        // Then multiply by this scale based on ingot count
        TIME_SCALE.put(1, 0.5f);   // 1 ingot → base time (≈1.5s for copper)
        TIME_SCALE.put(2, 0.75f);  // 2 ingots → ~2.5s for copper
        TIME_SCALE.put(3, 1f);   // 3 ingots → ~3.0s for copper
    }

    /**
     * Get the ingot cost for a given mold component ID.
     * Returns 1 as default if not found.
     */
    public static int getIngotCost(String moldId) {
        return INGOT_COSTS.getOrDefault(moldId, 1);
    }

    /**
     * Get the fluid amount needed in mb for a given mold.
     * 1 ingot = 1000mb.
     */
    public static int getFluidCost(String moldId) {
        return getIngotCost(moldId) * 1000;
    }

    /**
     * Calculate setting time in ticks for a given material and mold.
     * Formula: (material.meltTime * 1.5f) / 2f * timeScale * 20 ticks/sec
     */
    public static int getSettingTicks(AlloyMaterial material, String moldId) {
        int ingotCount = getIngotCost(moldId);
        float timeScale = TIME_SCALE.getOrDefault(ingotCount, 1.0f);
        float baseSeconds = (material.meltTime * 0.4f) / 2f;  // Changed from 1.2f to 0.6f
        float totalSeconds = baseSeconds * timeScale;
        return Math.max(20, (int) (totalSeconds * 20)); // minimum 1 second
    }
}
