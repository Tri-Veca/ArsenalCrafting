package com.arsenalcrafting.mod.blocks;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/** Stage 4 of 4 (final) — mineable, drops raw rutile via loot table. */
public class RutileClusterBlock extends AbstractRutileBudBlock {

    public RutileClusterBlock(Properties properties) {
        super(properties);
    }


    @Override
    protected VoxelShape getShapeForDirection(Direction direction) {
        return switch (direction) {
            case UP -> Shapes.box(0.25, 0.0, 0.25, 0.75, 0.4375, 0.75);
            case DOWN -> Shapes.box(0.25, 0.5625, 0.25, 0.75, 1.0, 0.75);
            case NORTH -> Shapes.box(0.25, 0.25, 0.5625, 0.75, 0.75, 1.0);
            case SOUTH -> Shapes.box(0.25, 0.25, 0.0, 0.75, 0.75, 0.4375);
            case EAST -> Shapes.box(0.0, 0.25, 0.25, 0.4375, 0.75, 0.75);
            case WEST -> Shapes.box(0.5625, 0.25, 0.25, 1.0, 0.75, 0.75);
        };
    }
}