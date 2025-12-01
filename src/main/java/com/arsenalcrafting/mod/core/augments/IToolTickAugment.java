package com.arsenalcrafting.mod.core.augments;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

/**
 * Augment for passive logic that executes while the tool is in the inventory or held (e.g., Auto-Repair, Magnetism).
 */
public interface IToolTickAugment extends Augment {
    /**
     * Executes the augment logic periodically, usually on the inventory tick.
     *
     * @param toolStack The tool ItemStack.
     * @param level The Level (World) where the entity exists.
     * @param entity The entity holding or possessing the tool.
     * @param slot The slot index where the tool resides.
     * @param isSelected True if the tool is currently in the main hand or off-hand.
     */
    void onInventoryTick(@NotNull ItemStack toolStack, @NotNull Level level, @NotNull Entity entity, int slot, boolean isSelected);
}