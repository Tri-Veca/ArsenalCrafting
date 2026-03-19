package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

public class LavaTankBlock extends BaseEntityBlock {

    public static final MapCodec<LavaTankBlock> CODEC = simpleCodec(LavaTankBlock::new);

    public LavaTankBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LavaTankBlockEntity(pos, state);
    }

    @Override
    public ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult
    ) {
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }

        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof LavaTankBlockEntity tank)) {
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        // Use NeoForge's FluidUtil for proper fluid handling
        boolean interacted = FluidUtil.interactWithFluidHandler(player, hand, tank.fluidTank);

        if (interacted) {
            // Force a block update to sync with client
            level.sendBlockUpdated(pos, state, state, 3);
            return ItemInteractionResult.CONSUME;
        }

        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }

    // Optional: Override to handle shift-click behavior
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        if (!state.is(newState.getBlock())) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof LavaTankBlockEntity tank) {
                // Optional: Drop fluid as bucket when block breaks?
                // You could implement this if you want

                //TODO: Look at this
            }
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}