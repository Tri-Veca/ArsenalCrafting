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
 * Bio Generator — burns organic plant/seed items → FE.
 * One fuel slot, accepts c:seeds, c:crops, saplings, leaves, etc.
 * Priority 3 generator type.
 */
public class BioGeneratorBlock extends BaseGeneratorBlock {

    private final PowerTier tier;

    public BioGeneratorBlock(Properties props, PowerTier tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(
                GeneratorType.BIO, tier, pos, state);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return null;
    }
}