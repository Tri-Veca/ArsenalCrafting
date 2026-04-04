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
    // Adjusts setting time based on how many ingots the mold needs
    // Bigger items take longer to cool and solidify
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
        // These multipliers make larger items take longer to cast:
        // - 1 ingot items (small parts) → 50% of base time (fastest)
        // - 2 ingot items (rods, blades) → 75% of base time (medium)
        // - 3 ingot items (tool heads) → 100% of base time (slowest)
        TIME_SCALE.put(1, 0.5f);   // Small parts: half the base time
        TIME_SCALE.put(2, 0.75f);  // Medium parts: three-quarters of base time
        TIME_SCALE.put(3, 1.0f);   // Large parts: full base time
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
     *
     * HOW IT WORKS (simple explanation):
     * =================================
     * 1. Each material has a "meltTime" (how long it takes to melt in the alloy smelter)
     *    - Gold melts fast (10 ticks = 0.5 seconds)
     *    - Iron melts slower (60 ticks = 3 seconds)
     *    - Diamond melts even slower (70 ticks = 3.5 seconds)
     *
     * 2. We take that meltTime and multiply by 0.4, then divide by 2
     *    This gives us a "base time" in seconds
     *    Example: Gold (10 meltTime) → (10 × 0.4) ÷ 2 = 2 seconds base time
     *
     * 3. Then we multiply by the TIME_SCALE based on how many ingots the mold needs
     *    - 1 ingot (small part): ×0.5 → Gold takes 1 second to cast
     *    - 2 ingots (rod): ×0.75 → Gold takes 1.5 seconds
     *    - 3 ingots (tool head): ×1.0 → Gold takes 2 seconds
     *
     * 4. Finally, convert seconds to ticks (20 ticks = 1 second)
     *    And enforce a minimum of 20 ticks (1 second) so casting isn't instant
     *
     * @param material The material being cast (Gold, Iron, Diamond, etc.)
     * @param moldId The mold being used (pickaxe_head, ingot, tool_rod, etc.)
     * @return Setting time in ticks (20 ticks = 1 second)
     */
    public static int getSettingTicks(AlloyMaterial material, String moldId) {
        int ingotCount = getIngotCost(moldId);
        float timeScale = TIME_SCALE.getOrDefault(ingotCount, 1.0f);

        // Base time calculation: (meltTime × 0.4) ÷ 2
        // The 0.4 multiplier makes casting times reasonable (not too long)
        float baseSeconds = (material.meltTime * 0.4f) / 2f;

        // Apply time scale based on item size
        float totalSeconds = baseSeconds * timeScale;

        // Convert seconds to ticks (20 ticks per second), minimum 20 ticks (1 second)
        return Math.max(20, (int) (totalSeconds * 20));
    }
}