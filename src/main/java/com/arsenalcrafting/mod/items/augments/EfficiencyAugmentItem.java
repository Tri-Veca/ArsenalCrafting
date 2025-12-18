package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class EfficiencyAugmentItem extends Item implements Augment, InstallableAugmentItem {
    private final int tier;

    public EfficiencyAugmentItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void installOnTool(ItemStack tool) {
        // Set the components in DynamicComponents
        tool.set(DynamicComponents.getEnabledComponent("efficiency"), true);
        tool.set(DynamicComponents.getTierComponent("efficiency"), tier);
        System.out.println("[Toolkit] Efficiency tier " + tier + " installed!");
    }

    /**
     * Helper to get the speed bonus based on the tier we planned.
     */
    public static float getSpeedBonus(ItemStack tool, float speed) {
        // Only provide bonus if the augment is enabled
        if (!tool.getOrDefault(DynamicComponents.getEnabledComponent("efficiency"), false)) {
            return 0.0f;
        }

        int tier = tool.getOrDefault(DynamicComponents.getTierComponent("efficiency"), 1);

        return switch (tier) {
            case 1 -> 2.0f;   // Vanilla Efficiency I equivalent
            case 2 -> 10.0f;  // Vanilla Efficiency III equivalent
            case 3 -> 26.0f;  // Vanilla Efficiency V equivalent
            default -> 2.0f;
        };
    }
}