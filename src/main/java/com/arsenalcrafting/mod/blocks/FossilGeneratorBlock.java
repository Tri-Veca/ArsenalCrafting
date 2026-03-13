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
 * Fossil Fuel Generator — burns coal/charcoal/coal blocks → FE.
 * One fuel slot, tier-scaled output. Priority 1 generator type.
 */
public class FossilGeneratorBlock extends BaseGeneratorBlock {

    private final PowerTier tier;

    public FossilGeneratorBlock(Properties props, PowerTier tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(
                GeneratorType.FOSSIL, tier, pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // Codec not needed for block entities — returning null is fine for non-serialized blocks
        return null;
    }
}