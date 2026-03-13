package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.GeneratorBlockEntity;
import com.arsenalcrafting.mod.power.GeneratorType;
import com.arsenalcrafting.mod.power.PowerTier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Solar Generator — produces FE during daylight when sky is visible.
 * No fuel slot. No output in rain/thunder. Priority 2 generator type.
 */
public class SolarGeneratorBlock extends BaseGeneratorBlock {

    private final PowerTier tier;

    public SolarGeneratorBlock(Properties props, PowerTier tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(
                GeneratorType.SOLAR, tier, pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }
}