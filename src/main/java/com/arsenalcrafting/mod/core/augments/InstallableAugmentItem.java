package com.arsenalcrafting.mod.core.augments;

import net.minecraft.world.item.ItemStack;

public interface InstallableAugmentItem {
    /**
     * Apply this augment to the given tool.
     * This is where you set the right ModComponents on the tool.
     */
    void installOnTool(ItemStack tool);
}