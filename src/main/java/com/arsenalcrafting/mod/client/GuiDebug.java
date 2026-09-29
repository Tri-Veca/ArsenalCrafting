package com.arsenalcrafting.mod.client;

import com.arsenalcrafting.mod.config.ArsenalDebug;

/**
 * Thin debug facade for all menu / screen related logging.
 * On/off control lives in {@link ArsenalDebug}.
 */
public final class GuiDebug {
    private GuiDebug() {}

    public static void open(String tag, String message) {
        ArsenalDebug.log("GuiOpen/" + tag, ArsenalDebug.GUI_OPEN, message);
    }

    public static void slots(String tag, String message) {
        ArsenalDebug.log("GuiSlots/" + tag, ArsenalDebug.GUI_SLOTS, message);
    }

    public static void packets(String tag, String message) {
        ArsenalDebug.log("GuiPackets/" + tag, ArsenalDebug.GUI_PACKETS, message);
    }

    public static void sync(String tag, String message) {
        ArsenalDebug.log("GuiSync/" + tag, ArsenalDebug.GUI_SYNC, message);
    }

    public static void render(String tag, String message) {
        ArsenalDebug.log("GuiRender/" + tag, ArsenalDebug.REPAIR_KIT_SCREEN, message);
    }
}