package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import com.arsenalcrafting.mod.blocks.BlockDebug;

/**
 * The active growth source — replaces iron ore when chromium is used on it (see ChromiumItem).
 * Mirrors vanilla's BuddingAmethystBlock.randomTick exactly: each tick, pick a random adjacent
 * face; if it's empty, start a small bud there; if it already holds one of our bud stages
 * (facing back at this block), advance it to the next stage.
 *
 * Per current design: growth additionally requires this block to have direct sky access
 * ("stay lit") — flagged as something we may revisit later.
 */
public class RutileBuddingBlock extends Block {

    private static final Direction[] DIRECTIONS = Direction.values();
    // Same 1-in-5 rate vanilla budding amethyst uses. Flagged UNDECIDED for balancing.
    private static final int GROWTH_ODDS = 5;

    public RutileBuddingBlock(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }


    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(GROWTH_ODDS) != 0) return;

        BlockDebug.tick("RutileBudding", "randomTick fired at " + pos);

        if (!level.isDay()) {
            BlockDebug.tick("RutileBudding", "FAILED: isDay returned false at " + pos);
            return;
        }

        BlockDebug.tick("RutileBudding", "isDay passed at " + pos);

        Direction direction = DIRECTIONS[random.nextInt(DIRECTIONS.length)];
        BlockPos neighborPos = pos.relative(direction);
        BlockState neighborState = level.getBlockState(neighborPos);

        BlockDebug.tick("RutileBudding", "Direction: " + direction + ", Neighbor: " + neighborPos + ", NeighborState: " + neighborState);

        Block nextBlock = null;

        if (canBudStartAt(neighborState)) {
            nextBlock = ModBlocks.SMALL_RUTILE_BUD.get();
            BlockDebug.tick("RutileBudding", "canBudStartAt returned true, placing SMALL bud");
        } else if (neighborState.is(ModBlocks.SMALL_RUTILE_BUD.get())
                && neighborState.getValue(AbstractRutileBudBlock.FACING) == direction) {
            nextBlock = ModBlocks.MEDIUM_RUTILE_BUD.get();
            BlockDebug.tick("RutileBudding", "Found SMALL bud facing " + direction + ", advancing to MEDIUM");
        } else if (neighborState.is(ModBlocks.MEDIUM_RUTILE_BUD.get())
                && neighborState.getValue(AbstractRutileBudBlock.FACING) == direction) {
            nextBlock = ModBlocks.LARGE_RUTILE_BUD.get();
            BlockDebug.tick("RutileBudding", "Found MEDIUM bud facing " + direction + ", advancing to LARGE");
        } else if (neighborState.is(ModBlocks.LARGE_RUTILE_BUD.get())
                && neighborState.getValue(AbstractRutileBudBlock.FACING) == direction) {
            nextBlock = ModBlocks.RUTILE_CLUSTER.get();
            BlockDebug.tick("RutileBudding", "Found LARGE bud facing " + direction + ", advancing to CLUSTER");
        }

        if (nextBlock != null) {
            BlockState newState = nextBlock.defaultBlockState()
                    .setValue(AbstractRutileBudBlock.FACING, direction)
                    .setValue(AbstractRutileBudBlock.WATERLOGGED,
                            neighborState.getFluidState().getType() == Fluids.WATER);
            level.setBlockAndUpdate(neighborPos, newState);
            BlockDebug.tick("RutileBudding", "PLACED: " + nextBlock + " at " + neighborPos + " facing " + direction);
        } else {
            BlockDebug.tick("RutileBudding", "No growth action taken (nextBlock was null)");
        }
    }

    /**
     * Whether a brand-new bud can start growing into this space — matches vanilla's
     * canClusterGrowAtState: must be air, or a fluid-source-free replaceable spot.
     */
    private boolean canBudStartAt(BlockState state) {
        return state.isAir() || state.getFluidState().is(Fluids.WATER) && state.getFluidState().isSource();
    }
}