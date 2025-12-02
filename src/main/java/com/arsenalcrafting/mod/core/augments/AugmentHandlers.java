package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.items.augments.AutoSmeltAugmentItem;
import com.arsenalcrafting.mod.items.augments.OreMinerAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class AugmentHandlers {

    // ============= MINING AUGMENTS =============

    public static class AutoSmelt implements AugmentHandler {
        @Override public int priority() { return 10; }
        @Override public boolean isExclusive() { return false; } // FALSE so Ore-Miner can run after

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
        @Override public int priority() { return 20; }
        @Override public boolean isExclusive() { return true; } // TRUE to stop processing after vein mining

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

    // ============= COMBAT AUGMENTS =============

    public static class FireAspect implements AugmentHandler {
        @Override public int priority() { return 15; }
        @Override public boolean isExclusive() { return false; }

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
        @Override public int priority() { return 25; }
        @Override public boolean isExclusive() { return false; }

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

    // ============= UTILITY AUGMENTS =============

   /* public static class Magnet implements AugmentHandler {
        @Override public int priority() { return 100; } // Runs last
        @Override public boolean isExclusive() { return false; }

        @Override
        public boolean supportsToolType(ToolType type) {
            return type == ToolType.UNIVERSAL; // Works on everything
        }

        @Override
        public void onToolUpdate(ItemStack tool, Player player) {
            // Collect nearby items every tick
            collectNearbyItems(player);
        }
    }

    */

    // ============= TOOL-SPECIFIC AUGMENTS =============


}
