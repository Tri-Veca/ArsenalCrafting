package com.arsenalcrafting.mod.core.smeltery;

import com.arsenalcrafting.mod.config.ArsenalDebug;
import net.minecraft.core.BlockPos;

/**
 * Thin per-feature debug facade for the smeltery.
 * All on/off control lives in {@link ArsenalDebug}.
 */
public final class SmelteryDebug {
    private SmelteryDebug() {}

    public static void formation(String tag, String message) {
        ArsenalDebug.log("SmelteryFormation/" + tag, ArsenalDebug.SMELTERY_FORMATION, message);
    }

    public static void lava(String tag, String message) {
        ArsenalDebug.log("SmelteryLava/" + tag, ArsenalDebug.SMELTERY_LAVA, message);
    }

    public static void liquid(String tag, String message) {
        ArsenalDebug.log("SmelteryLiquid/" + tag, ArsenalDebug.SMELTERY_LIQUID, message);
    }

    public static void settings(String tag, String message) {
        ArsenalDebug.log("SmelterySettings/" + tag, ArsenalDebug.SMELTERY_SETTINGS, message);
    }

    public static void redstone(String tag, String message) {
        ArsenalDebug.log("SmelteryRedstone/" + tag, ArsenalDebug.SMELTERY_REDSTONE, message);
    }

    public static void faucetPouring(String tag, String message) {
        ArsenalDebug.log("FaucetPouring/" + tag, ArsenalDebug.FAUCET_POURING, message);
    }

    public static void faucetCasting(String tag, String message) {
        ArsenalDebug.log("FaucetCasting/" + tag, ArsenalDebug.FAUCET_CASTING, message);
    }

    // ── Self-checks (kept for now) ──────────────────────────────────────────

    /** Call once early (e.g. mod constructor) to sanity-check pure membership helpers. */
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

        formation("SelfCheck", "All smeltery self-checks completed — see PASS/FAIL above.");
    }

    private static void check(String name, boolean condition) {
        // Self-check output is always on when GLOBAL is enabled (independent of category flags)
        if (ArsenalDebug.GLOBAL_ENABLED) {
            System.out.println("[Smeltery/SelfCheck] " + (condition ? "PASS " : "FAIL ") + name);
        }
    }
}