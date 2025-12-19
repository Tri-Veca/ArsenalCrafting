package com.arsenalcrafting.mod.core.alloy;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeInput;

public record AlloyRecipeInput(net.minecraft.world.Container container) implements RecipeInput {

    @Override
    public ItemStack getItem(int slot) {
        return container.getItem(slot);
    }

    @Override
    public int size() {
        return container.getContainerSize();
    }
}