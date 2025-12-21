package com.arsenalcrafting.mod.items.tool.powers;

import net.minecraft.world.level.block.state.BlockState;

public final class WorkHorse {
    private WorkHorse() {}

    private static final float HARDNESS_THRESHOLD = 10.0f;

    public static boolean isHardBlock(BlockState state) {
        // Check if block hardness is >= 10
        float hardness = state.getDestroySpeed(null, null);
        return hardness >= HARDNESS_THRESHOLD;
    }

    public static String getDescription() {
        return "Instantly breaks hard blocks (≥" + (int)HARDNESS_THRESHOLD + " hardness)";
    }
}