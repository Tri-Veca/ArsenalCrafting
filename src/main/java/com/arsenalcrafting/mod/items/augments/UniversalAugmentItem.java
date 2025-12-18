package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import com.arsenalcrafting.mod.component.DynamicComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class UniversalAugmentItem extends Item implements Augment, InstallableAugmentItem {
    private final String augmentId;
    private final int tier; // 0 for no tier

    public UniversalAugmentItem(String augmentId, int tier, Properties properties) {
        super(properties);
        this.augmentId = augmentId;
        this.tier = tier;
    }

    @Override
    public void installOnTool(ItemStack tool) {
        // Set enabled flag
        tool.set(DynamicComponents.getEnabledComponent(augmentId), true);

        // Set tier if applicable
        AugmentManager.AugmentDefinition def = AugmentManager.get(augmentId);
        if (def.hasTiers() && tier > 0) {
            tool.set(DynamicComponents.getTierComponent(augmentId), tier);
        }

        System.out.println("[Toolkit] " + def.name() + (tier > 0 ? " Tier " + tier : "") + " installed!");
    }

    public String getAugmentId() { return augmentId; }
    public int getTier() { return tier; }
}