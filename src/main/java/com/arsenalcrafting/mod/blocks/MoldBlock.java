package com.arsenalcrafting.mod.blocks;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;

public class MoldBlock extends Block {

    private static final VoxelShape SHAPE = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 1.0D, 16.0D);

    public MoldBlock() {
        super(BlockBehaviour.Properties.ofFullCopy(Blocks.GOLD_BLOCK)
                .sound(SoundType.METAL)
                .noOcclusion()
                .noCollission()
                .isRedstoneConductor((state, getter, pos) -> false) // Important for transparency
        );
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE; // Or return Shapes.empty() if you want no collision
    }

    @Override
    public boolean useShapeForLightOcclusion(BlockState state) {
        return true; // Allows light to pass through
    }
}