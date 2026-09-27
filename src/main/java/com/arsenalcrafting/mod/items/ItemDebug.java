package com.arsenalcrafting.mod.items;

import com.arsenalcrafting.mod.config.ArsenalDebug;

/**
 * Thin debug facade for items.
 * On/off control lives in {@link ArsenalDebug}.
 */
public final class ItemDebug {
    private ItemDebug() {}

    public static void use(String tag, String message) {
        ArsenalDebug.log("ItemUse/" + tag, ArsenalDebug.ITEM_USE, message);
    }

    public static void craft(String tag, String message) {
        ArsenalDebug.log("ItemCraft/" + tag, ArsenalDebug.ITEM_CRAFT, message);
    }

    public static void drop(String tag, String message) {
        ArsenalDebug.log("ItemDrop/" + tag, ArsenalDebug.ITEM_DROP, message);
    }
}
