package com.arsenalcrafting.mod.core.smeltery;

import com.arsenalcrafting.mod.blocks.AlloySmelterBlock;
import com.arsenalcrafting.mod.blocks.CasingBlock;
import com.arsenalcrafting.mod.blocks.LavaTankBlock;
import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class SmelteryFormation {

    public static boolean isValidWallBlock(BlockState state) {
        return state.getBlock() instanceof CasingBlock
                || state.getBlock() instanceof AlloySmelterBlock
                || state.getBlock() instanceof LavaTankBlock;
    }

    public static boolean isValidFloorBlock(BlockState state) {
        return state.getBlock() instanceof CasingBlock;
    }

    public static boolean isValidInteriorBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) return true;
        return level.getBlockEntity(pos) instanceof LavaTankBlockEntity;
    }

    public static SmelteryData validate(Level level, BlockPos controllerPos) {
        int controllerY = controllerPos.getY();
        int floorY = controllerY - 1;

        System.out.println("[Smeltery] === Starting validation ===");
        System.out.println("[Smeltery] Controller at: " + controllerPos);
        System.out.println("[Smeltery] Controller block: " + level.getBlockState(controllerPos).getBlock());

        // Phase 1: Flood fill to find all connected wall blocks at controllerY
        Set<BlockPos> wallBlocks = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();
        queue.add(controllerPos);
        wallBlocks.add(controllerPos);

        int minX = controllerPos.getX();
        int maxX = controllerPos.getX();
        int minZ = controllerPos.getZ();
        int maxZ = controllerPos.getZ();

        while (!queue.isEmpty()) {
            BlockPos current = queue.poll();
            for (Direction dir : new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockPos neighbor = new BlockPos(current.getX() + dir.getStepX(), controllerY, current.getZ() + dir.getStepZ());
                if (wallBlocks.contains(neighbor)) continue;
                BlockState ns = level.getBlockState(neighbor);
                if (isValidWallBlock(ns)) {
                    wallBlocks.add(neighbor);
                    queue.add(neighbor);
                    minX = Math.min(minX, neighbor.getX());
                    maxX = Math.max(maxX, neighbor.getX());
                    minZ = Math.min(minZ, neighbor.getZ());
                    maxZ = Math.max(maxZ, neighbor.getZ());
                }
            }
        }

        int outerWidth = maxX - minX + 1;
        int outerDepth = maxZ - minZ + 1;

        System.out.println("[Smeltery] Found bounds: X=" + minX + " to " + maxX + " Z=" + minZ + " to " + maxZ);
        System.out.println("[Smeltery] Outer size: " + outerWidth + "x" + outerDepth);

        if (outerWidth < 3 || outerDepth < 3) {
            System.out.println("[Smeltery] FAIL: Too small (need 3x3 minimum)");
            return SmelteryData.invalid(controllerPos);
        }

        int interiorMinX = minX + 1;
        int interiorMaxX = maxX - 1;
        int interiorMinZ = minZ + 1;
        int interiorMaxZ = maxZ - 1;
        int interiorWidth = interiorMaxX - interiorMinX + 1;
        int interiorDepth = interiorMaxZ - interiorMinZ + 1;

        System.out.println("[Smeltery] Interior bounds: X=" + interiorMinX + " to " + interiorMaxX +
                " Z=" + interiorMinZ + " to " + interiorMaxZ);

        // Phase 2: Validate interior floor only
        System.out.println("[Smeltery] Checking interior floor at Y=" + floorY);
        for (int x = interiorMinX; x <= interiorMaxX; x++) {
            for (int z = interiorMinZ; z <= interiorMaxZ; z++) {
                BlockState fs = level.getBlockState(new BlockPos(x, floorY, z));
                if (!isValidFloorBlock(fs)) {
                    System.out.println("[Smeltery] FAIL: Floor block at " +
                            x + "," + floorY + "," + z + " is " + fs.getBlock());
                    return SmelteryData.invalid(controllerPos);
                }
            }
        }
        System.out.println("[Smeltery] Floor check passed");

        // Phase 2 continued: Validate interior at controllerY
        System.out.println("[Smeltery] Checking interior at Y=" + controllerY);
        for (int x = interiorMinX; x <= interiorMaxX; x++) {
            for (int z = interiorMinZ; z <= interiorMaxZ; z++) {
                BlockPos iPos = new BlockPos(x, controllerY, z);
                if (!isValidInteriorBlock(level, iPos)) {
                    BlockState is = level.getBlockState(iPos);
                    System.out.println("[Smeltery] FAIL: Interior block at " +
                            x + "," + controllerY + "," + z + " is " + is.getBlock());
                    return SmelteryData.invalid(controllerPos);
                }
            }
        }
        System.out.println("[Smeltery] Interior check passed");

        // Validate walls at controllerY
        System.out.println("[Smeltery] Checking walls at Y=" + controllerY);
        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                boolean isInterior = x > minX && x < maxX && z > minZ && z < maxZ;
                if (!isInterior) {
                    BlockPos wPos = new BlockPos(x, controllerY, z);
                    BlockState ws = level.getBlockState(wPos);
                    if (!isValidWallBlock(ws)) {
                        System.out.println("[Smeltery] FAIL: Wall block at " +
                                x + "," + controllerY + "," + z + " is " + ws.getBlock());
                        return SmelteryData.invalid(controllerPos);
                    }
                }
            }
        }
        System.out.println("[Smeltery] Wall check passed");

        // Phase 3: Scan upward to find roof
        System.out.println("[Smeltery] Scanning upward from Y=" + (controllerY + 1));
        int roofY = controllerY;
        int maxHeight = 11;

        outer:
        for (int y = controllerY + 1; y <= controllerY + maxHeight; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean isInterior = x > minX && x < maxX && z > minZ && z < maxZ;
                    if (isInterior) {
                        if (!isValidInteriorBlock(level, new BlockPos(x, y, z))) {
                            System.out.println("[Smeltery] Vertical scan stopped at Y=" + y +
                                    " interior block: " + level.getBlockState(new BlockPos(x, y, z)).getBlock());
                            break outer;
                        }
                    } else {
                        if (!isValidWallBlock(level.getBlockState(new BlockPos(x, y, z)))) {
                            System.out.println("[Smeltery] Vertical scan stopped at Y=" + y +
                                    " wall block: " + level.getBlockState(new BlockPos(x, y, z)).getBlock());
                            break outer;
                        }
                    }
                }
            }
            roofY = y;
        }

        int interiorHeight = roofY - floorY;
        System.out.println("[Smeltery] RoofY=" + roofY + " FloorY=" + floorY +
                " InteriorHeight=" + interiorHeight);

        if (interiorHeight < 1) {
            System.out.println("[Smeltery] FAIL: Not tall enough (need height >= 1, got " + interiorHeight + ")");
            return SmelteryData.invalid(controllerPos);
        }
        // Controller must be in wall
        boolean controllerInWall =
                controllerPos.getX() == minX || controllerPos.getX() == maxX ||
                        controllerPos.getZ() == minZ || controllerPos.getZ() == maxZ;

        System.out.println("[Smeltery] Controller in wall: " + controllerInWall);

        if (!controllerInWall) {
            System.out.println("[Smeltery] FAIL: Controller not in wall");
            return SmelteryData.invalid(controllerPos);
        }

        BlockPos interiorOrigin = new BlockPos(interiorMinX, floorY + 1, interiorMinZ);

        System.out.println("[Smeltery] SUCCESS: " + interiorWidth + "x" + interiorDepth +
                "x" + interiorHeight + " at " + interiorOrigin);

        return new SmelteryData(
                controllerPos,
                interiorOrigin,
                interiorWidth,
                interiorDepth,
                interiorHeight,
                true,
                false
        );
    }
}