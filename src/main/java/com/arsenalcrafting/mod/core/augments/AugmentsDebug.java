package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.config.ArsenalDebug;

public final class AugmentsDebug {
    private AugmentsDebug() {}

    public static void install(String tag, String message) {
        ArsenalDebug.log("AugmentInstall/" + tag, ArsenalDebug.AUGMENTS, message);
    }

    public static void process(String tag, String message) {
        ArsenalDebug.log("AugmentProcess/" + tag, ArsenalDebug.AUGMENTS, message);
    }

    public static void combo(String tag, String message) {
        ArsenalDebug.log("AugmentCombo/" + tag, ArsenalDebug.AUGMENTS, message);
    }
}
