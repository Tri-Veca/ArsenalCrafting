package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.ToolAugmentSlots;
import com.arsenalcrafting.mod.registry.ModComponents;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class ToolMenu extends AbstractContainerMenu {
    private final ItemStack toolStack;
    private final Player player;
    private final ArsenalTool arsenalTool;

    // Inventory slot constants
    private static final int PLAYER_INVENTORY_ROWS = 3;
    private static final int PLAYER_INVENTORY_COLUMNS = 9;
    private static final int PLAYER_INVENTORY_SLOTS = PLAYER_INVENTORY_ROWS * PLAYER_INVENTORY_COLUMNS; // 27
    private static final int HOTBAR_SLOTS = 9;

    // Player inventory slot positions
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 142;

    // Client-side constructor
    public ToolMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.getMainHandItem());
    }

    // Server-side constructor
    public ToolMenu(int id, Inventory playerInventory, ItemStack toolStack) {
        super(ModMenus.TOOL.get(), id);

        this.toolStack = toolStack;
        this.player = playerInventory.player;

        if (!(toolStack.getItem() instanceof ArsenalTool)) {
            throw new IllegalArgumentException("Item is not an ArsenalTool");
        }
        this.arsenalTool = (ArsenalTool) toolStack.getItem();

        // Add player inventory slots (27 slots)
        for (int row = 0; row < PLAYER_INVENTORY_ROWS; ++row) {
            for (int col = 0; col < PLAYER_INVENTORY_COLUMNS; ++col) {
                int slotIndex = col + row * 9 + 9; // +9 to skip hotbar
                int x = PLAYER_INVENTORY_X + col * 18;
                int y = PLAYER_INVENTORY_Y + row * 18;
                this.addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }

        // Add hotbar slots (9 slots)
        for (int col = 0; col < HOTBAR_SLOTS; ++col) {
            int x = HOTBAR_X + col * 18;
            int y = HOTBAR_Y;
            this.addSlot(new Slot(playerInventory, col, x, y));
        }
    }

    public ItemStack getToolStack() {
        return toolStack;
    }

    public ArsenalTool getArsenalTool() {
        return arsenalTool;
    }

    public ToolAugmentSlots getAugmentSlots() {
        return arsenalTool.getAugmentSlots(toolStack);
    }

    public boolean isAugmentEnabled(String augmentId) {
        // ADD NULL CHECK
        if (augmentId == null || augmentId.isEmpty()) {
            return false;
        }
        return AugmentManager.isInstalled(augmentId, toolStack);
    }

    public void toggleAugment(String augmentId) {
        // ADD NULL CHECK
        if (augmentId == null || augmentId.isEmpty()) {
            return; // Can't toggle null/empty augment
        }

        boolean currentlyEnabled = isAugmentEnabled(augmentId);
        arsenalTool.setAugmentEnabled(toolStack, augmentId, !currentlyEnabled);

        // Sync with client
        broadcastChanges();
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        // Only valid if player is still holding the tool
        return player.getMainHandItem() == toolStack ||
                player.getOffhandItem() == toolStack;
    }

    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack slotStack = slot.getItem();
            itemstack = slotStack.copy();

            // Player inventory to hotbar or vice versa
            if (index < PLAYER_INVENTORY_SLOTS) {
                // From player inventory to hotbar
                if (!this.moveItemStackTo(slotStack, PLAYER_INVENTORY_SLOTS, PLAYER_INVENTORY_SLOTS + HOTBAR_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < PLAYER_INVENTORY_SLOTS + HOTBAR_SLOTS) {
                // From hotbar to player inventory
                if (!this.moveItemStackTo(slotStack, 0, PLAYER_INVENTORY_SLOTS, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (slotStack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (slotStack.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, slotStack);
        }

        return itemstack;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        // Update the tool stack in case augments were toggled
        if (!player.level().isClientSide) {
            // Force sync of the tool item
            player.inventoryMenu.broadcastChanges();
        }
    }
}