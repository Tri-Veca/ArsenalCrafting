package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import net.minecraft.world.item.ItemStack;

public class RepairReduction {


    private RepairReduction() {} // Utility class

    /**
     * Returns the repair cost multiplier
     *
     * @param tool The tool ItemStack
     * @return repair cost multiplier (0.8f for 20% reduction)
     */
    public static float getRepairCostMultiplier(ItemStack tool) {
        return 0.8f; // 20% reduction (1.0 - 0.2 = 0.8)
    }

    /**
     * Calculates the reduced repair cost
     *
     * @param originalCost The original repair cost
     * @param tool The tool ItemStack
     * @return reduced repair cost (originalCost * 0.8)
     */
    public static float getReducedRepairCost(float originalCost, ItemStack tool) {
        return originalCost * getRepairCostMultiplier(tool);
    }
}




