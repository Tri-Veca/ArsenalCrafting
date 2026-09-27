package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

public class LargerRutileBudBlock extends AbstractRutileBudBlock {

    public LargerRutileBudBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected VoxelShape getShapeForDirection(Direction direction) {
        return switch (direction) {
            case UP -> Shapes.box(0.3125, 0.0, 0.3125, 0.6875, 0.3125, 0.6875);
            case DOWN -> Shapes.box(0.3125, 0.6875, 0.3125, 0.6875, 1.0, 0.6875);
            case NORTH -> Shapes.box(0.3125, 0.3125, 0.6875, 0.6875, 0.6875, 1.0);
            case SOUTH -> Shapes.box(0.3125, 0.3125, 0.0, 0.6875, 0.6875, 0.3125);
            case EAST -> Shapes.box(0.0, 0.3125, 0.3125, 0.3125, 0.6875, 0.6875);
            case WEST -> Shapes.box(0.6875, 0.3125, 0.3125, 1.0, 0.6875, 0.6875);
        };
    }
}