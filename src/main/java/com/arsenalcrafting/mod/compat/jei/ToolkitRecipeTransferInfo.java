package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.client.menu.ToolkitMenu;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.transfer.IRecipeTransferInfo;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ToolkitRecipeTransferInfo implements IRecipeTransferInfo<ToolkitMenu, ToolkitRecipeCategory.ToolkitRecipe> {

    @Override
    public Class<? extends ToolkitMenu> getContainerClass() {
        return ToolkitMenu.class;
    }

    @Override
    public Optional<MenuType<ToolkitMenu>> getMenuType() {
        return Optional.empty(); // Not needed for basic transfer
    }

    @Override
    public RecipeType<ToolkitRecipeCategory.ToolkitRecipe> getRecipeType() {
        return ToolkitRecipeCategory.RECIPE_TYPE;
    }

    @Override
    public boolean canHandle(ToolkitMenu container, ToolkitRecipeCategory.ToolkitRecipe recipe) {
        return true;
    }

    @Override
    public List<Slot> getRecipeSlots(ToolkitMenu container, ToolkitRecipeCategory.ToolkitRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        // Add input slots: head (0), core (1), rod (3) - note: output is slot 2
        slots.add(container.getSlot(0)); // Head
        slots.add(container.getSlot(1)); // Core
        slots.add(container.getSlot(3)); // Rod
        return slots;
    }

    @Override
    public List<Slot> getInventorySlots(ToolkitMenu container, ToolkitRecipeCategory.ToolkitRecipe recipe) {
        List<Slot> slots = new ArrayList<>();
        // Player inventory slots start at index 8 (after 8 container slots)
        for (int i = 8; i < container.slots.size(); i++) {
            slots.add(container.getSlot(i));
        }
        return slots;
    }

    public Class<ToolkitRecipeCategory.ToolkitRecipe> getRecipeClass() {
        return ToolkitRecipeCategory.ToolkitRecipe.class;
    }
}