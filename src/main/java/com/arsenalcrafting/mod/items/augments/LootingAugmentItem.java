package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LootingAugmentItem extends Item implements Augment, InstallableAugmentItem {

    private final int tier;

    public LootingAugmentItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() { return tier; }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("looting"), true);
        tool.set(DynamicComponents.getTierComponent("looting"), tier);
        System.out.println("[Toolkit] Looting tier " + tier + " installed!");
    }

    /**
     * Returns the looting level for the given tier.
     * One tier above vanilla max (matching Fortune augment design).
     * Called from ArsenalLootingEvent via LootingLevelEvent.
     */
    public static int getLootingLevel(int tier) {
        return switch (tier) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> 0;
        };
    }
}