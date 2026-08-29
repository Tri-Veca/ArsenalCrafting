package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.core.smeltery.SmelteryData;
import com.arsenalcrafting.mod.core.smeltery.SmelteryTier;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class AlloySmelterBlock extends BaseEntityBlock {
    public static final MapCodec<AlloySmelterBlock> CODEC = simpleCodec(AlloySmelterBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final EnumProperty<SmelterTier> TIER = EnumProperty.create("tier", SmelterTier.class);

    public enum SmelterTier implements StringRepresentable {
        UNFORMED("unformed"),
        COPPER("copper"),
        IRON("iron"),
        STEEL("steel"),
        TITANIUM("titanium");

        private final String name;
        SmelterTier(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }

        public static SmelterTier fromSmelteryTier(SmelteryTier tier) {
            return switch (tier) {
                case COPPER -> COPPER;
                case IRON -> IRON;
                case STEEL -> STEEL;
                case TITANIUM -> TITANIUM;
            };
        }
    }

    public AlloySmelterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(ACTIVE, false)
                .setValue(TIER, SmelterTier.UNFORMED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVE, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(ACTIVE, false)
                .setValue(TIER, SmelterTier.UNFORMED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new AlloySmelterBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos,
                         BlockState newState, boolean movedByPiston) {
        super.onRemove(state, level, pos, newState, movedByPiston);
        notifyNearbyControllers(level, pos);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos,
                        BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        notifyNearbyControllers(level, pos);
    }

    private void notifyNearbyControllers(Level level, BlockPos pos) {
        for (BlockPos search : BlockPos.betweenClosed(
                pos.offset(-6, -6, -6), pos.offset(6, 6, 6))) {
            if (level.getBlockEntity(search) instanceof AlloySmelterBlockEntity be) {
                be.markNeedsRecheck();
            }
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof AlloySmelterBlockEntity controller)) return InteractionResult.PASS;

        controller.validateIfNeeded();

        if (controller.isFormed()) {
            player.openMenu(controller, buf -> buf.writeBlockPos(pos));
        } else {
            SmelteryData data = controller.getSmelteryData();
            String reason = (data != null && data.failReason() != null)
                    ? data.failReason()
                    : "§cSmeltery not formed!";
            player.displayClientMessage(Component.literal(reason), true);
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos,
                                Block neighborBlock, BlockPos neighborPos, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (level.getBlockEntity(pos) instanceof AlloySmelterBlockEntity be) {
            be.markNeedsRecheck();
        }
    }

    @Override
    public void onNeighborChange(BlockState state, LevelReader level, BlockPos pos, BlockPos neighbor) {
        super.onNeighborChange(state, level, pos, neighbor);
        if (level.getBlockEntity(pos) instanceof AlloySmelterBlockEntity be) {
            be.markNeedsRecheck();
        }
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.ALLOY_SMELTER.get(),
                (level1, pos, state1, blockEntity) ->
                        AlloySmelterBlockEntity.tick(level1, pos, state1, blockEntity));
    }
}