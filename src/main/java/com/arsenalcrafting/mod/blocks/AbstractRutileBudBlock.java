package com.arsenalcrafting.mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Shared base for the four rutile bud/cluster stages (small/medium/large/cluster).
 * Mirrors vanilla's AmethystClusterBlock: these stages are "dumb" — they hold facing,
 * waterlogging, and shape, but do NOT drive their own growth. Growth is driven entirely
 * by RutileBuddingBlock checking its own faces, same as BuddingAmethystBlock does for
 * vanilla amethyst.
 */
public abstract class AbstractRutileBudBlock extends Block implements SimpleWaterloggedBlock {

    // Full 6-directional property (not HorizontalDirectionalBlock's 4-way one) — buds can
    // attach to any face of the budding block, including straight up or down.
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    protected AbstractRutileBudBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.UP)
                .setValue(WATERLOGGED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, WATERLOGGED);
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos attachedTo = pos.relative(state.getValue(FACING).getOpposite());
        return level.getBlockState(attachedTo).isFaceSturdy(level, attachedTo, state.getValue(FACING));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return defaultBlockState()
                .setValue(FACING, context.getClickedFace())
                .setValue(WATERLOGGED, fluidState.getType() == Fluids.WATER);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED)
                ? Fluids.WATER.getSource(false)
                : super.getFluidState(state);
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType pathComputationType) {
        return false;
    }

    // Direction-specific shape — placeholder sizing based on amethyst's own bud shapes.
    // Replace once your rutile model exists.
    protected abstract VoxelShape getShapeForDirection(Direction direction);

    @Override
    public VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
                               BlockPos pos, CollisionContext context) {
        return getShapeForDirection(state.getValue(FACING));
    }
}