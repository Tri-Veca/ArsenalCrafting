package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.items.augments.*;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Contains all augment handler implementations.
 * Each handler delegates to the corresponding augment item's static methods.
 */
public class AugmentHandlers {

    // ============= MINING AUGMENTS =============

    public static class AutoSmelt implements AugmentHandler {
        @Override
        public int priority() {
            return 10;
        }

        @Override
        public boolean isExclusive() {
            return false; // Allows combos with other augments
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isMiningTool();
        }

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            return true; // Let the item method check recipes
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                        BlockPos pos, LivingEntity miner) {
            return AutoSmeltAugmentItem.onBlockMined(tool, level, state, pos, miner);
        }
    }

    public static class OreMiner implements AugmentHandler {
        @Override
        public int priority() {
            return 20; // Runs AFTER Auto-Smelt (10), BEFORE Fortune (30)
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isMiningTool();
        }

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            return OreMinerAugmentItem.isOre(state);
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                        BlockPos pos, LivingEntity miner) {
            if (!(level instanceof ServerLevel serverLevel)) {
                return false;
            }

            if (!(miner instanceof Player player)) {
                return false;
            }

            boolean autoSmeltActive = AugmentManager.isActive("auto_smelt", tool);
            boolean fortuneActive = AugmentManager.isActive("fortune", tool);

            System.out.println("[OreMiner] AutoSmelt: " + autoSmeltActive + ", Fortune: " + fortuneActive);

            if (autoSmeltActive && fortuneActive) {
                // All three: Auto-Smelt + Fortune + Ore Miner
                System.out.println("[OreMiner] Processing with Fortune + Auto-Smelt");
                return OreMinerAugmentItem.veinMineWithFortuneAndSmelting(
                        serverLevel, pos, state, player, tool);
            } else if (autoSmeltActive) {
                // Just Auto-Smelt + Ore Miner (no Fortune)
                System.out.println("[OreMiner] Processing with Auto-Smelt only");
                return OreMinerAugmentItem.veinMineWithSmelting(
                        serverLevel, pos, state, player, tool);
            } else if (fortuneActive) {
                // Just Fortune + Ore Miner (no Auto-Smelt)
                System.out.println("[OreMiner] Processing with Fortune only");
                return OreMinerAugmentItem.veinMineWithFortune(
                        serverLevel, pos, state, player, tool);
            } else {
                // Just Ore Miner alone
                System.out.println("[OreMiner] Processing normally");
                return OreMinerAugmentItem.veinMineNormal(
                        serverLevel, pos, state, player, tool);
            }
        }
    }

    public static class SilkTouch implements AugmentHandler {
        @Override
        public int priority() {
            return 10; // Same priority as Auto-Smelt
        }

        @Override
        public boolean isExclusive() {
            return AugmentCombos.SILK_TOUCH_BLOCKS_FORTUNE; // Configurable!
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isMiningTool();
        }

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            return true;
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                        BlockPos pos, LivingEntity miner) {
            return SilkTouchAugmentItem.onBlockMined(tool, level, state, pos, miner);
        }
    }

    public static class Fortune implements AugmentHandler {
        @Override
        public int priority() {
            return 30; // Run AFTER Auto-Smelt (10) and Ore Miner (20)
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isMiningTool();
        }

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            return FortuneAugmentItem.isOre(state);
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                        BlockPos pos, LivingEntity miner) {
            // Let FortuneAugmentItem.onBlockMined() decide whether to run
            // It has the logic to check other augments
            return FortuneAugmentItem.onBlockMined(tool, level, state, pos, miner);
        }
    }

    public static class Light implements AugmentHandler {
        // Track current light position per player
        private static final Map<UUID, BlockPos> PLAYER_LIGHT_POS = new HashMap<>();
        // Track all light blocks we've placed
        private static final Set<BlockPos> ALL_LIGHT_BLOCKS = new HashSet<>();

        @Override
        public int priority() {
            return 5; // Early priority for utility
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isMiningTool();
        }

        @Override
        public void onToolUpdate(ItemStack stack, Player player) {
            Level level = player.level();
            if (level.isClientSide) return;

            BlockPos currentPos = player.blockPosition().above();
            BlockPos lastPos = PLAYER_LIGHT_POS.get(player.getUUID());

            // Check if we need to move the light
            boolean needsMove = false;

            if (lastPos == null) {
                needsMove = true;
            } else if (!currentPos.equals(lastPos)) {
                needsMove = true;
            } else if (!(level.getBlockState(lastPos).getBlock() instanceof LightBlock)) {
                needsMove = true;
            } else if (level.getBlockState(lastPos).getValue(LightBlock.LEVEL) < 15) {
                needsMove = true;
            }

            if (needsMove) {
                // Remove old light
                if (lastPos != null) {
                    removeLightBlock(level, lastPos);
                }

                // Try to place new light
                if (canPlaceLight(level, currentPos)) {
                    placeLightBlock(level, currentPos);
                    PLAYER_LIGHT_POS.put(player.getUUID(), currentPos);
                    markAsTemporaryLight(level, currentPos);
                } else {
                    // Try positions around the player
                    for (BlockPos offset : new BlockPos[]{
                            currentPos.above(),
                            currentPos.north(),
                            currentPos.south(),
                            currentPos.east(),
                            currentPos.west()
                    }) {
                        if (canPlaceLight(level, offset)) {
                            placeLightBlock(level, offset);
                            PLAYER_LIGHT_POS.put(player.getUUID(), offset);
                            markAsTemporaryLight(level, offset);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onToolUnequip(ItemStack stack, Player player) {
            removePlayerLight(player);
        }

        private void markAsTemporaryLight(Level level, BlockPos pos) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    CompoundTag tag = blockEntity.getPersistentData();
                    tag.putBoolean("arsenal_temporary_light", true);
                    tag.putLong("arsenal_placed_time", level.getGameTime());
                    blockEntity.setChanged();
                }
            }
            ALL_LIGHT_BLOCKS.add(pos);
        }

        private boolean canPlaceLight(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.isAir() || state.getBlock() == Blocks.LIGHT || state.canBeReplaced();
        }

        private void placeLightBlock(Level level, BlockPos pos) {
            BlockState lightState = Blocks.LIGHT.defaultBlockState()
                    .setValue(LightBlock.LEVEL, 15)
                    .setValue(LightBlock.WATERLOGGED, false);

            level.setBlock(pos, lightState, 3);
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }

        private void removeLightBlock(Level level, BlockPos pos) {
            if (level.getBlockState(pos).getBlock() == Blocks.LIGHT) {
                level.removeBlock(pos, false);
                ALL_LIGHT_BLOCKS.remove(pos);
            }
        }

        public void removePlayerLight(Player player) {
            BlockPos pos = PLAYER_LIGHT_POS.remove(player.getUUID());
            if (pos != null) {
                removeLightBlock(player.level(), pos);
            }
        }

        public static void cleanupOnWorldLoad(Level level) {
            PLAYER_LIGHT_POS.clear();
            ALL_LIGHT_BLOCKS.clear();
            System.out.println("[ArsenalCrafting] Light augment: Cleared memory tracking on world load");

            // Clean up any light blocks around players
            for (Player player : level.players()) {
                BlockPos center = player.blockPosition();
                for (int x = -2; x <= 2; x++) {
                    for (int y = -1; y <= 3; y++) {
                        for (int z = -2; z <= 2; z++) {
                            BlockPos pos = center.offset(x, y, z);
                            if (level.getBlockState(pos).getBlock() == Blocks.LIGHT) {
                                level.removeBlock(pos, false);
                            }
                        }
                    }
                }
            }
        }

        public static void onPlayerLogout(Player player) {
            PLAYER_LIGHT_POS.remove(player.getUUID());
        }
    }

    public static class Magnetism implements AugmentHandler {
        @Override
        public int priority() {
            return 100; // Utility runs last
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return true; // Works with all tools
        }

        @Override
        public void onToolUpdate(ItemStack tool, Player player) {
            // Delegate to the augment item's static method
            MagnetismAugmentItem.tickMagnet(tool, player);
        }
    }

    public static class Efficiency implements AugmentHandler {
        @Override
        public int priority() {
            return 0; // Speed calculated early
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isMiningTool();
        }

        // Efficiency doesn't handle block breaks directly
        // It modifies speed via AugmentProcessor.applyMiningSpeedModifiers()
    }

    // ============= COMBAT AUGMENTS (Future) =============

    public static class FireAspect implements AugmentHandler {
        @Override
        public int priority() {
            return 15;
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isWeapon();
        }

        @Override
        public boolean canHandleEntity(Entity target, ItemStack tool) {
            return target instanceof LivingEntity;
        }

        @Override
        public boolean handleEntityAttack(ItemStack tool, LivingEntity attacker,
                                          Entity target, float damage) {
            if (target instanceof LivingEntity living) {
                living.igniteForSeconds(3);
                return true;
            }
            return false;
        }
    }

    public static class LifeSteal implements AugmentHandler {
        @Override
        public int priority() {
            return 25;
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isWeapon();
        }

        @Override
        public boolean handleEntityAttack(ItemStack tool, LivingEntity attacker,
                                          Entity target, float damage) {
            if (attacker instanceof Player player) {
                player.heal(damage * 0.1f);
                return true;
            }
            return false;
        }
    }
}