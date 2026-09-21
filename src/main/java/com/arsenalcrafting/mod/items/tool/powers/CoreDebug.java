package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.config.ArsenalDebug;

public final class CoreDebug {
    private CoreDebug() {}

    public static void resolution(String tag, String message) {
        ArsenalDebug.log("CoreResolution/" + tag, ArsenalDebug.CORE_POWERS, message);
    }

    public static void proc(String tag, String message) {
        ArsenalDebug.log("CoreProc/" + tag, ArsenalDebug.CORE_POWERS, message);
    }
}