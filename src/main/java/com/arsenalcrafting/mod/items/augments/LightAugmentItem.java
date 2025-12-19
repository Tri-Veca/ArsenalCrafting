package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class LightAugmentItem extends Item implements Augment, InstallableAugmentItem {

    public LightAugmentItem(Properties properties) {
        super(properties);
    }



    @Override
    public void installOnTool(ItemStack tool) {
        // This should mark the augment as installed
        tool.set(DynamicComponents.getEnabledComponent("light"), true);
        System.out.println("[ArsenalCrafting] Light augment installed!");
    }
}