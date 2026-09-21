package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.config.ArsenalDebug;

/**
 * Thin debug facade for tools & weapons.
 * On/off control lives in {@link ArsenalDebug}.
 */
public final class ToolsDebug {
    private ToolsDebug() {}

    public static void damage(String tag, String message) {
        ArsenalDebug.log("ToolDamage/" + tag, ArsenalDebug.WEAPON_DAMAGE, message);
    }

    public static void abilities(String tag, String message) {
        ArsenalDebug.log("ToolAbility/" + tag, ArsenalDebug.WEAPON_ABILITIES, message);
    }

    public static void mining(String tag, String message) {
        ArsenalDebug.log("ToolMining/" + tag, ArsenalDebug.TOOL_MINING, message);
    }

    public static void interact(String tag, String message) {
        ArsenalDebug.log("ToolInteract/" + tag, ArsenalDebug.TOOL_INTERACT, message);
    }
}