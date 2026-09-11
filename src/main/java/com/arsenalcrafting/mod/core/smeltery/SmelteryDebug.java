package com.arsenalcrafting.mod.core.smeltery;

import net.minecraft.core.BlockPos;

// One switch for all smeltery debug output, plus lightweight self-checks
// that run once and print PASS/FAIL — a cheap substitute for real unit
// tests until this project has a test framework set up.
public final class SmelteryDebug {
    public static boolean DEBUG_ENABLED = true; // set false before release

    private SmelteryDebug() {}

    public static void log(String tag, String message) {
        if (DEBUG_ENABLED) {
            System.out.println("[Smeltery/" + tag + "] " + message);
        }
    }

    // Call this once anywhere early (e.g. mod constructor) to sanity-check
    // the pure membership functions before relying on them in-game.
    public static void runSelfChecks() {
        SmelteryData valid = new SmelteryData(
                BlockPos.ZERO, new BlockPos(0, 65, 0),
                3, 3, 2, true, false, SmelteryTier.COPPER, null);
        SmelteryData invalid = SmelteryData.invalid(BlockPos.ZERO);

        check("isInsideBounds/interior", valid.isInsideBounds(new BlockPos(1, 65, 1)));
        check("isInsideBounds/wall-is-false", !valid.isInsideBounds(new BlockPos(-1, 65, 1)));
        check("isInsideBounds/invalid-is-false", !invalid.isInsideBounds(new BlockPos(0, 65, 0)));

        check("isOnWallShell/wall-face", SmelteryFormation.isOnWallShell(valid, new BlockPos(1, 65, -1)));
        check("isOnWallShell/interior-is-false", !SmelteryFormation.isOnWallShell(valid, new BlockPos(1, 65, 1)));
        check("isOnWallShell/invalid-is-false", !SmelteryFormation.isOnWallShell(invalid, new BlockPos(0, 65, -1)));
        check("isOnWallShell/out-of-height-is-false", !SmelteryFormation.isOnWallShell(valid, new BlockPos(1, 100, -1)));

        log("SelfCheck", "All smeltery self-checks completed — see PASS/FAIL above.");
    }

    private static void check(String name, boolean condition) {
        System.out.println("[Smeltery/SelfCheck] " + (condition ? "PASS " : "FAIL ") + name);
    }
}