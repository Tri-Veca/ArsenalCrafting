package com.arsenalcrafting.mod.core.augments;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

/**
 * Augment for logic that executes when a block is successfully mined (e.g., Auto-Smelt, Fortune).
 */
public interface IBlockMiningAugment extends Augment {
    /**
     * Executes the augment logic after a block is broken.
     * @param toolStack The tool ItemStack used for mining.
     * @param level The Level (World) where the mining occurred.
     * @param state The BlockState of the mined block.
     * @param pos The BlockPos of the mined block.
     * @param entity The LivingEntity (usually the player) mining the block.
     * @return True if the augment successfully handled the drops/block logic.
     */
    boolean onBlockMined(@NotNull ItemStack toolStack, @NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity entity);
}