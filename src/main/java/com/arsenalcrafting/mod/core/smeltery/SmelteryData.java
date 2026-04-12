package com.arsenalcrafting.mod.core.smeltery;

import net.minecraft.core.BlockPos;
import org.checkerframework.checker.nullness.qual.Nullable;

public record SmelteryData(
        BlockPos controllerPos,
        BlockPos interiorOrigin,
        int width,
        int depth,
        int height,
        boolean isValid,
        boolean needsRecheck,
        SmelteryTier tier,
        @Nullable String failReason
){


    public static SmelteryData invalid(BlockPos controllerPos) {
        return new SmelteryData(controllerPos, BlockPos.ZERO, 0, 0, 0,
                false, false, SmelteryTier.COPPER, null);
    }

    public static SmelteryData invalidWithReason(BlockPos controllerPos, String reason) {
        return new SmelteryData(controllerPos, BlockPos.ZERO, 0, 0, 0,
                false, false, SmelteryTier.COPPER, reason);
    }

    public SmelteryData withNeedsRecheck() {
        return new SmelteryData(controllerPos, interiorOrigin, width, depth,
                height, isValid, true, tier, failReason);
    }

    public int volume() {
        return width * depth * height;
    }

    public int getTotalSlots() {
        return tier.getTotalSlots(height);
    }

    public int getMaxLavaTanks() {
        return tier.getMaxLavaTanks(height);
    }

    public boolean isInsideBounds(BlockPos pos) {
        if (!isValid) return false;
        return pos.getX() >= interiorOrigin.getX() && pos.getX() < interiorOrigin.getX() + width &&
                pos.getY() >= interiorOrigin.getY() && pos.getY() < interiorOrigin.getY() + height &&
                pos.getZ() >= interiorOrigin.getZ() && pos.getZ() < interiorOrigin.getZ() + depth;
    }
}