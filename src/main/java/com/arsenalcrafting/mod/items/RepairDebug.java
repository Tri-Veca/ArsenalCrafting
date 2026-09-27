package com.arsenalcrafting.mod.items;

import com.arsenalcrafting.mod.config.ArsenalDebug;

/**
 * Thin debug facade for repair kit system.
 * On/off control lives in {@link ArsenalDebug}.
 */
public final class RepairDebug {
    private RepairDebug() {}

    public static void repair(String tag, String message) {
        ArsenalDebug.log("RepairKitRepair/" + tag, ArsenalDebug.REPAIR_KIT_REPAIR, message);
    }

    public static void calc(String tag, String message) {
        ArsenalDebug.log("RepairKitCalc/" + tag, ArsenalDebug.REPAIR_KIT_CALC, message);
    }
}
