package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;

public class CasingBlock extends Block {

    public static final DirectionProperty FACING = DirectionProperty.create("facing", Direction.Plane.HORIZONTAL);
    public static final BooleanProperty DRAIN = BooleanProperty.create("drain");

    public CasingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(DRAIN, false));

    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block changedBlock, BlockPos changedPos, boolean isMoving) {
        super.neighborChanged(state, level, pos, changedBlock, changedPos, isMoving);
        if (level.isClientSide) return;
        notifyController(level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING,DRAIN);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            notifyController(level, pos);
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    private void notifyController(Level level, BlockPos pos) {
        // Search neighbors for controller
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = pos.relative(dir);
            if (level.getBlockEntity(neighbor) instanceof AlloySmelterBlockEntity controller) {
                controller.markNeedsRecheck();
                return;
            }
        }
        // If not adjacent, try to find controller via SmelteryFormation
        // For now direct neighbor check is sufficient for wall adjacency
    }
}