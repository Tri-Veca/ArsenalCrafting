package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

public class AlloySmelterMenu extends AbstractContainerMenu {
    public final AlloySmelterBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    // Client constructor
    public AlloySmelterMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory,
                playerInventory.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(4));
    }

    // Server constructor
    public AlloySmelterMenu(int containerId, Inventory playerInventory,
                            BlockEntity entity, ContainerData data) {
        super(ModMenus.ALLOY_SMELTER_MENU.get(), containerId);

        checkContainerSize(playerInventory, 10);
        this.blockEntity = (AlloySmelterBlockEntity) entity;
        this.level       = playerInventory.player.level();
        this.data        = data;

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);

        var handler = this.blockEntity.getItemHandler();

        // Input grid (3x3) - slots 0-8 — unchanged
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int index = row * 3 + col;
                this.addSlot(new SlotItemHandler(handler, index, 10 + col * 18, 19 + row * 18));
            }
        }

        // ── OLD ITEM OUTPUT SLOT (commented out — slot 9 now unused) ─────────
        // this.addSlot(new SlotItemHandler(handler, 9, 143, 35) {
        //     @Override
        //     public boolean mayPlace(ItemStack stack) {
        //         return false; // Output only
        //     }
        // });

        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot instanceof SlotItemHandler) {
                System.out.println("[Menu] Slot " + i + " is SlotItemHandler with index "
                        + ((SlotItemHandler) slot).getSlotIndex());
            }
        }

        addDataSlots(data);
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (blockEntity != null) {
            blockEntity.setChanged();
            slots.forEach(Slot::setChanged);
        }
        System.out.println(getDebugInfo());
    }

    // ── LAVA DISPLAY TANK GETTERS (existing — unchanged) ─────────────────────

    public int getScaledFluid() {
        int fluidAmount = blockEntity.DISPLAY_TANK.getFluidAmount();
        int capacity    = blockEntity.DISPLAY_TANK.getCapacity();
        int tankHeight  = 60;
        return capacity != 0 ? (fluidAmount * tankHeight / capacity) : 0;
    }

    public int getFluidAmount()    { return blockEntity.DISPLAY_TANK.getFluidAmount(); }
    public boolean hasFluid()      { return !blockEntity.DISPLAY_TANK.isEmpty(); }
    public int getFluidCapacity()  { return blockEntity.DISPLAY_TANK.getCapacity(); }
    public FluidStack getFluid()   { return blockEntity.DISPLAY_TANK.getFluid(); }
    public int getDisplayCapacity(){ return blockEntity.DISPLAY_TANK.getCapacity(); }

    // ── OUTPUT TANK GETTERS (new) ─────────────────────────────────────────────

    // Get total amount of ALL fluids combined (for scaling the tank visual)
    public int getOutputFluidAmount() {
        return blockEntity.OUTPUT_TANK.getTotalAmount();
    }

    // Get total capacity
    public int getOutputFluidCapacity() {
        return blockEntity.OUTPUT_TANK.getCapacity();
    }

    // For tooltip/debug - returns the first fluid (or all)
    public FluidStack getOutputFluid() {
        List<FluidStack> fluids = blockEntity.OUTPUT_TANK.getAllFluids();
        return fluids.isEmpty() ? FluidStack.EMPTY : fluids.get(0);
    }

    // Check if any fluid exists
    public boolean hasOutputFluid() {
        return !blockEntity.OUTPUT_TANK.isEmpty();
    }

    // Get all fluids for tooltip display
    public List<FluidStack> getAllOutputFluids() {
        return new ArrayList<>(blockEntity.OUTPUT_TANK.getAllFluids());
    }

    // Get fluid by index or type (for rendering multiple tanks)
    public FluidStack getFluidAt(int index) {
        List<FluidStack> fluids = blockEntity.OUTPUT_TANK.getAllFluids();
        return index < fluids.size() ? fluids.get(index) : FluidStack.EMPTY;
    }

    // Get number of different fluids in tank
    public int getFluidCount() {
        return blockEntity.OUTPUT_TANK.getAllFluids().size();
    }


    public int getScaledOutputFluid() {
        int amount   = blockEntity.OUTPUT_TANK.getTotalAmount();
        int capacity = blockEntity.OUTPUT_TANK.getCapacity();
        int height   = 60;
        return capacity != 0 ? (amount * height / capacity) : 0;
    }

    // ── PROGRESS (existing — unchanged) ───────────────────────────────────────

    public boolean isCrafting() { return data.get(0) > 0; }

    public int getScaledProgress() {
        int progress    = data.get(0);
        int maxProgress = data.get(1);
        int arrowSize   = 24;
        return maxProgress != 0 ? (progress * arrowSize / maxProgress) : 0;
    }

    public int getScaledFuelProgress() {
        int fuelTime    = data.get(2);
        int maxFuelTime = data.get(3);
        int fuelBarSize = 14;
        return maxFuelTime != 0 ? (fuelTime * fuelBarSize / maxFuelTime) : 0;
    }


    public String getDebugInfo() {
        FluidStack outputFluid = getOutputFluid();
        String outputFluidName = outputFluid.isEmpty() ? "Empty" : outputFluid.getFluid().getFluidType().getDescription().getString();

        return String.format("Display: %d/%d mb | Output: %d mb of %s | Connected tanks: %d",
                getFluidAmount(),
                getFluidCapacity(),
                getOutputFluidAmount(),
                outputFluidName,
                blockEntity.getConnectedTanksCount());
    }

    public int getConnectedTanksCount() { return data.get(2); }

    // ── SHIFT CLICK (existing — unchanged) ────────────────────────────────────

    private static final int HOTBAR_SLOT_COUNT              = 9;
    private static final int PLAYER_INVENTORY_ROW_COUNT     = 3;
    private static final int PLAYER_INVENTORY_COLUMN_COUNT  = 9;
    private static final int PLAYER_INVENTORY_SLOT_COUNT    =
            PLAYER_INVENTORY_COLUMN_COUNT * PLAYER_INVENTORY_ROW_COUNT;
    private static final int VANILLA_SLOT_COUNT             =
            HOTBAR_SLOT_COUNT + PLAYER_INVENTORY_SLOT_COUNT;
    private static final int VANILLA_FIRST_SLOT_INDEX       = 0;
    private static final int TE_INVENTORY_FIRST_SLOT_INDEX  =
            VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT;
    private static final int TE_INVENTORY_SLOT_COUNT        = 11;

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy  = stack.copy();

        if (index < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if (!moveItemStackTo(stack, TE_INVENTORY_FIRST_SLOT_INDEX,
                    TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT - 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < TE_INVENTORY_FIRST_SLOT_INDEX + TE_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(stack, VANILLA_FIRST_SLOT_INDEX,
                    VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                player, ModBlocks.ALLOY_SMELTER.get());
    }

    private void addPlayerInventory(Inventory inv) {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
}