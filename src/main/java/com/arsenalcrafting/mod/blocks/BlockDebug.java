package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.config.ArsenalDebug;

/**
 * Thin debug facade for blocks.
 * On/off control lives in {@link ArsenalDebug}.
 */
public final class BlockDebug {
    private BlockDebug() {}

    public static void tick(String tag, String message) {
        ArsenalDebug.log("BlockTick/" + tag, ArsenalDebug.BLOCK_TICK, message);
    }

    public static void interact(String tag, String message) {
        ArsenalDebug.log("BlockInteract/" + tag, ArsenalDebug.BLOCK_INTERACT, message);
    }

    public static void place(String tag, String message) {
        ArsenalDebug.log("BlockPlace/" + tag, ArsenalDebug.BLOCK_PLACE, message);
    }

    public static void breakBlock(String tag, String message) {
        ArsenalDebug.log("BlockBreak/" + tag, ArsenalDebug.BLOCK_BREAK, message);
    }
}
