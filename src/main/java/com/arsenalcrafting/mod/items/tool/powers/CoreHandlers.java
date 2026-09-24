package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Random;

/**
 * Central holder for built-in CoreHandler implementations.
 *
 * CoreHandlers are SINGLETONS and must be stateless.
 */
public final class CoreHandlers {

    private CoreHandlers() {}


    //Register Core Abilities here until automated
    public static final SpeedBoostCore SPEED_BOOST = new SpeedBoostCore();
    public static final RepairReductionCore REPAIR_REDUCTION = new RepairReductionCore();
    public static final GreedyCore GREEDY = new GreedyCore();
    public static final MomentumCore MOMENTUM = new MomentumCore();
    public static final BurnProtectionCore BURN_PROTECTION = new BurnProtectionCore();
    public static final SturdyCore STURDY = new SturdyCore();
    public static final WorkHorseCore WORKHORSE = new WorkHorseCore();
    public static final GamblerCore GAMBLER = new GamblerCore();
    public static final PerfectStrikeHandler PERFECT_STRIKE = new PerfectStrikeHandler();



    /**
     * SpeedBoost core handler
     *
     * Dynamically increases mining speed as tool durability decreases.
     * This handler:
     * - Does NOT apply effects
     * - Does NOT tick
     * - Is fully reversible when repaired
     */
    public static final class SpeedBoostCore implements CoreHandler {

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            // SpeedBoost applies to all mining tools
            return type != null && type.isMiningTool();
        }

        /**
         * Modify mining speed directly.
         * <p>
         * Called from getDestroySpeed(...)
         */
        @Override
        public float modifyMiningSpeed(ItemStack tool, float baseSpeed) {
            return baseSpeed + SpeedBoost.getMiningSpeedBonus(tool);
        }
    }


    /**
     * Repair Reduction core handler
     */
    public static final class RepairReductionCore implements CoreHandler {
        @Override public boolean supportsToolType(ArsenalToolType type) {
            return type != null && (type.isMiningTool() || type.isWeapon());
        }

        // Helper methods for repair kit
        public static float getRepairBonus() {
            return RepairReduction.getBonusMultiplier();
        }

        public static String getBonusText() {
            return String.format("+%.0f%% repair", RepairReduction.getBonusPercentage());
        }
    }



    public static final class GreedyCore implements CoreHandler {
        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type != null && type.isMiningTool();
        }

        @Override
        public float modifyMiningSpeed(ItemStack tool, float baseSpeed) {
            return baseSpeed + Greedy.getMiningSpeedBonus();
        }

        @Override
        public float modifyMaxDurability(ItemStack tool, float currentMax) {
            // This is where we get the 0.85f from your Greedy class
            return currentMax * Greedy.getDurabilityMultiplier();
        }

    }

    public static final class MomentumCore implements CoreHandler {
        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type != null && (type.isMiningTool() || type.isWeapon());
        }

        @Override
        public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
            // Only count the origin block the player directly mined — not vein mine extras (#22)
            if (!Momentum.isOriginBlock()) return;
            Momentum.onBlockMined(stack, entity);
        }

        @Override
        public void onEntityHit(ItemStack weapon, LivingEntity attacker, Entity target, float damage) {
            Momentum.onWeaponHit(weapon, attacker);
        }

        @Override
        public void onToolUpdate(ItemStack tool, Player player) {
            // Decay weapon stacks if expired — only for weapons
            if (tool.getItem() instanceof ArsenalTool arsenalTool && arsenalTool.getToolType().isWeapon()) {
                Momentum.decayWeaponStacksIfNeeded(tool, player.level());
            }
        }
    }

    public static final class BurnProtectionCore implements CoreHandler {
        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type != null && (type.isMiningTool() || type.isWeapon());
        }
    }

    public static final class SturdyCore implements CoreHandler {
        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type != null && (type.isMiningTool() || type.isWeapon());
        }
    }

    public static final class WorkHorseCore implements CoreHandler {
        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            // widened from mining-tool-only, since weapons now need cobweb-break too
            return type != null && (type.isMiningTool() || type.isWeapon());
        }

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            if (!(tool.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool arsenalTool)) return false;
            ArsenalToolType type = arsenalTool.getToolType();

            if (type.isMiningTool() && type != ArsenalToolType.SHOVEL && type != ArsenalToolType.AXE) {
                // pickaxe: existing hard-block behavior only
                return WorkHorse.isHardBlock(state);
            }
            return false;
        }
        @Override
        public boolean handleEntityInteract(ItemStack tool, Player player, LivingEntity target, InteractionHand hand) {
            if (!(tool.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool arsenalTool)) return false;
            if (arsenalTool.getToolType() != ArsenalToolType.AXE) return false;
            return WorkHorse.tryShearEntity(player, target, hand);
        }

        @Override
        public void onEntityHit(ItemStack weapon, LivingEntity attacker, Entity target, float damage) {
            if (!(target instanceof LivingEntity livingTarget)) return;
            if (!(weapon.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool arsenalTool)) return;
            if (!arsenalTool.getToolType().isWeapon()) return;
            WorkHorse.trySlownessProc(livingTarget,  attacker.getRandom());
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool, Level level, BlockState state,
                                        BlockPos pos, LivingEntity miner) {
            return canHandleBlock(state, tool); // same gating logic, reused
        }

        @Override
        public float modifyMiningSpeed(ItemStack tool, float baseSpeed) {
            // DO NOT modify base speed here - let getDestroySpeed handle it
            return baseSpeed;
        }

        @Override
        public boolean handleBlockInteract(ItemStack tool, Level level, BlockPos pos,
                                           BlockState state, Player player) {

            if (!(tool.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool arsenalTool)) return false;
            ArsenalToolType type = arsenalTool.getToolType();
            System.out.println("[WorkHorseDebug] handleBlockInteract called, toolType=" + type);

            if (type == ArsenalToolType.SHOVEL) {
                return WorkHorse.tryHoeInteraction(level, pos, state, player);
            }
            if (type == ArsenalToolType.AXE) {
                return WorkHorse.tryShearInteraction(level, pos, state, player);
            }
            return false;
        }


    }



    public static final class GamblerCore implements CoreHandler {

        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return true; // Works on all tools for now
        }

        @Override
        public boolean canHandleBlock(BlockState state, ItemStack tool) {
            boolean isOre = state.is(BlockTags.COAL_ORES)
                    || state.is(BlockTags.COPPER_ORES)
                    || state.is(BlockTags.DIAMOND_ORES)
                    || state.is(BlockTags.EMERALD_ORES)
                    || state.is(BlockTags.GOLD_ORES)
                    || state.is(BlockTags.IRON_ORES)
                    || state.is(BlockTags.LAPIS_ORES)
                    || state.is(BlockTags.REDSTONE_ORES);

            //System.out.println("[GamblerCore] Checking: " + state.getBlock().getName().getString() + " = " + isOre);
            return isOre;
        }

        @Override
        public void onBlockDestroyed(ItemStack stack, Level level,
                                     BlockState state, BlockPos pos,
                                     LivingEntity entity) {
            CoreDebug.proc("Gambler", "onBlockDestroyed called for " + state.getBlock());

            if (canHandleBlock(state, stack)) {
                CoreDebug.proc("Gambler", "It's an ore, rolling for bonus loot...");
                Gambler.tryProcMining(level, pos, level.getRandom());
            }
        }

        @Override
        public boolean handleBlockBreak(ItemStack tool, Level level,
                                        BlockState state, BlockPos pos,
                                        LivingEntity miner) {
            return false;
        }

    }


    public static final class PerfectStrikeHandler implements CoreHandler {
        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type != null && (type.isMiningTool() || type.isWeapon());
        }

        @Override
        public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
            PerfectStrike.tryPerfectStrike(stack, level, state, pos, entity);
        }

        @Override
        public void onEntityHit(ItemStack weapon, LivingEntity attacker, Entity target, float damage) {
            PerfectStrike.tryExtraStrike(weapon, attacker, target, damage);
        }
    }

}