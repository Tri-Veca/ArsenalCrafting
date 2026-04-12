package com.arsenalcrafting.mod.core.smeltery;

import com.arsenalcrafting.mod.blocks.*;
import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class SmelteryFormation {

    // ── TIER DETECTION ────────────────────────────────────────────────────────

    public static SmelteryTier getTierForBlock(Block block) {
        if (block == ModBlocks.COPPER_CASING.get()) return SmelteryTier.COPPER;
        if (block == ModBlocks.IRON_CASING.get()) return SmelteryTier.IRON;
        if (block == ModBlocks.BRONZE_CASING.get()) return SmelteryTier.IRON;
        if (block == ModBlocks.STEEL_CASING.get()) return SmelteryTier.STEEL;
        if (block == ModBlocks.TITANIUM_CASING.get()) return SmelteryTier.TITANIUM;
        return null;
    }

    public static boolean isValidWallBlock(BlockState state) {
        return getTierForBlock(state.getBlock()) != null
                || state.getBlock() instanceof AlloySmelterBlock
                || state.getBlock() instanceof LavaTankBlock;
    }

    public static boolean isValidFloorBlock(BlockState state) {
        return getTierForBlock(state.getBlock()) != null;
    }

    public static boolean isValidInteriorBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        return level.getBlockEntity(pos) instanceof LavaTankBlockEntity;
    }

    private static boolean isCorner(int x, int z, int minX, int maxX, int minZ, int maxZ) {
        return (x == minX || x == maxX) && (z == minZ || z == maxZ);
    }

    // ── MAIN ENTRY POINT ─────────────────────────────────────────────────────

    public static SmelteryData validate(Level level, BlockPos controllerPos) {
        int controllerY = controllerPos.getY();
        int floorY = controllerY - 1;

        System.out.println("[Smeltery] === Starting validation ===");
        System.out.println("[Smeltery] Controller at: " + controllerPos);

        int MAX_SIZE = 11;

        // Phase 1: Walk along controller's own wall in both axes
        int minZ = controllerPos.getZ();
        int maxZ = controllerPos.getZ();
        int minX = controllerPos.getX();
        int maxX = controllerPos.getX();

        while (minZ > controllerPos.getZ() - MAX_SIZE &&
                isValidWallBlock(level.getBlockState(new BlockPos(controllerPos.getX(), controllerY, minZ - 1)))) {
            minZ--;
        }
        while (maxZ < controllerPos.getZ() + MAX_SIZE &&
                isValidWallBlock(level.getBlockState(new BlockPos(controllerPos.getX(), controllerY, maxZ + 1)))) {
            maxZ++;
        }
        while (minX > controllerPos.getX() - MAX_SIZE &&
                isValidWallBlock(level.getBlockState(new BlockPos(minX - 1, controllerY, controllerPos.getZ())))) {
            minX--;
        }
        while (maxX < controllerPos.getX() + MAX_SIZE &&
                isValidWallBlock(level.getBlockState(new BlockPos(maxX + 1, controllerY, controllerPos.getZ())))) {
            maxX++;
        }

        // Phase 2: Use wall endpoints to find perpendicular walls
        boolean controllerOnZWall = (maxZ - minZ) >= (maxX - minX);

        if (controllerOnZWall) {
            // Walk X from minZ row
            int fromMinZ_minX = controllerPos.getX();
            int fromMinZ_maxX = controllerPos.getX();
            while (fromMinZ_minX > controllerPos.getX() - MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(fromMinZ_minX - 1, controllerY, minZ)))) {
                fromMinZ_minX--;
            }
            while (fromMinZ_maxX < controllerPos.getX() + MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(fromMinZ_maxX + 1, controllerY, minZ)))) {
                fromMinZ_maxX++;
            }
            // Walk X from maxZ row
            int fromMaxZ_minX = controllerPos.getX();
            int fromMaxZ_maxX = controllerPos.getX();
            while (fromMaxZ_minX > controllerPos.getX() - MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(fromMaxZ_minX - 1, controllerY, maxZ)))) {
                fromMaxZ_minX--;
            }
            while (fromMaxZ_maxX < controllerPos.getX() + MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(fromMaxZ_maxX + 1, controllerY, maxZ)))) {
                fromMaxZ_maxX++;
            }
            minX = Math.min(fromMinZ_minX, fromMaxZ_minX);
            maxX = Math.max(fromMinZ_maxX, fromMaxZ_maxX);
        } else {
            // Walk Z from minX column
            int fromMinX_minZ = controllerPos.getZ();
            int fromMinX_maxZ = controllerPos.getZ();
            while (fromMinX_minZ > controllerPos.getZ() - MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(minX, controllerY, fromMinX_minZ - 1)))) {
                fromMinX_minZ--;
            }
            while (fromMinX_maxZ < controllerPos.getZ() + MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(minX, controllerY, fromMinX_maxZ + 1)))) {
                fromMinX_maxZ++;
            }
            // Walk Z from maxX column
            int fromMaxX_minZ = controllerPos.getZ();
            int fromMaxX_maxZ = controllerPos.getZ();
            while (fromMaxX_minZ > controllerPos.getZ() - MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(maxX, controllerY, fromMaxX_minZ - 1)))) {
                fromMaxX_minZ--;
            }
            while (fromMaxX_maxZ < controllerPos.getZ() + MAX_SIZE &&
                    isValidWallBlock(level.getBlockState(new BlockPos(maxX, controllerY, fromMaxX_maxZ + 1)))) {
                fromMaxX_maxZ++;
            }
            minZ = Math.min(fromMinX_minZ, fromMaxX_minZ);
            maxZ = Math.max(fromMinX_maxZ, fromMaxX_maxZ);
        }

        int outerWidth = maxX - minX + 1;
        int outerDepth = maxZ - minZ + 1;

        System.out.println("[Smeltery] Found bounds: X=" + minX + " to " + maxX +
                " Z=" + minZ + " to " + maxZ);
        System.out.println("[Smeltery] Outer size: " + outerWidth + "x" + outerDepth);

        if (outerWidth < 3 || outerDepth < 3) {
            System.out.println("[Smeltery] FAIL: Too small");
            return SmelteryData.invalid(controllerPos);
        }

        int interiorMinX = minX + 1;
        int interiorMaxX = maxX - 1;
        int interiorMinZ = minZ + 1;
        int interiorMaxZ = maxZ - 1;
        int interiorWidth = interiorMaxX - interiorMinX + 1;
        int interiorDepth = interiorMaxZ - interiorMinZ + 1;

        // ── TIER DETECTION (skip corners) ────────────────────────────────────
        SmelteryTier detectedTier = null;
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean interior = x > minX && x < maxX && z > minZ && z < maxZ;
                boolean corner = isCorner(x, z, minX, maxX, minZ, maxZ);
                // Skip corners and interior for tier detection
                if (interior || corner) continue;

                BlockState bs = level.getBlockState(new BlockPos(x, controllerY, z));
                SmelteryTier blockTier = getTierForBlock(bs.getBlock());
                if (blockTier == null) continue;

                if (detectedTier == null) {
                    detectedTier = blockTier;
                } else if (detectedTier != blockTier) {
                    System.out.println("[Smeltery] FAIL: Mixed casing tiers detected");
                    return SmelteryData.invalid(controllerPos);
                }
            }
        }

        if (detectedTier == null) {
            System.out.println("[Smeltery] FAIL: No casing blocks found");
            return SmelteryData.invalid(controllerPos);
        }

        System.out.println("[Smeltery] Detected tier: " + detectedTier.getDisplayName());

        // ── FLOOR CHECK (corners optional) ───────────────────────────────────
        System.out.println("[Smeltery] Checking interior floor at Y=" + floorY);
        for (int x = interiorMinX; x <= interiorMaxX; x++) {
            for (int z = interiorMinZ; z <= interiorMaxZ; z++) {
                BlockState fs = level.getBlockState(new BlockPos(x, floorY, z));
                if (!isValidFloorBlock(fs)) {
                    System.out.println("[Smeltery] FAIL: Floor block at " +
                            x + "," + floorY + "," + z + " is " + fs.getBlock());
                    return SmelteryData.invalid(controllerPos);
                }
                SmelteryTier floorTier = getTierForBlock(fs.getBlock());
                if (floorTier != detectedTier) {
                    System.out.println("[Smeltery] FAIL: Floor tier mismatch at " +
                            x + "," + floorY + "," + z);
                    return SmelteryData.invalid(controllerPos);
                }
            }
        }
        System.out.println("[Smeltery] Floor check passed");

        // ── INTERIOR CHECK AT CONTROLLER Y ───────────────────────────────────
        for (int x = interiorMinX; x <= interiorMaxX; x++) {
            for (int z = interiorMinZ; z <= interiorMaxZ; z++) {
                BlockPos iPos = new BlockPos(x, controllerY, z);
                if (!isValidInteriorBlock(level, iPos)) {
                    System.out.println("[Smeltery] FAIL: Interior block at " +
                            x + "," + controllerY + "," + z + " is " +
                            level.getBlockState(iPos).getBlock());
                    return SmelteryData.invalid(controllerPos);
                }
            }
        }

        // ── WALL CHECK AT CONTROLLER Y (corners optional) ─────────────────────
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean interior = x > minX && x < maxX && z > minZ && z < maxZ;
                boolean corner = isCorner(x, z, minX, maxX, minZ, maxZ);
                // Skip interior and corners (corners are optional)
                if (interior || corner) continue;

                BlockPos wPos = new BlockPos(x, controllerY, z);
                BlockState ws = level.getBlockState(wPos);
                if (!isValidWallBlock(ws)) {
                    System.out.println("[Smeltery] FAIL: Wall block at " +
                            x + "," + controllerY + "," + z + " is " + ws.getBlock());
                    return SmelteryData.invalid(controllerPos);
                }
            }
        }
        System.out.println("[Smeltery] Wall check passed (corners optional)");

        // ── VERTICAL SCAN (corners optional) ─────────────────────────────────────
        int roofY = controllerY;
        int maxHeight = detectedTier.getMaxHeight();

        outer:
        for (int y = controllerY + 1; y <= controllerY + maxHeight; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean interior = x > minX && x < maxX && z > minZ && z < maxZ;
                    boolean corner = isCorner(x, z, minX, maxX, minZ, maxZ);

                    if (interior) {
                        if (!isValidInteriorBlock(level, new BlockPos(x, y, z))) {
                            System.out.println("[Smeltery] Vertical scan stopped at Y=" + y);
                            break outer;
                        }
                    } else if (!corner) {
                        // Only check non-corner wall positions
                        BlockState ws = level.getBlockState(new BlockPos(x, y, z));
                        if (!isValidWallBlock(ws)) {
                            System.out.println("[Smeltery] Vertical scan stopped at Y=" + y +
                                    " wall: " + ws.getBlock());
                            break outer;
                        }
                        SmelteryTier upperTier = getTierForBlock(ws.getBlock());
                        if (upperTier != null && upperTier != detectedTier) {
                            System.out.println("[Smeltery] FAIL: Mixed tier in upper wall at Y=" + y);
                            return SmelteryData.invalid(controllerPos);
                        }
                    }
                    // Corners are completely skipped - no check needed
                }
            }
            roofY = y;
        }

        int interiorHeight = roofY - floorY;
        System.out.println("[Smeltery] RoofY=" + roofY + " InteriorHeight=" + interiorHeight);

        if (interiorHeight < 1) {
            System.out.println("[Smeltery] FAIL: Not tall enough");
            return SmelteryData.invalid(controllerPos);
        }

        boolean exceededMaxHeight = interiorHeight > detectedTier.getMaxHeight();
        if (exceededMaxHeight) {
            System.out.println("[Smeltery] FAIL: Exceeds max height for " +
                    detectedTier.getDisplayName() + " tier (" + detectedTier.getMaxHeight() + ")");
            return SmelteryData.invalidWithReason(controllerPos,
                    "§e" + detectedTier.getDisplayName() +
                            " Tier supports a max wall height of " + detectedTier.getMaxHeight());
        }

        boolean controllerInWall =
                controllerPos.getX() == minX || controllerPos.getX() == maxX ||
                        controllerPos.getZ() == minZ || controllerPos.getZ() == maxZ;

        if (!controllerInWall) {
            System.out.println("[Smeltery] FAIL: Controller not in wall");
            return SmelteryData.invalid(controllerPos);
        }

        int lavaTankCount = countLavaTanks(level, minX, maxX, minZ, maxZ, floorY, roofY);
        int maxLavaTanks = detectedTier.getMaxLavaTanks(interiorHeight);
        if (lavaTankCount > maxLavaTanks) {
            System.out.println("[Smeltery] FAIL: Too many lava tanks (" + lavaTankCount +
                    " max " + maxLavaTanks + ")");
            return SmelteryData.invalid(controllerPos);
        }

        BlockPos interiorOrigin = new BlockPos(interiorMinX, floorY + 1, interiorMinZ);

        System.out.println("[Smeltery] SUCCESS: Tier=" + detectedTier.getDisplayName() +
                " " + interiorWidth + "x" + interiorDepth + "x" + interiorHeight);

        return new SmelteryData(
                controllerPos,
                interiorOrigin,
                interiorWidth,
                interiorDepth,
                interiorHeight,
                true,
                false,
                detectedTier,
                null
        );
    }

    private static int countLavaTanks(Level level, int minX, int maxX,
                                      int minZ, int maxZ, int floorY, int roofY) {
        int count = 0;
        for (int y = floorY; y <= roofY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof LavaTankBlockEntity) {
                        count++;
                    }
                }
            }
        }
        return count;
    }
}