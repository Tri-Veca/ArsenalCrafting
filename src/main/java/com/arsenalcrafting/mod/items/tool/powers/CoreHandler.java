package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

public interface CoreHandler {
    // What tool types this augment works with
    default boolean supportsToolType(ArsenalToolType type) {
        return true; // Default: works with all tools
    }

    // ============= BLOCK INTERACTIONS =============
    default boolean canHandleBlock(BlockState state, ItemStack tool) {
        return false;
    }

    default boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                     BlockPos pos, LivingEntity miner) {
        return false;
    }

    default boolean handleBlockPlace(ItemStack tool, Level level, BlockPos pos,
                                     Player player) {
        return false;
    }

    default float modifyMiningSpeed(ItemStack tool, float baseSpeed) {
        return baseSpeed;
    }

    default float modifyMaxDurability(ItemStack tool, float currentMax) {
        return currentMax;
    }

    // ── WEAPON STAT MODIFIERS ─────────────────────────────────────────────────
    // Override in weapon cores (Gold: +dmg/-dur, Steel: +dmg vs armored, etc.)

    default float modifyAttackDamage(ItemStack tool, float baseDamage) {
        return baseDamage;
    }

    default float modifyAttackSpeed(ItemStack tool, float baseSpeed) {
        return baseSpeed;
    }

    default void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
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
     * Called when the weapon hits a living entity.
     * Runs before crit is rolled. Use for on-hit cores (bleed, elemental, etc.)
     */
    default void onEntityHit(ItemStack weapon, LivingEntity attacker,
                             Entity target, float damage) {
    }

    /**
     * Called when the weapon kills a living entity.
     * Use for on-kill cores (Gambler drop table, etc.)
     */
    default void onEntityKill(ItemStack weapon, LivingEntity attacker,
                              LivingEntity killed) {
    }

    // ============= TOOL USAGE =============
    default void onToolUpdate(ItemStack tool, Player player) {
    }

    default void onToolEquip(ItemStack tool, Player player) {
    }

    default void onToolUnequip(ItemStack tool, Player player) {
    }

    // ============= POST-PROCESSING =============
    default void postProcess(ItemStack tool, Level level, BlockPos pos) {
    }
}