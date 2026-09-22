package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.ValveFaucetBlockEntity;
import com.arsenalcrafting.mod.core.smeltery.SmelteryDebug;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class ValveFaucetBlock extends BaseEntityBlock {
    public static final MapCodec<ValveFaucetBlock> CODEC = simpleCodec(ValveFaucetBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
    public ValveFaucetBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ValveFaucetBlockEntity(pos, state);
    }


    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState().setValue(FACING, context.getHorizontalDirection());

        BlockPos faucetPos = context.getClickedPos();
        BlockPos casingPos = faucetPos.relative(context.getClickedFace().getOpposite());
        BlockState casingState = context.getLevel().getBlockState(casingPos);

        SmelteryDebug.faucetCasting("Place", "==== FAUCET DEBUG ====");
        SmelteryDebug.faucetCasting("Place", "faucetPos: " + faucetPos);
        SmelteryDebug.faucetCasting("Place", "casingPos: " + casingPos);
        SmelteryDebug.faucetCasting("Place", "casingState: " + casingState);

        if (casingState.getBlock() instanceof CasingBlock) {
            SmelteryDebug.faucetCasting("Place", "Setting drain=true and facing=" + context.getClickedFace());
            context.getLevel().setBlock(casingPos, casingState
                    .setValue(CasingBlock.FACING, context.getClickedFace())
                    .setValue(CasingBlock.DRAIN, true), 3);
        }

        return state;
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            BlockPos casingPos = pos.relative(state.getValue(FACING));
            BlockState casingState = level.getBlockState(casingPos);
            if (casingState.getBlock() instanceof CasingBlock && casingState.getValue(CasingBlock.DRAIN)) {
                level.setBlock(casingPos, casingState
                        .setValue(CasingBlock.FACING, Direction.NORTH)  // Reset facing
                        .setValue(CasingBlock.DRAIN, false), 3);
            }
            super.onRemove(state, level, pos, newState, moved);
        }
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.VALVE_FAUCET.get(),
                (level1, pos, state1, be) -> ValveFaucetBlockEntity.tick(level1, pos, state1, be));
    }
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING)) {
            case NORTH -> net.minecraft.world.phys.shapes.Shapes.box(0.376, 0.309, 0.03, 0.624, 0.495, 0.353);
            case SOUTH -> net.minecraft.world.phys.shapes.Shapes.box(0.376, 0.309, 0.647, 0.624, 0.495, 0.97);
            case EAST ->  net.minecraft.world.phys.shapes.Shapes.box(0.647, 0.309, 0.376, 0.97, 0.495, 0.624);
            case WEST ->  net.minecraft.world.phys.shapes.Shapes.box(0.03, 0.309, 0.376, 0.353, 0.495, 0.624);
            default ->    net.minecraft.world.phys.shapes.Shapes.block();
        };
    }
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof ValveFaucetBlockEntity faucet)) return InteractionResult.PASS;

        String message = faucet.activate(level, pos);
        if (message != null) {
            player.displayClientMessage(Component.literal(message), true);
        } else {
            player.displayClientMessage(
                    Component.literal("Pouring..."), true);
        }
        return InteractionResult.SUCCESS;
    }
}