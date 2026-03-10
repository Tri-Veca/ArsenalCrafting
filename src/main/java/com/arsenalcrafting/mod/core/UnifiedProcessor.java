package com.arsenalcrafting.mod.core;

import com.arsenalcrafting.mod.core.augments.AugmentProcessor;

import com.arsenalcrafting.mod.items.tool.powers.CoreProcessor;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;


public class UnifiedProcessor {

    public static boolean processBlockBreak(ItemStack tool, Level level, BlockState state,
                                            BlockPos pos, LivingEntity miner) {
        // 1. Cores handle (Momentum, etc.)
        CoreProcessor.onBlockDestroyed(tool, level, state, pos, miner);

        // 2. Augments handle (Auto-Smelt, Fortune, etc.)
        if (AugmentProcessor.processBlockBreak(tool, level, state, pos, miner)) {
            return true;
        }

        return false;
    }

    public static float getDestroySpeed(ItemStack tool, BlockState state, float baseSpeed) {
        // Apply core modifiers FIRST
        float speed = CoreProcessor.applyMiningSpeedModifiers(tool, baseSpeed, state);

        // Then augment modifiers
        speed = AugmentProcessor.applyMiningSpeedModifiers(tool, speed, state);

        return speed;
    }

    public static boolean isCorrectToolForDrops(ItemStack tool, BlockState state) {
        return CoreProcessor.checkToolCorrectness(tool, state);
    }

    public static boolean shouldSaveDurability(ItemStack tool, RandomSource random) {
        return CoreProcessor.shouldSaveDurability(tool, random);
    }

    public static int getModifiedMaxDurability(ItemStack tool, int baseDurability) {
        return (int) CoreProcessor.applyDurabilityModifiers(tool, baseDurability);
    }

    public static void processToolTick(ItemStack tool, Player player) {
        CoreProcessor.processToolTick(tool, player);
        AugmentProcessor.processToolTick(tool, player);
    }
    /**
     * Call from ArsenalTool.hurtEnemy() BEFORE rollCrit().
     * Handles core on-hit effects and augment on-hit effects (fire, freeze, etc.)
     */
    public static boolean processEntityHit(ItemStack weapon, LivingEntity attacker,
                                           Entity target, float damage) {
        // Step 1: Core on-hit effects (bleed, elemental cores, etc.)
        CoreProcessor.onEntityHit(weapon, attacker, target, damage);

        // Step 2: Augment on-hit effects (fire aspect, freeze, lightning, looting, etc.)
        if (AugmentProcessor.processEntityHit(weapon, attacker, target, damage)) {
            return true;
        }

        return false;
    }

    /**
     * Call from ArsenalTool on kill event.
     * Handles Gambler diamond core drops, looting augments, etc.
     */
    public static void processEntityKill(ItemStack weapon, LivingEntity attacker,
                                         LivingEntity killed) {
        CoreProcessor.onEntityKill(weapon, attacker, killed);
        AugmentProcessor.processEntityKill(weapon, attacker, killed);
    }
}

