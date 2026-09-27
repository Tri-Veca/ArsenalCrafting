package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class SmallRutileBudBlock extends AbstractRutileBudBlock {

    public SmallRutileBudBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShapeForDirection(Direction direction) {
        return switch (direction) {
            case UP -> Shapes.box(0.375, 0.0, 0.375, 0.625, 0.1875, 0.625);
            case DOWN -> Shapes.box(0.375, 0.8125, 0.375, 0.625, 1.0, 0.625);
            case NORTH -> Shapes.box(0.375, 0.375, 0.8125, 0.625, 0.625, 1.0);
            case SOUTH -> Shapes.box(0.375, 0.375, 0.0, 0.625, 0.625, 0.1875);
            case EAST -> Shapes.box(0.0, 0.375, 0.375, 0.1875, 0.625, 0.625);
            case WEST -> Shapes.box(0.8125, 0.375, 0.375, 1.0, 0.625, 0.625);
        };
    }
}