package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.blocks.entity.GeneratorBlockEntity;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fossil Generator container menu.
 * One fuel slot + energy/burn sync via DataSlots.
 */
public class FossilGeneratorMenu extends AbstractContainerMenu {

    private final GeneratorBlockEntity blockEntity;

    // ── DATA SLOTS ────────────────────────────────────────────────────────────
    // Energy stored (split high/low for large values)
    private final DataSlot energyHigh    = DataSlot.standalone();
    private final DataSlot energyLow     = DataSlot.standalone();
    private final DataSlot maxHigh       = DataSlot.standalone();
    private final DataSlot maxLow        = DataSlot.standalone();
    // Burn progress (0 = not burning, > 0 = burning)
    private final DataSlot burnRemaining = DataSlot.standalone();
    private final DataSlot burnTotal     = DataSlot.standalone();

    // ── CLIENT CONSTRUCTOR ────────────────────────────────────────────────────
    public FossilGeneratorMenu(int id, Inventory playerInventory, FriendlyByteBuf buf) {
        this(id, playerInventory, getBlockEntity(playerInventory, buf));
    }

    private static GeneratorBlockEntity getBlockEntity(Inventory inv, FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inv.player.level().getBlockEntity(pos);
        if (be instanceof GeneratorBlockEntity gen) return gen;
        throw new IllegalStateException("No GeneratorBlockEntity at " + pos);
    }

    // ── SERVER CONSTRUCTOR ────────────────────────────────────────────────────
    public FossilGeneratorMenu(int id, Inventory playerInventory, GeneratorBlockEntity blockEntity) {
        super(ModMenus.FOSSIL_GENERATOR.get(), id);
        this.blockEntity = blockEntity;

        Container fuelInv = blockEntity.getFuelInventory();

        // Fuel slot — x=80, y=38 from texture analysis
        addSlot(new Slot(fuelInv, 0, 80, 38) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return com.arsenalcrafting.mod.power.FuelRegistry.isFossilFuel(stack);
            }
        });

        // Player inventory (3 rows) — y=84,102,120
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        // Player hotbar — y=142
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        // Register data slots
        addDataSlot(energyHigh);
        addDataSlot(energyLow);
        addDataSlot(maxHigh);
        addDataSlot(maxLow);
        addDataSlot(burnRemaining);
        addDataSlot(burnTotal);
    }

    // ── DATA SYNC ─────────────────────────────────────────────────────────────

    @Override
    public void broadcastChanges() {
        int energy = blockEntity.getEnergyStored();
        int max    = blockEntity.getMaxEnergy();

        energyHigh.set((energy >> 16) & 0xFFFF);
        energyLow.set(energy & 0xFFFF);
        maxHigh.set((max >> 16) & 0xFFFF);
        maxLow.set(max & 0xFFFF);
        burnRemaining.set(Math.min(blockEntity.getBurnTimeRemaining(), Short.MAX_VALUE));
        burnTotal.set(Math.min(blockEntity.getBurnTimeTotalForCurrentFuel(), Short.MAX_VALUE));

        super.broadcastChanges();
    }

    // ── ACCESSORS ─────────────────────────────────────────────────────────────

    public int getEnergyStored() {
        return ((energyHigh.get() & 0xFFFF) << 16) | (energyLow.get() & 0xFFFF);
    }

    public int getMaxEnergy() {
        return ((maxHigh.get() & 0xFFFF) << 16) | (maxLow.get() & 0xFFFF);
    }

    public float getEnergyFraction() {
        int max = getMaxEnergy();
        return max <= 0 ? 0f : (float) getEnergyStored() / max;
    }

    /** 0.0–1.0 fraction of current fuel item burned (1.0 = full, 0.0 = empty/not burning) */
    public float getBurnFraction() {
        int total = burnTotal.get();
        if (total <= 0) return 0f;
        return (float) burnRemaining.get() / total;
    }

    public boolean isBurning() {
        return burnRemaining.get() > 0;
    }

    public GeneratorBlockEntity getBlockEntity() { return blockEntity; }

    // ── QUICK MOVE ────────────────────────────────────────────────────────────

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot == null || !slot.hasItem()) return result;

        ItemStack slotStack = slot.getItem();
        result = slotStack.copy();

        // From fuel slot → player inventory
        if (index == 0) {
            if (!moveItemStackTo(slotStack, 1, 37, true)) return ItemStack.EMPTY;
        }
        // From player inventory → fuel slot
        else if (com.arsenalcrafting.mod.power.FuelRegistry.isFossilFuel(slotStack)) {
            if (!moveItemStackTo(slotStack, 0, 1, false)) return ItemStack.EMPTY;
        }

        if (slotStack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity.getLevel() != null &&
                AbstractContainerMenu.stillValid(
                        ContainerLevelAccess.create(
                                blockEntity.getLevel(), blockEntity.getBlockPos()),
                        player,
                        blockEntity.getBlockState().getBlock());
    }
}