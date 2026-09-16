package com.arsenalcrafting.mod.config;

public final class ArsenalDebug {
    private ArsenalDebug() {}

    public static boolean GLOBAL_ENABLED = true;

    // ── SMELTERY ──────────────────────────────────────────────────────────
    public static boolean SMELTERY_FORMATION = true;
    public static boolean SMELTERY_LAVA = true;
    public static boolean SMELTERY_LIQUID = true;
    public static boolean SMELTERY_SETTINGS = false;
    public static boolean SMELTERY_REDSTONE = false;

    // ── FAUCET ────────────────────────────────────────────────────────────
    public static boolean FAUCET_POURING = true;
    public static boolean FAUCET_CASTING = true;

    // ── COMBAT ────────────────────────────────────────────────────────────
    public static boolean WEAPON_DAMAGE = true;
    public static boolean WEAPON_ABILITIES = true;

    // ── TOOLS ─────────────────────────────────────────────────────────────
    public static boolean TOOL_MINING = false;
    public static boolean TOOL_INTERACT = false;

    // ── CORE POWERS ───────────────────────────────────────────────────────
    public static boolean CORE_POWERS = true;

    // ── AUGMENTS ──────────────────────────────────────────────────────────
    public static boolean AUGMENTS = false;

    // ── POWER SYSTEM ──────────────────────────────────────────────────────
    public static boolean POWER_GENERATION = true;
    public static boolean POWER_NETWORK = true;
    public static boolean POWER_STORAGE = true;

    public static void log(String category, boolean enabled, String message) {
        if (GLOBAL_ENABLED && enabled) {
            System.out.println("[" + category + "] " + message);
        }
    }
}
