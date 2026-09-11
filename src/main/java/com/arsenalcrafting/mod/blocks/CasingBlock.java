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
import com.arsenalcrafting.mod.core.smeltery.SmelteryFormation;

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
        if (changedBlock instanceof ValveFaucetBlock) return;

        SmelteryFormation.notifyNearbyControllers(level, pos);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, DRAIN);
    }


@Override
    public void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {

        super.onPlace(state, level, pos, oldState, movedByPiston);

        if (level.isClientSide) return;

        SmelteryFormation.notifyNearbyControllers(level, pos);
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            SmelteryFormation.notifyNearbyControllers(level, pos);
        }

        super.onRemove(state, level, pos, newState, movedByPiston);
    }


}
