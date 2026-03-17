package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.power.PowerStorage;
import com.arsenalcrafting.mod.power.PowerTier;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Power Bank block entity.
 *
 * - Stores energy up to tier capacity
 * - Each face independently configurable: IN, OUT, or DISABLED
 * - Active: on tick, pushes energy out through OUT faces
 * - Exposes IEnergyStorage per-face respecting I/O config
 * - 2-slot charge inventory: [0] discharge (bank→item), [1] charge (item→bank)
 */
public class PowerBankBlockEntity extends BlockEntity {

    // ── I/O FACE CONFIG ───────────────────────────────────────────────────────

    public enum FaceMode { IN, OUT, DISABLED }

    private final FaceMode[] faceModes = new FaceMode[6];

    // ── ENERGY ────────────────────────────────────────────────────────────────

    private final PowerStorage energy;
    private final PowerTier tier;

    // 2-slot inventory: [0]=discharge (bank→item), [1]=charge (item→bank)
    private final SimpleContainer chargeInventory = new SimpleContainer(2);

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────

    public PowerBankBlockEntity(PowerTier tier, BlockPos pos, BlockState state) {
        super(ModBlockEntities.getPowerBankType(tier), pos, state);
        this.tier = tier;
        this.energy = new PowerStorage(tier.bankCapacity(), tier.transferRate());

        // Default: all faces IN except bottom which is OUT
        for (Direction dir : Direction.values()) {
            faceModes[dir.ordinal()] = FaceMode.IN;
        }
        faceModes[Direction.DOWN.ordinal()] = FaceMode.OUT;
    }

    // ── TICK ──────────────────────────────────────────────────────────────────

    public void tickServer() {
        Level level = getLevel();
        if (level == null) return;

        // Transfer energy to/from items in charge slots — runs even when empty
        // so items can charge the bank from empty
        tickChargeSlots(level);

        if (energy.getEnergyStored() <= 0) return;

        for (Direction dir : Direction.values()) {
            if (faceModes[dir.ordinal()] != FaceMode.OUT) continue;

            BlockPos neighborPos = worldPosition.relative(dir);
            if (!level.isLoaded(neighborPos)) continue;

            // Skip cable blocks — cable network handles distribution
            if (level.getBlockState(neighborPos).getBlock()
                    instanceof com.arsenalcrafting.mod.blocks.CableBlock) continue;

            IEnergyStorage neighbor = level.getCapability(
                    Capabilities.EnergyStorage.BLOCK,
                    neighborPos,
                    level.getBlockState(neighborPos),
                    level.getBlockEntity(neighborPos),
                    dir.getOpposite()
            );

            if (neighbor == null || !neighbor.canReceive()) continue;

            int toSend = Math.min(energy.getEnergyStored(), tier.transferRate());
            int accepted = neighbor.receiveEnergy(toSend, false);
            if (accepted > 0) {
                energy.extractEnergy(accepted, false);
                setChanged();
                if (energy.getEnergyStored() <= 0) break;
            }
        }
    }

    // ── CHARGE SLOTS ──────────────────────────────────────────────────────────

    private void tickChargeSlots(Level level) {
        if (level.isClientSide) return;

        // Slot [0] (left, red minus): item → bank (drain item into bank)
        net.minecraft.world.item.ItemStack drainStack = chargeInventory.getItem(1);
        if (!drainStack.isEmpty()) {
            var itemEnergy = drainStack.getCapability(
                    Capabilities.EnergyStorage.ITEM, null);
            if (itemEnergy != null && itemEnergy.canExtract() && energy.canReceive()) {
                int toReceive = Math.min(
                        itemEnergy.getEnergyStored(),
                        energy.getMaxEnergyStored() - energy.getEnergyStored());
                toReceive = Math.min(toReceive, tier.transferRate());
                int extracted = itemEnergy.extractEnergy(toReceive, false);
                if (extracted > 0) {
                    energy.receiveEnergy(extracted, false);
                    setChanged();
                }
            }
        }

        // Slot [1] (right, green plus): bank → item (charge item from bank)
        net.minecraft.world.item.ItemStack chargeStack = chargeInventory.getItem(0);
        if (!chargeStack.isEmpty()) {
            var itemEnergy = chargeStack.getCapability(
                    Capabilities.EnergyStorage.ITEM, null);
            if (itemEnergy != null && itemEnergy.canReceive() && energy.getEnergyStored() > 0) {
                int toSend = Math.min(energy.getEnergyStored(), tier.transferRate());
                int accepted = itemEnergy.receiveEnergy(toSend, false);
                if (accepted > 0) {
                    energy.extractEnergy(accepted, false);
                    setChanged();
                }
            }
        }
    }

    // ── CAPABILITY EXPOSURE ───────────────────────────────────────────────────

    public IEnergyStorage getEnergyForSide(Direction side) {
        FaceMode mode = faceModes[side.ordinal()];
        return switch (mode) {
            case IN -> new IEnergyStorage() {
                @Override public int receiveEnergy(int max, boolean sim) { return energy.receiveEnergy(max, sim); }
                @Override public int extractEnergy(int max, boolean sim) { return 0; }
                @Override public int getEnergyStored()    { return energy.getEnergyStored(); }
                @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
                @Override public boolean canExtract()     { return false; }
                @Override public boolean canReceive()     { return true; }
            };
            case OUT -> new IEnergyStorage() {
                @Override public int receiveEnergy(int max, boolean sim) { return 0; }
                @Override public int extractEnergy(int max, boolean sim) { return energy.extractEnergy(max, sim); }
                @Override public int getEnergyStored()    { return energy.getEnergyStored(); }
                @Override public int getMaxEnergyStored() { return energy.getMaxEnergyStored(); }
                @Override public boolean canExtract()     { return true; }
                @Override public boolean canReceive()     { return false; }
            };
            case DISABLED -> null;
        };
    }

    // ── FACE CONFIG ───────────────────────────────────────────────────────────

    public FaceMode getFaceMode(Direction dir) {
        return faceModes[dir.ordinal()];
    }

    public void cycleFaceMode(Direction dir) {
        FaceMode current = faceModes[dir.ordinal()];
        faceModes[dir.ordinal()] = switch (current) {
            case IN       -> FaceMode.OUT;
            case OUT      -> FaceMode.DISABLED;
            case DISABLED -> FaceMode.IN;
        };
        setChanged();
    }

    // ── ACCESSORS ─────────────────────────────────────────────────────────────

    public SimpleContainer getChargeInventory()    { return chargeInventory; }
    public PowerStorage getEnergyStorage()         { return energy; }
    public PowerTier getTier()                     { return tier; }
    public int getEnergyStored()                   { return energy.getEnergyStored(); }
    public int getMaxEnergy()                      { return energy.getMaxEnergyStored(); }

    public float getFillFraction() {
        return (float) energy.getEnergyStored() / (float) energy.getMaxEnergyStored();
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
        tag.put("ChargeInventory", chargeInventory.createTag(registries));
        int[] modes = new int[6];
        for (Direction dir : Direction.values()) {
            modes[dir.ordinal()] = faceModes[dir.ordinal()].ordinal();
        }
        tag.putIntArray("FaceModes", modes);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setEnergy(tag.getInt("Energy"));
        if (tag.contains("ChargeInventory")) {
            chargeInventory.fromTag(tag.getList("ChargeInventory",
                    net.minecraft.nbt.Tag.TAG_COMPOUND), registries);
        }
        if (tag.contains("FaceModes")) {
            int[] modes = tag.getIntArray("FaceModes");
            FaceMode[] values = FaceMode.values();
            for (Direction dir : Direction.values()) {
                int idx = dir.ordinal();
                if (idx < modes.length) {
                    faceModes[idx] = values[Math.min(modes[idx], values.length - 1)];
                }
            }
        }
    }
}