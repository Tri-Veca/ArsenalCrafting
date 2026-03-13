package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.GeneratorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.jetbrains.annotations.Nullable;

public abstract class BaseGeneratorBlock extends BaseEntityBlock {

    protected BaseGeneratorBlock(Properties properties) {
        super(properties);
    }

    @Nullable
    @Override
    public abstract BlockEntity newBlockEntity(BlockPos pos, BlockState state);

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type
    ) {

        if (level.isClientSide) return null;

        return (lvl, pos, st, be) -> {

            if (be instanceof GeneratorBlockEntity generator) {
                generator.tickServer();
            }

        };
    }
}