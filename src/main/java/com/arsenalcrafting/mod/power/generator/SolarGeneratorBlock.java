package com.arsenalcrafting.mod.power.generator;

import com.arsenalcrafting.mod.power.PowerTier;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SolarGeneratorBlock extends BaseGeneratorBlock {

    private final PowerTier tier;

    public SolarGeneratorBlock(Properties props, PowerTier tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(pos, state, GeneratorType.SOLAR, tier);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }
}