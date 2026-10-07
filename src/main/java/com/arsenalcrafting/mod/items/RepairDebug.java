package com.arsenalcrafting.mod.items;

import com.arsenalcrafting.mod.config.ArsenalDebug;

/**
 * Debug logging for the repair kit. Toggle via ArsenalDebug config.
 * NOTE: calc() currently has no callers — can be removed later.
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
