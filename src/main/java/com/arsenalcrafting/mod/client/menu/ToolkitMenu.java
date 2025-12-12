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
import net.minecraft.world.inventory.ContainerListener;
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

    // --- slot counts ---
    private static final int TOOLKIT_SLOTS = ToolkitBlockEntity.SLOTS; // 8
    private static final int PLAYER_INV_ROWS = 3;
    private static final int PLAYER_INV_COLUMNS = 9;
    private static final int PLAYER_INV_SIZE = PLAYER_INV_ROWS * PLAYER_INV_COLUMNS; // 27
    private static final int HOTBAR_SIZE = 9;

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
    // --- THIS is what makes shift-click work ---
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack originalStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot != null && slot.hasItem()) {
            ItemStack stackInSlot = slot.getItem();
            originalStack = stackInSlot.copy();

            // Index layout:
            // 0 .. 7   -> Toolkit BE slots
            // 8 .. 34  -> Player inv
            // 35 .. 43 -> Hotbar

            int beStart = 0;
            int beEnd = TOOLKIT_SLOTS; // 8 (exclusive)
            int playerInvStart = beEnd; // 8
            int playerInvEnd = playerInvStart + PLAYER_INV_SIZE; // 8 + 27 = 35
            int hotbarStart = playerInvEnd; // 35
            int hotbarEnd = hotbarStart + HOTBAR_SIZE; // 44

            if (index < beEnd) {
                // Moving FROM toolkit → TO player
                if (!this.moveItemStackTo(stackInSlot, playerInvStart, hotbarEnd, true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Moving FROM player/hotbar → TO toolkit
                // We let IItemHandler.isItemValid decide which slots accept the item.
                if (!this.moveItemStackTo(stackInSlot, beStart, beEnd, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (stackInSlot.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return originalStack;
    }
}