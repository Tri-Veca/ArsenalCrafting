package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;
import com.arsenalcrafting.mod.core.smeltery.SmelteryFormation;

public class LavaTankBlock extends BaseEntityBlock {

    public static final MapCodec<LavaTankBlock> CODEC = simpleCodec(LavaTankBlock::new);

    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<TankTier> TIER = EnumProperty.create("tier", TankTier.class);

    public enum TankTier implements StringRepresentable {
        UNFORMED("unformed"),
        COPPER("copper"),
        IRON("iron"),
        BRONZE("bronze"),
        STEEL("steel"),
        TITANIUM("titanium");

        private final String name;
        TankTier(String name) { this.name = name; }

        @Override
        public String getSerializedName() { return name; }
    }

    public LavaTankBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(TIER, TankTier.UNFORMED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, TIER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(FACING, context.getHorizontalDirection().getOpposite())
                .setValue(TIER, TankTier.UNFORMED);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LavaTankBlockEntity(pos, state);
    }

    @Override
    public ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level,
            BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LavaTankBlockEntity tank))
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        boolean interacted = false;
        if (stack.is(net.minecraft.world.item.Items.LAVA_BUCKET) && !tank.isFull()) {
            int capacity = tank.getCapacity();
            int current = tank.getFluidAmount();
            int space = capacity - current;
            tank.addFluid(Math.min(1000, space));
            if (!player.isCreative()) {
                stack.shrink(1);
                player.getInventory().add(new ItemStack(net.minecraft.world.item.Items.BUCKET));
            }
            interacted = true;
        } else {
            interacted = FluidUtil.interactWithFluidHandler(player, hand, tank.fluidTank);
        }
        if (interacted) {
            level.sendBlockUpdated(pos, state, state, 3);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
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
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean isMoving) {

        if (!state.is(newState.getBlock())) {
            SmelteryFormation.notifyNearbyControllers(level, pos);
        }

        super.onRemove(state, level, pos, newState, isMoving);
    }
}