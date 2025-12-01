package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.blocks.entity.ToolkitBlockEntity;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolHeadItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolRodItem;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

// The Menu (Container) for the Toolkit block GUI. This connects the inventory slots and handles shift-clicking.
public class ToolkitMenu extends AbstractContainerMenu {
    private final ToolkitBlockEntity blockEntity;
    private final Level level;

    // Server-side constructor
    public ToolkitMenu(int id, Inventory inv, ToolkitBlockEntity entity) {
        super(ModMenus.TOOLKIT.get(), id);
        this.blockEntity = entity;
        this.level = inv.player.level();

        if (entity.getItemHandler() != null) {
            addToolSlots(entity);
            addPlayerInventory(inv);
            addPlayerHotbar(inv);
        }
    }

    // Client-side constructor (for opening via packet)
    public ToolkitMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        // Calls the server-side constructor after reading the BlockEntity from the packet data
        this(id, inv, getBlockEntity(inv, extraData));
    }

    /**
     * Helper method to read the BlockPos from the packet buffer and retrieve the BlockEntity.
     * @throws IllegalStateException if the extraData is null or the BlockEntity cannot be found/is wrong type.
     */
    private static ToolkitBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf extraData) {
        if (extraData == null) {
            throw new IllegalStateException("Cannot open ToolkitMenu: Missing BlockPos data in packet. " +
                    "Ensure server-side call uses player.openMenu(menuProvider, pos).");
        }

        BlockPos pos = extraData.readBlockPos();
        BlockEntity entity = inv.player.level().getBlockEntity(pos);

        if (entity instanceof ToolkitBlockEntity blockEntity) {
            return blockEntity;
        }

        // If the BlockEntity at the position is null or the wrong type, throw an error
        throw new IllegalStateException("Could not find expected ToolkitBlockEntity at " + pos);
    }

    private void addToolSlots(ToolkitBlockEntity entity) {

        // in image editor, layer slot will be x+1 y+1

        // --- 1. TOOL ASSEMBLY INPUTS/OUTPUT (Slots 0-3) ---
        // Index 0: Head Input Slot
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 0, 49, 6));

        // Index 1: Core Input Slot (DISABLED in BE)
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 1, 49, 29));

        // Index 2: Rod/Handle Input Slot
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 3, 49, 52));

        // Index 3: Output Slot
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 2, 116, 20));

        //augment slots

        //index 4: Augment 1
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 4, 84, 61));

        //index 5: Augment 2
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 5, 107, 61));

        //index 6: Augment 3
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 6, 129, 61));

        //index 7: Augment 4
        this.addSlot(new SlotItemHandler(entity.getItemHandler(), 7, 152, 61));

    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int i = 0; i < 3; ++i) {
            for (int l = 0; l < 9; ++l) {
                this.addSlot(new Slot(playerInventory, l + i * 9 + 9, 8 + l * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.TOOLKIT.get());
    }

    /**
     * Handles shift-click behavior (quick transfer).
     */
    @Override
    @NotNull
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack slotStack = slot.getItem();
        itemstack = slotStack.copy();

        final int toolkitSlots = ToolkitBlockEntity.SLOTS; // 9
        final int outputSlot = 3; // Output slot index
        final int augmentSlotsStart = 4; // First augment slot index

        // Player Inventory and Hotbar ranges
        final int playerInvStart = toolkitSlots; // 9
        final int playerInvEnd = playerInvStart + 27; // 36
        final int hotbarStart = playerInvEnd; // 36
        final int hotbarEnd = hotbarStart + 9; // 45 (Total slots)

        if (index < toolkitSlots) {
            // Moving from Toolkit to Player Inventory/Hotbar

            // Do not allow moving the tool from the Output slot (index 3) to prevent duplication issues
            if (index == outputSlot) {
                // Move item from output slot 3 to the player inventory (9-45)
                if (!moveItemStackTo(slotStack, playerInvStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Move item from any other toolkit slot (0, 1, 2, 4-8) to the player inventory (9-45)
                if (!moveItemStackTo(slotStack, playerInvStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            }
        } else if (index >= playerInvStart) {
            // Moving from Player Inventory/Hotbar to Toolkit Slots

            // 1. Try moving ToolHeadItem to Slot 0
            if (slotStack.getItem() instanceof ToolHeadItem) {
                if (!moveItemStackTo(slotStack, 0, 1, false)) { return ItemStack.EMPTY; }
            }
            // 2. Try moving ToolRodItem to Slot 2
            else if (slotStack.getItem() instanceof ToolRodItem) {
                if (!moveItemStackTo(slotStack, 2, 3, false)) { return ItemStack.EMPTY; }
            }
            else if (slotStack.getItem() instanceof ArsenalPickaxeItem) {
                // Allow moving assembled tools to the output slot (slot 2)
                if (!moveItemStackTo(slotStack, 2, 3, false)) {
                    return ItemStack.EMPTY;
                }
            }

            // 3. Try moving any other item to the Augment slots (4 through 8, range is [4, 9))
            else if (moveItemStackTo(slotStack, augmentSlotsStart, toolkitSlots, false)) {
                // Success moving to an augment slot
            }
            // 4. Default: Move between player inventory and hotbar
            else if (index < playerInvEnd) {
                // Moving from Main Inventory to Hotbar
                if (!moveItemStackTo(slotStack, hotbarStart, hotbarEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else if (index < hotbarEnd) {
                // Moving from Hotbar to Main Inventory
                if (!moveItemStackTo(slotStack, playerInvStart, playerInvEnd, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Should not happen
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
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
        return itemstack;
    }
}