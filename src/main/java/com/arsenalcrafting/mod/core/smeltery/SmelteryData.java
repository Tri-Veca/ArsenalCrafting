package com.arsenalcrafting.mod.core.smeltery;

import net.minecraft.core.BlockPos;

public record SmelteryData(
        BlockPos controllerPos,
        BlockPos interiorOrigin,
        int width,
        int depth,
        int height,
        boolean isValid,
        boolean needsRecheck
) {
    public static SmelteryData invalid(BlockPos controllerPos) {
        return new SmelteryData(controllerPos, BlockPos.ZERO, 0, 0, 0, false, false);
    }

    public SmelteryData withNeedsRecheck() {
        return new SmelteryData(controllerPos, interiorOrigin, width, depth, height, isValid, true);
    }

    /** Interior volume — used to scale smeltery capacity */
    public int volume() {
        return width * depth * height;
    }

    /** Check if a world position falls within the interior bounds */
    public boolean isInsideBounds(BlockPos pos) {
        if (!isValid) return false;
        return pos.getX() >= interiorOrigin.getX() && pos.getX() < interiorOrigin.getX() + width &&
                pos.getY() >= interiorOrigin.getY() && pos.getY() < interiorOrigin.getY() + height &&
                pos.getZ() >= interiorOrigin.getZ() && pos.getZ() < interiorOrigin.getZ() + depth;
    }
}