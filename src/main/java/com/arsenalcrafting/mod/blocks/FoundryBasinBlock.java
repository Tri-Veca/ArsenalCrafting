package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.FoundryBasinBlockEntity;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class FoundryBasinBlock extends BaseEntityBlock {
    public static final MapCodec<FoundryBasinBlock> CODEC = simpleCodec(FoundryBasinBlock::new);

    public FoundryBasinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FoundryBasinBlockEntity(pos, state);
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
        return createTickerHelper(type, ModBlockEntities.FOUNDRY_BASIN.get(),
                (level1, pos, state1, be) -> FoundryBasinBlockEntity.tick(level1, pos, state1, be));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, net.minecraft.world.InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryBasinBlockEntity basin)) return ItemInteractionResult.FAIL;

        if (basin.isReady()) {
            ItemStack result = basin.collectOutput();
            if (!result.isEmpty()) {
                if (!player.addItem(result)) player.drop(result, false);
                player.displayClientMessage(Component.literal("Collected " + result.getHoverName().getString()), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof FoundryBasinBlockEntity basin)) return InteractionResult.PASS;

        if (basin.isReady()) {
            ItemStack output = basin.collectOutput();
            if (!output.isEmpty()) {
                if (!player.addItem(output)) player.drop(output, false);
                player.displayClientMessage(Component.literal("Block cast complete!"), true);
            }
            return InteractionResult.SUCCESS;
        }

        if (basin.isSetting()) {
            int pct = basin.getSettingTime() > 0
                    ? (basin.getSettingProgress() * 100 / basin.getSettingTime()) : 0;
            player.displayClientMessage(Component.literal("Setting... " + pct + "%"), true);
            return InteractionResult.SUCCESS;
        }

        if (basin.getFluidAmount() == 0) {
            player.displayClientMessage(
                    Component.literal("Pour 9000mb of fluid using a valve faucet to cast a block"), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }
}