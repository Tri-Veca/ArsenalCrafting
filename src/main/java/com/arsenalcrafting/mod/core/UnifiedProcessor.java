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
        // 1. Augments handle (Auto-Smelt, Fortune, etc.)
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
}