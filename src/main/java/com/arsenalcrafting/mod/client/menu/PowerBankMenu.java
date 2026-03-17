package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Power Bank container menu.
 * No item slots — purely displays energy level and face I/O config.
 * Energy is synced via DataSlots (int x2 for full int range via split high/low).
 */
public class PowerBankMenu extends AbstractContainerMenu {

    private final PowerBankBlockEntity blockEntity;
    private final Level level;

    // Energy synced as two ints (high 16 bits + low 16 bits) to support large values
    private final DataSlot energyHigh  = DataSlot.standalone();
    private final DataSlot energyLow   = DataSlot.standalone();
    private final DataSlot maxHigh     = DataSlot.standalone();
    private final DataSlot maxLow      = DataSlot.standalone();
    private final DataSlot transferRate = DataSlot.standalone();

    // ── CLIENT CONSTRUCTOR ────────────────────────────────────────────────────
    public PowerBankMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, getBlockEntity(playerInventory, extraData));
    }

    private static PowerBankBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf data) {
        BlockPos pos = data.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof PowerBankBlockEntity bank) return bank;
        throw new IllegalStateException("No PowerBankBlockEntity at " + pos);
    }

    // ── SERVER CONSTRUCTOR ────────────────────────────────────────────────────
    public PowerBankMenu(int id, Inventory playerInventory, PowerBankBlockEntity blockEntity) {
        super(ModMenus.POWER_BANK.get(), id);
        this.blockEntity = blockEntity;
        this.level = playerInventory.player.level();

        // Discharge slot (left, red minus) x=17,y=34 — drains energy FROM bank into item
        addSlot(new Slot(blockEntity.getChargeInventory(), 1, 17, 34) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM, null) != null;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        // Charge slot (right, green plus) x=142,y=34 — pulls energy FROM item into bank
        addSlot(new Slot(blockEntity.getChargeInventory(), 0, 142, 34) {
            @Override
            public boolean mayPlace(net.minecraft.world.item.ItemStack stack) {
                return stack.getCapability(
                        net.neoforged.neoforge.capabilities.Capabilities.EnergyStorage.ITEM, null) != null;
            }
            @Override public int getMaxStackSize() { return 1; }
        });

        // Player inventory slots (3 rows) — y=84,102,120 from texture analysis
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar — y=142 from texture analysis
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Sync energy data
        addDataSlot(energyHigh);
        addDataSlot(energyLow);
        addDataSlot(maxHigh);
        addDataSlot(maxLow);
        addDataSlot(transferRate);
    }

    // ── DATA SYNC ─────────────────────────────────────────────────────────────

    @Override
    public void broadcastChanges() {
        int energy = blockEntity.getEnergyStored();
        int max    = blockEntity.getMaxEnergy();
        int rate   = blockEntity.getTier().transferRate();

        energyHigh.set((energy >> 16) & 0xFFFF);
        energyLow.set(energy & 0xFFFF);
        maxHigh.set((max >> 16) & 0xFFFF);
        maxLow.set(max & 0xFFFF);
        transferRate.set(Math.min(rate, Short.MAX_VALUE));

        super.broadcastChanges();
    }

    // ── ACCESSORS (client-side) ───────────────────────────────────────────────

    public int getEnergyStored() {
        return ((energyHigh.get() & 0xFFFF) << 16) | (energyLow.get() & 0xFFFF);
    }

    public int getMaxEnergy() {
        return ((maxHigh.get() & 0xFFFF) << 16) | (maxLow.get() & 0xFFFF);
    }

    public int getTransferRate() {
        return transferRate.get();
    }

    public float getFillFraction() {
        int max = getMaxEnergy();
        if (max <= 0) return 0f;
        return (float) getEnergyStored() / (float) max;
    }

    public PowerBankBlockEntity getBlockEntity() {
        return blockEntity;
    }

    // ── CONTAINER ─────────────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack stack = slot.getItem();
        result = stack.copy();

        // From charge/discharge slots → player inventory
        if (index < 2) {
            if (!moveItemStackTo(stack, 2, 38, true)) return ItemStack.EMPTY;
        }
        // From player inventory → try charge slots
        else {
            if (!moveItemStackTo(stack, 0, 2, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                AbstractContainerMenu.stillValid(
                        ContainerLevelAccess.create(blockEntity.getLevel(),
                                blockEntity.getBlockPos()),
                        player,
                        blockEntity.getBlockState().getBlock()
                );
    }
}