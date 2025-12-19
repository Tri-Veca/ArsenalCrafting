package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.items.augments.*;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
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

public class AugmentHandlers {

    // ============= MINING AUGMENTS =============

    public static class AutoSmelt implements AugmentHandler {
        @Override
        public int priority() {
            return 10;
        }

        @Override
        public boolean isExclusive() {
            return false;
        } // FALSE so Ore-Miner can run after

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            return true; // Let handleBlockBreak check recipes
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
            return 20;
        }

        @Override
        public boolean isExclusive() {
            return true;
        } // TRUE to stop processing after vein mining

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            return OreMinerAugmentItem.isOre(state);
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                        BlockPos pos, LivingEntity miner) {
            return OreMinerAugmentItem.onBlockMined(tool, level, state, pos, miner);
        }
    }

    public static class SilkTouch implements AugmentHandler {
        @Override
        public int priority() {
            return 10;
        }

        @Override
        public boolean isExclusive() {
            return true;
        } // TRUE to stop processing after vein mining

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
            // Run before AutoSmelt / OreMiner or after, depending on how you want the combo.
            // You can adjust this value to tune ordering.
            return 30;
        }

        @Override
        public boolean isExclusive() {
            // false so it can combo with AutoSmelt, OreMiner, Magnetism, etc.
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            // Fortune only makes sense on pickaxes in your system
            return type == ArsenalToolType.PICKAXE;
        }

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            // Only run if Fortune is actually installed on the tool
            return tool.getOrDefault(DynamicComponents.getEnabledComponent("fortune"), false);
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool,
                                        Level level,
                                        BlockState state,
                                        BlockPos pos,
                                        LivingEntity miner) {
            return FortuneAugmentItem.onBlockMined(tool, level, state, pos, miner);
        }

    }


    public static class Light implements AugmentHandler {

        // Track current light position per player
        private static final Map<UUID, BlockPos> PLAYER_LIGHT_POS = new HashMap<>();
        // Track all light blocks we've placed in this session
        private static final Set<BlockPos> ALL_LIGHT_BLOCKS = new HashSet<>();

        @Override
        public int priority() {
            return 5;
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type == ArsenalToolType.PICKAXE;
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
                // No light currently placed
                needsMove = true;
            } else if (!currentPos.equals(lastPos)) {
                // Player moved
                needsMove = true;
            } else if (!(level.getBlockState(lastPos).getBlock() instanceof LightBlock)) {
                // Light was destroyed by something
                needsMove = true;
            } else if (level.getBlockState(lastPos).getValue(LightBlock.LEVEL) < 15) {
                // Light level is wrong
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

        /**
         * Mark a light block as temporary so we can clean it up on world load
         */
        private void markAsTemporaryLight(Level level, BlockPos pos) {
            if (!level.isClientSide) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    // Store metadata in the block entity
                    CompoundTag tag = blockEntity.getPersistentData();
                    tag.putBoolean("arsenal_temporary_light", true);
                    tag.putLong("arsenal_placed_time", level.getGameTime());
                    blockEntity.setChanged();
                }
            }
            ALL_LIGHT_BLOCKS.add(pos);
        }

        /**
         * Check if we can place a light block here
         */
        private boolean canPlaceLight(Level level, BlockPos pos) {
            BlockState state = level.getBlockState(pos);
            return state.isAir() ||
                    state.getBlock() == Blocks.LIGHT ||
                    state.canBeReplaced();
        }

        /**
         * Place light block with our custom properties
         */
        private void placeLightBlock(Level level, BlockPos pos) {
            // Use a light block with level 15 (maximum brightness)
            BlockState lightState = Blocks.LIGHT.defaultBlockState()
                    .setValue(LightBlock.LEVEL, 15)
                    .setValue(LightBlock.WATERLOGGED, false);

            level.setBlock(pos, lightState, 3); // Block.UPDATE_ALL flag

            // Force light update
            level.getChunkSource().getLightEngine().checkBlock(pos);
        }

        /**
         * Remove light block safely
         */
        private void removeLightBlock(Level level, BlockPos pos) {
            if (level.getBlockState(pos).getBlock() == Blocks.LIGHT) {
                level.removeBlock(pos, false);
                ALL_LIGHT_BLOCKS.remove(pos);
            }
        }

        /**
         * Remove all lights for a player
         */
        public void removePlayerLight(Player player) {
            BlockPos pos = PLAYER_LIGHT_POS.remove(player.getUUID());
            if (pos != null) {
                removeLightBlock(player.level(), pos);
            }
        }

        /**
         * CRITICAL: Clean up all temporary lights on world load
         * Call this from a world load event handler
         */
        public static void cleanupOnWorldLoad(Level level) {
            // Clear memory tracking
            PLAYER_LIGHT_POS.clear();
            ALL_LIGHT_BLOCKS.clear();

            System.out.println("[ArsenalCrafting] Light augment: Cleared memory tracking on world load");

            // Optional: Scan and remove any light blocks around players
            // This is more aggressive but fixes stuck blocks
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


        /**
         * Clean up when player leaves/logs out
         */
        public static void onPlayerLogout(Player player) {
            PLAYER_LIGHT_POS.remove(player.getUUID());
        }
    }

    public static class Magnetism implements AugmentHandler {
        @Override
        public int priority() {
            return 100; // Utility runs after mining/combat logic
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public void onToolUpdate(ItemStack tool, Player player) {
            MagnetismAugmentItem.tickMagnet(tool, player);
        }
    }

    public static class Efficiency implements AugmentHandler {
        @Override
        public int priority() {
            return 0; // Speed should be calculated early
        }

        @Override
        public boolean isExclusive() {
            return false;
        }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isMiningTool();
        }


        // Efficiency doesn't "handle" the block break (drops),
        // it only changes speed, so we don't override handleBlockBreak.
    }


    // ============= COMBAT AUGMENTS =============

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
            return type.isWeapon(); // Only weapons
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
                player.heal(damage * 0.1f); // Heal 10% of damage dealt
                return true;
            }
            return false;
        }
    }
}







