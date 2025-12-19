package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

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
            return type != null && type.isMiningTool();
        }

        @Override
        public void onBlockDestroyed(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
            Momentum.onBlockMined(stack, entity);
        }
    }

    public static final class BurnProtectionCore implements CoreHandler {
        @Override
        public boolean supportsToolType(ArsenalToolType type) {
            return type != null && (type.isMiningTool() || type.isWeapon());
        }
    }

}

