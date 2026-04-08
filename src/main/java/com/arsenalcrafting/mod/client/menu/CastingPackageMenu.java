package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.items.CastingPackageItem;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import org.jetbrains.annotations.NotNull;

public class CastingPackageMenu extends AbstractContainerMenu {

    private final ItemStack packageStack;
    private final CastingPackageItem packageItem;
    private final PackageType packageType;
    private final ItemStackHandler inventory;
    private final Player player;
    private final RegistryAccess registryAccess;

    // Mold grid positions
    private static final int MOLD_GRID_START_X = 9;
    private static final int MOLD_GRID_START_Y = 31;
    private static final int MOLD_SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 9;

    // Player inventory positions
    private static final int PLAYER_INVENTORY_X = 8;
    private static final int PLAYER_INVENTORY_Y = 84;
    private static final int HOTBAR_X = 8;
    private static final int HOTBAR_Y = 142;

    // Client constructor
    public CastingPackageMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.getMainHandItem());
    }

    // Server constructor
    public CastingPackageMenu(int id, Inventory playerInventory, ItemStack packageStack) {
        super(ModMenus.CASTING_PACKAGE_MENU.get(), id);

        this.player = playerInventory.player;
        this.packageStack = packageStack;
        this.registryAccess = player.level().registryAccess();

        if (!(packageStack.getItem() instanceof CastingPackageItem)) {
            throw new IllegalArgumentException("Item is not a CastingPackageItem");
        }

        this.packageItem = (CastingPackageItem) packageStack.getItem();
        this.packageType = packageItem.getPackageType();
        this.inventory = packageItem.getInventory(packageStack, registryAccess);

        // Add mold slots - 9 per row, new row after every 9 slots
        int requiredMolds = packageType.getMolds().size();

        for (int i = 0; i < requiredMolds; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            int x = MOLD_GRID_START_X + (col * MOLD_SLOT_SIZE);
            int y = MOLD_GRID_START_Y + (row * MOLD_SLOT_SIZE);

            this.addSlot(new SlotItemHandler(inventory, i, x, y));
        }

        // Add player inventory slots
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                int slotIndex = col + row * 9 + 9;
                int x = PLAYER_INVENTORY_X + col * 18;
                int y = PLAYER_INVENTORY_Y + row * 18;
                this.addSlot(new Slot(playerInventory, slotIndex, x, y));
            }
        }

        // Add hotbar slots
        for (int col = 0; col < 9; ++col) {
            int x = HOTBAR_X + col * 18;
            int y = HOTBAR_Y;
            this.addSlot(new Slot(playerInventory, col, x, y));
        }
    }

    public PackageType getPackageType() {
        return packageType;
    }

    public ItemStackHandler getInventory() {
        return inventory;
    }

    public int getFilledCount() {
        return packageItem.getFilledCount(packageStack, registryAccess);
    }

    public int getTotalRequired() {
        return packageType.getMolds().size();
    }

    public boolean isComplete() {
        return packageItem.isComplete(packageStack, registryAccess);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return player.getMainHandItem() == packageStack ||
                player.getOffhandItem() == packageStack;
    }

    @Override
    public ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = this.slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int totalRequired = getTotalRequired();
        int firstPlayerSlot = totalRequired;
        int lastPlayerSlot = this.slots.size();

        if (index < totalRequired) {
            if (!this.moveItemStackTo(stack, firstPlayerSlot, lastPlayerSlot, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            if (packageItem.isValidMold(stack)) {
                if (!this.moveItemStackTo(stack, 0, totalRequired, false)) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }

        return copy;
    }

    public ItemStack extractMold(int slotIndex) {
        if (slotIndex >= 0 && slotIndex < inventory.getSlots()) {
            ItemStack extracted = inventory.extractItem(slotIndex, 64, false);
            if (!extracted.isEmpty()) {
                packageItem.saveInventory(packageStack, inventory, registryAccess);
            }
            return extracted;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player != null && !player.level().isClientSide) {
            packageItem.saveInventory(packageStack, inventory, registryAccess);
        }
    }
}