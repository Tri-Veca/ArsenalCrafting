package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity;
import com.arsenalcrafting.mod.power.PowerTier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Tiered power storage block.
 * Capacity and transfer rate scale with PowerTier.
 * Bar color: green (full) → yellow → red (empty).
 */
public class PowerBankBlock extends BaseEntityBlock {

    private final PowerTier tier;

    public PowerBankBlock(BlockBehaviour.Properties properties, PowerTier tier) {
        super(properties);
        this.tier = tier;
    }

    public PowerTier getTier() { return tier; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PowerBankBlockEntity(tier, pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide) return null;
        return (lvl, pos, st, be) -> {
            if (be instanceof PowerBankBlockEntity bank) {
                bank.tickServer();
            }
        };
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }
}