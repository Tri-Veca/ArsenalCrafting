package com.arsenalcrafting.mod.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Power cable — NO BlockEntity, no ticking.
 * Connection state is stored in blockstate properties for rendering.
 * Network logic lives entirely in PowerNetworkSavedData.
 *
 * Shape: thin center core (4x4x4) + arms that extend toward connected neighbors.
 */
public class CableBlock extends Block {

    // One boolean per face — true if connected to an energy neighbor
    public static final BooleanProperty NORTH = BooleanProperty.create("north");
    public static final BooleanProperty SOUTH = BooleanProperty.create("south");
    public static final BooleanProperty EAST  = BooleanProperty.create("east");
    public static final BooleanProperty WEST  = BooleanProperty.create("west");
    public static final BooleanProperty UP    = BooleanProperty.create("up");
    public static final BooleanProperty DOWN  = BooleanProperty.create("down");

    // Core cube in the center
    private static final VoxelShape CORE = Block.box(6, 6, 6, 10, 10, 10);

    // Arms per direction
    private static final VoxelShape ARM_NORTH = Block.box(6, 6, 0,  10, 10, 6);
    private static final VoxelShape ARM_SOUTH = Block.box(6, 6, 10, 10, 10, 16);
    private static final VoxelShape ARM_WEST  = Block.box(0, 6, 6,  6,  10, 10);
    private static final VoxelShape ARM_EAST  = Block.box(10, 6, 6, 16, 10, 10);
    private static final VoxelShape ARM_DOWN  = Block.box(6, 0, 6,  10, 6,  10);
    private static final VoxelShape ARM_UP    = Block.box(6, 10, 6, 10, 16, 10);

    public CableBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
                .setValue(NORTH, false).setValue(SOUTH, false)
                .setValue(EAST,  false).setValue(WEST,  false)
                .setValue(UP,    false).setValue(DOWN,  false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, SOUTH, EAST, WEST, UP, DOWN);
    }

    // ── CONNECTIONS ───────────────────────────────────────────────────────────

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return connectionState(defaultBlockState(), ctx.getLevel(), ctx.getClickedPos());
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return connectionState(state, level, pos);
    }

    private BlockState connectionState(BlockState state, LevelAccessor level, BlockPos pos) {
        return state
                .setValue(NORTH, canConnect(level, pos, Direction.NORTH))
                .setValue(SOUTH, canConnect(level, pos, Direction.SOUTH))
                .setValue(EAST,  canConnect(level, pos, Direction.EAST))
                .setValue(WEST,  canConnect(level, pos, Direction.WEST))
                .setValue(UP,    canConnect(level, pos, Direction.UP))
                .setValue(DOWN,  canConnect(level, pos, Direction.DOWN));
    }

    /**
     * Connect to other cables or blocks that expose IEnergyStorage.
     */
    private boolean canConnect(LevelAccessor level, BlockPos pos, Direction dir) {
        BlockPos neighbor = pos.relative(dir);
        BlockState neighborState = level.getBlockState(neighbor);

        // Always connect to other cables
        if (neighborState.getBlock() instanceof CableBlock) return true;

        // Connect to anything with an energy capability on the facing side
        if (level instanceof net.minecraft.world.level.Level l) {
            var storage = l.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.BLOCK,
                    neighbor, neighborState,
                    l.getBlockEntity(neighbor), dir.getOpposite());
            return storage != null;
        }

        return false;
    }

    // ── SHAPE ─────────────────────────────────────────────────────────────────

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext ctx) {
        VoxelShape shape = CORE;
        if (Boolean.TRUE.equals(state.getValue(NORTH))) shape = Shapes.or(shape, ARM_NORTH);
        if (Boolean.TRUE.equals(state.getValue(SOUTH))) shape = Shapes.or(shape, ARM_SOUTH);
        if (Boolean.TRUE.equals(state.getValue(EAST)))  shape = Shapes.or(shape, ARM_EAST);
        if (Boolean.TRUE.equals(state.getValue(WEST)))  shape = Shapes.or(shape, ARM_WEST);
        if (Boolean.TRUE.equals(state.getValue(UP)))    shape = Shapes.or(shape, ARM_UP);
        if (Boolean.TRUE.equals(state.getValue(DOWN)))  shape = Shapes.or(shape, ARM_DOWN);
        return shape;
    }

    // Cables shouldn't occlude neighbors
    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return false;
    }
}