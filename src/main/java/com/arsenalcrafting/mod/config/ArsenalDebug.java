package com.arsenalcrafting.mod.config;

public final class ArsenalDebug {
    private ArsenalDebug() {}
    // set to false to silence all debug
    // set to true to let each category decide
    public static boolean GLOBAL_ENABLED = true;

    // ── SMELTERY ──────────────────────────────────────────────────────────
    public static boolean SMELTERY_FORMATION = false;
    public static boolean SMELTERY_LAVA = false;
    public static boolean SMELTERY_LIQUID = true;
    public static boolean SMELTERY_SETTINGS = false;
    public static boolean SMELTERY_REDSTONE = false;

    // ── FAUCET ────────────────────────────────────────────────────────────
    public static boolean FAUCET_POURING = true;
    public static boolean FAUCET_CASTING = true;


    // ── GUI / MENUS ───────────────────────────────────────────────────────
    public static boolean GUI_OPEN   = true;
    public static boolean GUI_SLOTS  = true;
    public static boolean GUI_PACKETS = true;
    public static boolean GUI_SYNC   = true;

    // ── COMBAT ────────────────────────────────────────────────────────────
    public static boolean WEAPON_DAMAGE = true;
    public static boolean WEAPON_ABILITIES = true;

    // ── TOOLS ─────────────────────────────────────────────────────────────
    public static boolean TOOL_MINING = false;
    public static boolean TOOL_INTERACT = false;

    // ── CORE POWERS ───────────────────────────────────────────────────────
    public static boolean CORE_POWERS = true;

    // ── AUGMENTS ──────────────────────────────────────────────────────────
    public static boolean AUGMENTS = true;

    // ── POWER SYSTEM ──────────────────────────────────────────────────────
    public static boolean POWER_GENERATION = true;
    public static boolean POWER_NETWORK = true;
    public static boolean POWER_STORAGE = true;

    // ── ITEMS ─────────────────────────────────────────────────────────────
    public static boolean ITEM_USE = true;
    public static boolean ITEM_CRAFT = false;
    public static boolean ITEM_DROP = false;

    // ── BLOCKS ────────────────────────────────────────────────────────────
    public static boolean BLOCK_TICK = false;
    public static boolean BLOCK_INTERACT = false;
    public static boolean BLOCK_PLACE = false;
    public static boolean BLOCK_BREAK = false;

    // ── REPAIR KIT ────────────────────────────────────────────────────────
    public static boolean REPAIR_KIT_REPAIR = true;
    public static boolean REPAIR_KIT_CALC = true;
    public static boolean REPAIR_KIT_SCREEN = true;

    public static void log(String category, boolean enabled, String message) {
        if (GLOBAL_ENABLED && enabled) {
            System.out.println("[" + category + "] " + message);
        }
    }
}
