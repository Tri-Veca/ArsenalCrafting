package com.arsenalcrafting.mod.component;

import com.mojang.serialization.Codec;

/**
 * Simple boolean component marking an Arsenal tool as broken.
 * When true: tool does nothing on use, shows "⚠ BROKEN!" in tooltip.
 *
 * Stored as a boolean via Codec.BOOL — presence alone is enough
 * but a value lets us be explicit.
 */
public record BrokenState(boolean broken) {

    public static final BrokenState BROKEN   = new BrokenState(true);
    public static final BrokenState REPAIRED = new BrokenState(false);

    public static final Codec<BrokenState> CODEC =
            Codec.BOOL.xmap(BrokenState::new, BrokenState::broken);

    public boolean isBroken() { return broken; }
}