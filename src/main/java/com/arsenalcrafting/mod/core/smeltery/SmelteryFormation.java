package com.arsenalcrafting.mod.core.smeltery;

import com.arsenalcrafting.mod.blocks.*;
import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Optional;
import java.util.function.BiPredicate;
import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;

import java.util.*;

public class SmelteryFormation {

    // Shared search limit — every "find my structure" query in this mod uses
    // this same number, so no component can drift out of sync with another.
    public static final int MAX_SIZE = 11;

    // Default corner policy. Flip to false to let corners be anything —
    // this is the ONE place that decision lives.
    public static final boolean CORNERS_REQUIRE_CASING = false;

    // ── TIER DETECTION ────────────────────────────────────────────────────────

    public static SmelteryTier getTierForBlock(Block block) {
        if (block == ModBlocks.COPPER_CASING.get()) return SmelteryTier.COPPER;
        if (block == ModBlocks.IRON_CASING.get()) return SmelteryTier.IRON;
        if (block == ModBlocks.BRONZE_CASING.get()) return SmelteryTier.BRONZE;
        if (block == ModBlocks.STEEL_CASING.get()) return SmelteryTier.STEEL;
        if (block == ModBlocks.TITANIUM_CASING.get()) return SmelteryTier.TITANIUM;
        return null;
    }

    // Used for wall validation — faucet counts as a valid wall block
    public static boolean isValidWallBlock(BlockState state) {
        return getTierForBlock(state.getBlock()) != null
                || state.getBlock() instanceof AlloySmelterBlock
                || state.getBlock() instanceof LavaTankBlock
                || state.getBlock() instanceof ValveFaucetBlock;
    }

    // Used for boundary detection only — faucet excluded so it doesn't expand bounds
    public static boolean isValidBoundaryBlock(BlockState state) {
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

    // ── SEARCH RECTANGLE ─────────────────────────────────────────────────────
    // Holds the outer bounds found during formation scanning, plus the
    // derived interior bounds (one block in from each wall).
    private record BoundingRect(int minX, int maxX, int minZ, int maxZ) {
        int outerWidth()      { return maxX - minX + 1; }
        int outerDepth()      { return maxZ - minZ + 1; }
        int interiorMinX()    { return minX + 1; }
        int interiorMaxX()    { return maxX - 1; }
        int interiorMinZ()    { return minZ + 1; }
        int interiorMaxZ()    { return maxZ - 1; }
        int interiorWidth()   { return interiorMaxX() - interiorMinX() + 1; }
        int interiorDepth()   { return interiorMaxZ() - interiorMinZ() + 1; }
    }

    // Result of scanning upward for the roof. mixedTierFailure signals an
    // outright invalid structure (return invalid), vs. a normal stop (roof found).
    private record HeightScanResult(int roofY, boolean mixedTierFailure) {}

    // True if this cell should be skipped by horizontal checks — interior
    // always skipped, corners skipped only when cornersRequired is false.
    private static boolean isSkippableCell(int x, int z, BoundingRect rect, boolean cornersRequired) {
        boolean interior = x > rect.minX() && x < rect.maxX() && z > rect.minZ() && z < rect.maxZ();
        boolean corner = isCorner(x, z, rect.minX(), rect.maxX(), rect.minZ(), rect.maxZ());
        return interior || (!cornersRequired && corner);
    }

    // Reads top-to-bottom as a checklist: trace the rectangle, detect the
    // tier, then validate floor/interior/walls/height/tank-count in order.

    public static SmelteryData validate(
            Level level,
            BlockPos controllerPos,
            boolean cornersRequired) {

        int controllerY = controllerPos.getY();
        int floorY = controllerY - 1;

       SmelteryDebug.formation("Validate",
                "=== Starting validation for controller at " + controllerPos + " ==="
        );

        // =========================================================
        // 1. Discover the outer structure
        // =========================================================

        BoundingRect cross = traceInitialCross(level, controllerPos);
        BoundingRect rect = expandToRectangle(level, controllerPos, cross, cornersRequired);

        if (rect.outerWidth() < 3 || rect.outerDepth() < 3) {
            SmelteryDebug.formation("Validate",
                    "FAIL: structure too small"
            );
            return SmelteryData.invalid(controllerPos);
        }

        // =========================================================
        // 2. Determine the casing tier
        // =========================================================

        SmelteryTier tier = detectTier(
                level,
                rect,
                controllerY,
                cornersRequired
        );

        if (tier == null) {
            SmelteryDebug.formation("Validate",
                    "FAIL: no valid or consistent casing tier detected"
            );
            return SmelteryData.invalid(controllerPos);
        }

        // =========================================================
        // 3. Validate the structure at controller level
        // =========================================================

        // The floor must be casing of the detected tier.
        if (!validateFloor(level, rect, floorY, tier)) {
            return SmelteryData.invalid(controllerPos);
        }

        // The interior must contain only valid interior blocks.
        if (!validateInteriorAtControllerY(level, rect, controllerY)) {
            return SmelteryData.invalid(controllerPos);
        }

        // The surrounding wall must contain valid wall blocks.
        if (!validateWalls(
                level,
                rect,
                controllerPos,
                cornersRequired)) {

            return SmelteryData.invalid(controllerPos);
        }

        // =========================================================
        // 4. Determine the smeltery height
        // =========================================================

        HeightScanResult heightScan = scanVerticalHeight(
                level,
                rect,
                controllerY,
                tier
        );

        if (heightScan.mixedTierFailure()) {
            return SmelteryData.invalid(controllerPos);
        }

        int roofY = heightScan.roofY();
        int interiorHeight = roofY - floorY;

        if (interiorHeight < 1) {
            SmelteryDebug.formation("Validate",
                    "FAIL: not tall enough"
            );
            return SmelteryData.invalid(controllerPos);
        }

        if (interiorHeight > tier.getMaxHeight()) {
            return SmelteryData.invalidWithReason(
                    controllerPos,
                    "§e" + tier.getDisplayName()
                            + " Tier supports a max wall height of "
                            + tier.getMaxHeight()
            );
        }

        // =========================================================
        // 5. Validate controller position
        // =========================================================

        boolean controllerInWall =
                controllerPos.getX() == rect.minX()
                        || controllerPos.getX() == rect.maxX()
                        || controllerPos.getZ() == rect.minZ()
                        || controllerPos.getZ() == rect.maxZ();

        if (!controllerInWall) {
            SmelteryDebug.formation("Validate",
                    "FAIL: controller not in wall"
            );
            return SmelteryData.invalid(controllerPos);
        }

        // =========================================================
        // 6. Validate lava tank count
        // =========================================================

        int lavaTankCount = countLavaTanks(
                level,
                rect.minX(),
                rect.maxX(),
                rect.minZ(),
                rect.maxZ(),
                floorY,
                roofY
        );

        int maxLavaTanks = tier.getMaxLavaTanks(interiorHeight);
       SmelteryDebug.formation("Validate",
                "Lava tanks: "
                        + lavaTankCount
                        + " / "
                        + maxLavaTanks
                        + " | tier="
                        + tier.getDisplayName()
                        + " | height="
                        + interiorHeight
        );
        if (lavaTankCount > maxLavaTanks) {
            SmelteryDebug.formation("Validate",
                    "FAIL: too many lava tanks ("
                            + lavaTankCount
                            + " > "
                            + maxLavaTanks
                            + ")"
            );
            return SmelteryData.invalid(controllerPos);
        }

        // =========================================================
        // 7. Build the final smeltery data
        // =========================================================

        BlockPos interiorOrigin = new BlockPos(
                rect.interiorMinX(),
                floorY + 1,
                rect.interiorMinZ()
        );

       SmelteryDebug.formation("Validate",
                "SUCCESS: "
                        + tier.getDisplayName()
                        + " "
                        + rect.interiorWidth()
                        + "x"
                        + rect.interiorDepth()
                        + "x"
                        + interiorHeight
        );

        return new SmelteryData(
                controllerPos,
                interiorOrigin,
                rect.interiorWidth(),
                rect.interiorDepth(),
                interiorHeight,
                true,
                false,
                tier,
                null
        );
    }

    public static SmelteryData validate(Level level, BlockPos controllerPos) {
        return validate(level, controllerPos, CORNERS_REQUIRE_CASING);
    }

    // ── STEP 1: initial cross — walk outward along the controller's own axes ──
    private static BoundingRect traceInitialCross(Level level, BlockPos controllerPos) {
        int controllerY = controllerPos.getY();
        int minZ = controllerPos.getZ(), maxZ = controllerPos.getZ();
        int minX = controllerPos.getX(), maxX = controllerPos.getX();

        // North
        while (minZ > controllerPos.getZ() - MAX_SIZE) {
            int nextZ = minZ - 1;
            if (!isBoundaryOrCornerAdjacent(level, controllerPos.getX(), controllerY, nextZ, 1, 0)) break;
            minZ = nextZ;
        }
// South
        while (maxZ < controllerPos.getZ() + MAX_SIZE) {
            int nextZ = maxZ + 1;
            if (!isBoundaryOrCornerAdjacent(level, controllerPos.getX(), controllerY, nextZ, 1, 0)) break;
            maxZ = nextZ;
        }
// West
        while (minX > controllerPos.getX() - MAX_SIZE) {
            int nextX = minX - 1;
            if (!isBoundaryOrCornerAdjacent(level, nextX, controllerY, controllerPos.getZ(), 0, 1)) break;
            minX = nextX;
        }
// East
        while (maxX < controllerPos.getX() + MAX_SIZE) {
            int nextX = maxX + 1;
            if (!isBoundaryOrCornerAdjacent(level, nextX, controllerY, controllerPos.getZ(), 0, 1)) break;
            maxX = nextX;
        }

        SmelteryDebug.formation("Validate", "Initial cross: X=" + minX + ".." + maxX + " Z=" + minZ + ".." + maxZ);
        return new BoundingRect(minX, maxX, minZ, maxZ);
    }

    private static boolean isBoundaryOrCornerAdjacent(Level level, int x, int y, int z,
                                                      int perpDx, int perpDz) {
        if (isValidBoundaryBlock(level.getBlockState(new BlockPos(x, y, z)))) {
            return true;
        }
        if (CORNERS_REQUIRE_CASING) {
            return false;
        }
        return isValidBoundaryBlock(level.getBlockState(new BlockPos(x + perpDx, y, z + perpDz)))
                || isValidBoundaryBlock(level.getBlockState(new BlockPos(x - perpDx, y, z - perpDz)));
    }

    // ── STEP 2: expand the cross into the full outer rectangle ───────────────
    private static BoundingRect expandToRectangle(Level level, BlockPos controllerPos, BoundingRect cross, boolean cornersRequired) {
        int controllerY = controllerPos.getY();
        boolean controllerOnZWall = (cross.maxZ() - cross.minZ()) >= (cross.maxX() - cross.minX());

        SmelteryDebug.formation("Validate", "controllerOnZWall=" + controllerOnZWall);

        if (controllerOnZWall) {
            int minZ = cross.minZ();
            int maxZ = cross.maxZ();

            int searchMinZ = minZ;
            int searchMaxZ = maxZ;

            SmelteryDebug.formation("Validate",
                    "searchMinZ=" + searchMinZ + " searchMaxZ=" + searchMaxZ
            );

            int fromMinZ_minX = controllerPos.getX();
            int fromMinZ_maxX = controllerPos.getX();

            while (fromMinZ_minX > controllerPos.getX() - MAX_SIZE) {
                int nextX = fromMinZ_minX - 1;

                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(nextX, controllerY, searchMinZ)));

                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(nextX, controllerY, searchMinZ + 1)));
                }

                if (!valid) {
                    break;
                }

                fromMinZ_minX = nextX;
            }

            while (fromMinZ_maxX < controllerPos.getX() + MAX_SIZE) {
                int nextX = fromMinZ_maxX + 1;

                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(nextX, controllerY, searchMinZ)));

                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(nextX, controllerY, searchMinZ + 1)));
                }

                if (!valid) {
                    break;
                }

                fromMinZ_maxX = nextX;
            }

            int fromMaxZ_minX = controllerPos.getX();
            int fromMaxZ_maxX = controllerPos.getX();

            while (fromMaxZ_minX > controllerPos.getX() - MAX_SIZE) {
                int nextX = fromMaxZ_minX - 1;

                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(nextX, controllerY, searchMaxZ)));

                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(nextX, controllerY, searchMaxZ - 1)));
                }

                if (!valid) {
                    break;
                }

                fromMaxZ_minX = nextX;
            }

            while (fromMaxZ_maxX < controllerPos.getX() + MAX_SIZE) {
                int nextX = fromMaxZ_maxX + 1;

                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(nextX, controllerY, searchMaxZ)));

                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(nextX, controllerY, searchMaxZ - 1)));
                }

                if (!valid) {
                    break;
                }

                fromMaxZ_maxX = nextX;
            }

            int minX = Math.min(fromMinZ_minX, fromMaxZ_minX);
            int maxX = Math.max(fromMinZ_maxX, fromMaxZ_maxX);

            SmelteryDebug.formation("Validate",
                    "Rectangle evidence: "
                            + "fromMinWall=" + fromMinZ_minX + ".." + fromMinZ_maxX
                            + " | fromMaxWall=" + fromMaxZ_minX + ".." + fromMaxZ_maxX
                            + " | final=" + minX + ".." + maxX
            );

            return new BoundingRect(minX, maxX, searchMinZ, searchMaxZ);

        } else {
            int minX = cross.minX();
            int maxX = cross.maxX();

            int searchMinX = minX;
            int searchMaxX = maxX;

            SmelteryDebug.formation("Validate",
                    "searchMinX=" + searchMinX + " searchMaxX=" + searchMaxX
            );

            int fromMinX_minZ = controllerPos.getZ();
            int fromMinX_maxZ = controllerPos.getZ();

            // ---- grow Z on the min-X wall ----
            while (fromMinX_minZ > controllerPos.getZ() - MAX_SIZE) {
                int nextZ = fromMinX_minZ - 1;
                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(searchMinX, controllerY, nextZ)));
                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(searchMinX + 1, controllerY, nextZ)));
                }
                if (!valid) break;
                fromMinX_minZ = nextZ;
            }

            while (fromMinX_maxZ < controllerPos.getZ() + MAX_SIZE) {
                int nextZ = fromMinX_maxZ + 1;
                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(searchMinX, controllerY, nextZ)));
                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(searchMinX + 1, controllerY, nextZ)));
                }
                if (!valid) break;
                fromMinX_maxZ = nextZ;
            }

            int fromMaxX_minZ = controllerPos.getZ();
            int fromMaxX_maxZ = controllerPos.getZ();

            // ---- grow Z on the max-X wall ----
            while (fromMaxX_minZ > controllerPos.getZ() - MAX_SIZE) {
                int nextZ = fromMaxX_minZ - 1;
                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(searchMaxX, controllerY, nextZ)));
                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(searchMaxX - 1, controllerY, nextZ)));
                }
                if (!valid) break;
                fromMaxX_minZ = nextZ;
            }

            while (fromMaxX_maxZ < controllerPos.getZ() + MAX_SIZE) {
                int nextZ = fromMaxX_maxZ + 1;
                boolean valid = isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(searchMaxX, controllerY, nextZ)));
                if (!valid && !cornersRequired) {
                    valid = isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(searchMaxX - 1, controllerY, nextZ)));
                }
                if (!valid) break;
                fromMaxX_maxZ = nextZ;
            }

            int minZ = Math.min(fromMinX_minZ, fromMaxX_minZ);
            int maxZ = Math.max(fromMinX_maxZ, fromMaxX_maxZ);

            if (!cornersRequired) {
                int frontZ = controllerPos.getZ();

                if (!isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(maxX, controllerY, frontZ)))) {

                    int candidateX = maxX + 1;

                    if (isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(candidateX, controllerY, frontZ + 1)))) {
                        maxX = candidateX;
                    }
                }

                if (!isValidBoundaryBlock(level.getBlockState(
                        new BlockPos(minX, controllerY, frontZ)))) {

                    int candidateX = minX - 1;

                    if (isValidBoundaryBlock(level.getBlockState(
                            new BlockPos(candidateX, controllerY, frontZ + 1)))) {
                        minX = candidateX;
                    }
                }
            }

            SmelteryDebug.formation("Validate",
                    "Rectangle evidence: "
                            + "fromMinWall=" + fromMinX_minZ + ".." + fromMinX_maxZ
                            + " | fromMaxWall=" + fromMaxX_minZ + ".." + fromMaxX_maxZ
                            + " | final=" + minX + ".." + maxX
            );

            return new BoundingRect(searchMinX, searchMaxX, minZ, maxZ);
        }
    }


    // ── STEP 3: tier detection ────────────────────────────────────────────────
    private static SmelteryTier detectTier(Level level, BoundingRect rect, int controllerY, boolean cornersRequired) {
        SmelteryTier detectedTier = null;
        for (int x = rect.minX(); x <= rect.maxX(); x++) {
            for (int z = rect.minZ(); z <= rect.maxZ(); z++) {
                if (isSkippableCell(x, z, rect, cornersRequired)) continue;
                BlockState bs = level.getBlockState(new BlockPos(x, controllerY, z));
                SmelteryTier blockTier = getTierForBlock(bs.getBlock());
                if (blockTier == null) continue;
                if (detectedTier == null) {
                    detectedTier = blockTier;
                } else if (detectedTier != blockTier) {
                    SmelteryDebug.formation("Validate", "Mixed casing tiers detected");
                    return null;
                }
            }
        }
        return detectedTier;
    }

    // ── STEP 4: floor must be full casing, matching the detected tier ────────
    private static boolean validateFloor(Level level, BoundingRect rect, int floorY, SmelteryTier tier) {
        for (int x = rect.interiorMinX(); x <= rect.interiorMaxX(); x++) {
            for (int z = rect.interiorMinZ(); z <= rect.interiorMaxZ(); z++) {
                BlockState fs = level.getBlockState(new BlockPos(x, floorY, z));
                if (!isValidFloorBlock(fs)) {
                    SmelteryDebug.formation("Validate", "Floor block at " + x + "," + floorY + "," + z + " is " + fs.getBlock());
                    return false;
                }
                if (getTierForBlock(fs.getBlock()) != tier) {
                    SmelteryDebug.formation("Validate", "Floor tier mismatch at " + x + "," + floorY + "," + z);
                    return false;
                }
            }
        }
        return true;
    }

    // ── STEP 5: interior at controller's own Y must be air or a lava tank ────
    private static boolean validateInteriorAtControllerY(Level level, BoundingRect rect, int controllerY) {
        for (int x = rect.interiorMinX(); x <= rect.interiorMaxX(); x++) {
            for (int z = rect.interiorMinZ(); z <= rect.interiorMaxZ(); z++) {
                BlockPos iPos = new BlockPos(x, controllerY, z);
                if (!isValidInteriorBlock(level, iPos)) {
                    SmelteryDebug.formation("Validate", "Interior block at " + iPos + " is " + level.getBlockState(iPos).getBlock());
                    return false;
                }
            }
        }
        return true;
    }

    // ── STEP 6: full wall ring at controller's own Y ─────────────────────────
    private static boolean validateWalls(Level level, BoundingRect rect, BlockPos controllerPos, boolean cornersRequired) {
       SmelteryDebug.formation("Validate",
                "WALL RECTANGLE: X=" + rect.minX() + ".." + rect.maxX()
                        + " Z=" + rect.minZ() + ".." + rect.maxZ()
        );

        for (int x = rect.minX(); x <= rect.maxX(); x++) {
            for (int z = rect.minZ(); z <= rect.maxZ(); z++) {
                if (isSkippableCell(x, z, rect, cornersRequired)) continue;

                BlockPos wPos = new BlockPos(x, controllerPos.getY(), z);
                BlockState ws = level.getBlockState(wPos);

                if (!isValidWallBlock(ws)) {
                    SmelteryDebug.formation("Validate",
                            "Wall block at " + wPos + " is " + ws.getBlock()
                    );
                    return false;
                }
            }
        }

        return true;
    }

    // ── STEP 7: scan upward to find the roof ─────────────────────────────────
    private static HeightScanResult scanVerticalHeight(Level level, BoundingRect rect, int controllerY, SmelteryTier tier) {
        int roofY = controllerY;
        int maxHeight = tier.getMaxHeight();

        for (int y = controllerY + 1; y <= controllerY + maxHeight; y++) {
            for (int x = rect.minX(); x <= rect.maxX(); x++) {
                for (int z = rect.minZ(); z <= rect.maxZ(); z++) {
                    boolean interior = x > rect.minX() && x < rect.maxX() && z > rect.minZ() && z < rect.maxZ();
                    boolean corner = isCorner(x, z, rect.minX(), rect.maxX(), rect.minZ(), rect.maxZ());

                    if (interior) {
                        if (!isValidInteriorBlock(level, new BlockPos(x, y, z))) {
                            SmelteryDebug.formation("Validate", "Vertical scan stopped at Y=" + y);
                            return new HeightScanResult(roofY, false);
                        }
                    } else if (!corner) {
                        BlockState ws = level.getBlockState(new BlockPos(x, y, z));
                        if (!isValidWallBlock(ws)) {
                            SmelteryDebug.formation("Validate", "Vertical scan stopped at Y=" + y + " wall: " + ws.getBlock());
                            return new HeightScanResult(roofY, false);
                        }
                        SmelteryTier upperTier = getTierForBlock(ws.getBlock());
                        if (upperTier != null && upperTier != tier) {
                            SmelteryDebug.formation("Validate", "Mixed tier in upper wall at Y=" + y);
                            return new HeightScanResult(roofY, true);
                        }
                    }
                }
            }
            roofY = y;
        }
        return new HeightScanResult(roofY, false);
    }

    private static int countLavaTanks(Level level, int minX, int maxX, int minZ, int maxZ, int floorY, int roofY) {
        int count = 0;

        for (int y = floorY; y <= roofY; y++) {
            for (int x = minX; x <= maxX; x++) {
                for (int z = minZ; z <= maxZ; z++) {
                    boolean interior = x > minX && x < maxX && z > minZ && z < maxZ;
                    if (interior) continue;

                    if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof LavaTankBlockEntity) {
                        count++;
                    }
                }
            }
        }

        return count;
    }

    public static boolean isOnWallShell(SmelteryData data, BlockPos pos) {
        if (!data.isValid()) return false;
        BlockPos origin = data.interiorOrigin();
        int wallMinX = origin.getX() - 1, wallMaxX = origin.getX() + data.width();
        int wallMinZ = origin.getZ() - 1, wallMaxZ = origin.getZ() + data.depth();
        int wallMinY = origin.getY() - 1, wallMaxY = origin.getY() + data.height() - 1;
        if (pos.getY() < wallMinY || pos.getY() > wallMaxY) return false;
        boolean north = pos.getZ() == wallMinZ - 1 && pos.getX() >= wallMinX && pos.getX() <= wallMaxX;
        boolean south = pos.getZ() == wallMaxZ + 1 && pos.getX() >= wallMinX && pos.getX() <= wallMaxX;
        boolean west  = pos.getX() == wallMinX - 1 && pos.getZ() >= wallMinZ && pos.getZ() <= wallMaxZ;
        boolean east  = pos.getX() == wallMaxX + 1 && pos.getZ() >= wallMinZ && pos.getZ() <= wallMaxZ;
        return north || south || west || east;
    }

    public static Optional<AlloySmelterBlockEntity> findController(
            Level level, BlockPos pos, BiPredicate<SmelteryData, BlockPos> membershipTest) {
        for (BlockPos candidate : BlockPos.betweenClosed(
                pos.offset(-MAX_SIZE, -MAX_SIZE, -MAX_SIZE),
                pos.offset(MAX_SIZE, MAX_SIZE, MAX_SIZE))) {
            if (level.getBlockEntity(candidate) instanceof AlloySmelterBlockEntity smelter) {
                SmelteryData data = smelter.getSmelteryData();
                if (data != null && membershipTest.test(data, pos)) {
                    SmelteryDebug.formation("Find", "pos=" + pos + " claimed by controller=" + candidate);
                    return Optional.of(smelter);
                }
            }
        }
        SmelteryDebug.formation("Find", "pos=" + pos + " — no controller found within radius " + MAX_SIZE);
        return Optional.empty();
    }

    public static void notifyNearbyControllers(Level level, BlockPos changedPos) {
        for (BlockPos search : BlockPos.betweenClosed(
                changedPos.offset(-MAX_SIZE, -MAX_SIZE, -MAX_SIZE),
                changedPos.offset(MAX_SIZE, MAX_SIZE, MAX_SIZE))) {

            if (level.getBlockEntity(search) instanceof AlloySmelterBlockEntity be) {
                be.markNeedsRecheck();
            }
        }
    }
}