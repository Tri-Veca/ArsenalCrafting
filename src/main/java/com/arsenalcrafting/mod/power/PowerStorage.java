package com.arsenalcrafting.mod.power;

import net.neoforged.neoforge.energy.IEnergyStorage;

public class PowerStorage implements IEnergyStorage {

    private int energy;
    private int capacity;
    private int maxReceive;
    private int maxExtract;

    public PowerStorage(int capacity, int transfer) {
        this.capacity = capacity;
        this.maxReceive = transfer;
        this.maxExtract = transfer;
    }

    public void setEnergy(int energy) {
        this.energy = Math.min(capacity, energy);
    }

    @Override
    public int receiveEnergy(int maxReceive, boolean simulate) {

        int received = Math.min(capacity - energy, Math.min(this.maxReceive, maxReceive));

        if (!simulate) {
            energy += received;
        }

        return received;
    }

    @Override
    public int extractEnergy(int maxExtract, boolean simulate) {

        int extracted = Math.min(energy, Math.min(this.maxExtract, maxExtract));

        if (!simulate) {
            energy -= extracted;
        }

        return extracted;
    }

    @Override
    public int getEnergyStored() {
        return energy;
    }

    @Override
    public int getMaxEnergyStored() {
        return capacity;
    }

    @Override
    public boolean canExtract() {
        return maxExtract > 0;
    }

    @Override
    public boolean canReceive() {
        return maxReceive > 0;
    }
}