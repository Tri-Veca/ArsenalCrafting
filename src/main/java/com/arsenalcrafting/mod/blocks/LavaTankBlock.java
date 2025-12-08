package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
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
        // We still have a normal block model for the frame.
        return RenderShape.MODEL;
    }

    // --- BlockEntity ---

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LavaTankBlockEntity(pos, state);
    }

    // --- Right-click interaction with buckets ---

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

        ItemStack held = player.getItemInHand(hand);

        // Fill tank with lava bucket
        if (held.is(Items.LAVA_BUCKET)) {
            if (!tank.isFull()) {
                tank.addFluid(1000);

                if (!player.isCreative()) {
                    held.shrink(1);
                    ItemStack emptyBucket = new ItemStack(Items.BUCKET);
                    if (!player.getInventory().add(emptyBucket)) {
                        player.drop(emptyBucket, false);
                    }
                }
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        // Take lava out with empty bucket
        if (held.is(Items.BUCKET)) {
            if (!tank.isEmpty()) {
                tank.removeFluid(1000);

                if (!player.isCreative()) {
                    held.shrink(1);
                    ItemStack lavaBucket = new ItemStack(Items.LAVA_BUCKET);
                    if (!player.getInventory().add(lavaBucket)) {
                        player.drop(lavaBucket, false);
                    }
                }
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        }

        return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
    }
}
