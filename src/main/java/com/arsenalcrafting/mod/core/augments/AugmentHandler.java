package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public interface AugmentHandler {
    int priority();           // Lower = runs first
    boolean isExclusive();    // If true, blocks later handlers

    // What tool types this augment works with
    default boolean supportsToolType(ArsenalToolType type) {
        return true; // Default: works with all tools
    }

    // ============= BLOCK INTERACTIONS =============
    default boolean canHandleBlock(BlockState state, ItemStack tool) {
        return false; // Default: doesn't handle blocks
    }

    default boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                     BlockPos pos, LivingEntity miner) {
        return false; // Default: doesn't handle block breaks
    }

    default boolean handleBlockPlace(ItemStack tool, Level level, BlockPos pos,
                                     Player player) {
        return false;
    }

    // ============= ENTITY INTERACTIONS =============
    default boolean canHandleEntity(Entity target, ItemStack tool) {
        return false;
    }

    default boolean handleEntityAttack(ItemStack tool, LivingEntity attacker,
                                       Entity target, float damage) {
        return false;
    }

    default boolean handleEntityHit(ItemStack tool, LivingEntity user,
                                    Entity target, HitResult hit) {
        return false;
    }
    /**
     * Called when the weapon kills a living entity.
     * Implement in weapon augments for on-kill effects (soul harvest, looting, etc.)
     */
    default void onEntityKill(ItemStack weapon, LivingEntity attacker, LivingEntity killed) {
        // Default: do nothing
    }

    // ============= TOOL USAGE =============
    default void onToolUpdate(ItemStack tool, Player player) {
        // Called each tick while holding the tool
    }

    default void onToolEquip(ItemStack tool, Player player) {
        // When tool is equipped
    }

    default void onToolUnequip(ItemStack tool, Player player) {
        // When tool is unequipped
    }

    // ============= POST-PROCESSING =============
    default void postProcess(ItemStack tool, Level level, BlockPos pos) {
        // After block break/place
    }
}
