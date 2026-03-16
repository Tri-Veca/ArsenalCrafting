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
        // Track which item stack ID placed the light — prevents multi-tool flicker (#19)
        // Key = player UUID, Value = System.identityHashCode of the stack that owns the light
        private static final Map<UUID, Integer> LIGHT_OWNER = new HashMap<>();

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

            UUID uuid = player.getUUID();
            int stackId = System.identityHashCode(stack);

            // Only the currently held tool manages the light.
            // If another stack owns it, this one steps back. (#19 multi-tool flicker fix)
            Integer owner = LIGHT_OWNER.get(uuid);
            if (owner != null && owner != stackId) {
                // This is not the owning tool — don't touch the light
                return;
            }

            BlockPos currentPos = player.blockPosition().above();
            BlockPos lastPos = PLAYER_LIGHT_POS.get(uuid);

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
                // Remove old light — also scan nearby in case fast movement left orphans
                if (lastPos != null) {
                    removeLightBlock(level, lastPos);
                    // Clean up any orphaned lights within 3 blocks of last pos (#19 fast movement fix)
                    for (BlockPos nearby : BlockPos.betweenClosed(
                            lastPos.offset(-2, -1, -2), lastPos.offset(2, 2, 2))) {
                        if (ALL_LIGHT_BLOCKS.contains(nearby.immutable())
                                && level.getBlockState(nearby).getBlock() instanceof LightBlock) {
                            removeLightBlock(level, nearby.immutable());
                        }
                    }
                }

                // Try to place new light
                if (canPlaceLight(level, currentPos)) {
                    placeLightBlock(level, currentPos);
                    PLAYER_LIGHT_POS.put(uuid, currentPos);
                    LIGHT_OWNER.put(uuid, stackId);
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
                            PLAYER_LIGHT_POS.put(uuid, offset);
                            LIGHT_OWNER.put(uuid, stackId);
                            markAsTemporaryLight(level, offset);
                            break;
                        }
                    }
                }
            }
        }

        @Override
        public void onToolUnequip(ItemStack stack, Player player) {
            // #19: Always clean up on unequip — handles multi-tool scenario
            removePlayerLight(player);
        }

        @Override
        public void onToolEquip(ItemStack stack, Player player) {
            // #19: Clear any stale light from a previous tool before placing new one
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
            UUID uuid = player.getUUID();
            BlockPos pos = PLAYER_LIGHT_POS.remove(uuid);
            LIGHT_OWNER.remove(uuid);
            if (pos != null) {
                removeLightBlock(player.level(), pos);
            }
        }

        public static void cleanupOnWorldLoad(Level level) {
            PLAYER_LIGHT_POS.clear();
            ALL_LIGHT_BLOCKS.clear();
            LIGHT_OWNER.clear();
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

    // ============= WEAPON AUGMENTS =============

    /**
     * Sharpness — weapons only.
     * Tier 1: +1.5 bonus damage  Tier 2: +3.0  Tier 3: +5.0
     * Applies flat bonus damage on top of assembled damage + crits.
     */
    public static class Sharpness implements AugmentHandler {
        @Override
        public int priority() { return 20; }

        @Override
        public boolean isExclusive() { return false; }

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
            if (!(target instanceof LivingEntity livingTarget)) return false;
            return SharpnessAugmentItem.applySharpness(tool, attacker, livingTarget);
        }
    }

    /**
     * Looting — weapons only.
     * Tier 1: Looting I  Tier 2: Looting II  Tier 3: Looting III
     * Works via LootingLevelEvent in ArsenalLootingEvent — no handleEntityAttack needed.
     */
    public static class Looting implements AugmentHandler {
        @Override
        public int priority() { return 10; }

        @Override
        public boolean isExclusive() { return false; }

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type.isWeapon();
        }

        public static int getLootingLevel(int tier) {
            return LootingAugmentItem.getLootingLevel(tier);
        }
    }

    /**
     * Knockback — weapons only.
     * Tier 1: ~vanilla sword knockback  Tier 2: sprint-attack level  Tier 3: above vanilla max
     * Pushes target away from attacker on hit.
     */
    public static class Knockback implements AugmentHandler {
        private static final float[] STRENGTH = { 0f, 0.4f, 0.7f, 1.1f };

        @Override
        public int priority() { return 30; }

        @Override
        public boolean isExclusive() { return false; }

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
            if (!(target instanceof LivingEntity livingTarget)) return false;
            return KnockbackAugmentItem.applyKnockback(tool, attacker, livingTarget);
        }
    }
}