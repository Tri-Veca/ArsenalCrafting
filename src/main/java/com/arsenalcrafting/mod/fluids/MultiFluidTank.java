package com.arsenalcrafting.mod.fluids;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;




public abstract class MultiFluidTank implements IFluidHandler {
    private final Map<Fluid, Integer> fluids = new HashMap<>();
    private final int capacity;

    public MultiFluidTank(int capacity) {
        this.capacity = capacity;
    }

    @Override
    public int getTanks() {
        return fluids.size();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        List<FluidStack> allFluids = getAllFluids();
        if (tank >= 0 && tank < allFluids.size()) {
            return allFluids.get(tank);
        }
        return FluidStack.EMPTY;
    }

    @Override
    public int getTankCapacity(int tank) {
        return capacity;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return true;
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return 0;

        int currentAmount = fluids.getOrDefault(resource.getFluid(), 0);
        int totalAmount = getTotalAmount();
        int space = capacity - totalAmount;

        if (space <= 0) return 0;

        int fillAmount = Math.min(resource.getAmount(), space);

        if (action.execute()) {
            fluids.put(resource.getFluid(), currentAmount + fillAmount);
        }

        return fillAmount;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (resource.isEmpty()) return FluidStack.EMPTY;
        return drain(resource.getFluid(), resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        // Drain from first fluid (could be customized)
        if (fluids.isEmpty()) return FluidStack.EMPTY;
        Fluid firstFluid = fluids.keySet().iterator().next();
        return drain(firstFluid, maxDrain, action);
    }

    public FluidStack drain(Fluid fluid, int amount, FluidAction action) {
        int currentAmount = fluids.getOrDefault(fluid, 0);
        if (currentAmount <= 0) return FluidStack.EMPTY;

        int drainAmount = Math.min(amount, currentAmount);

        if (action.execute()) {
            if (drainAmount >= currentAmount) {
                fluids.remove(fluid);
            } else {
                fluids.put(fluid, currentAmount - drainAmount);
            }
        }

        return new FluidStack(fluid, drainAmount);
    }

    public List<FluidStack> getAllFluids() {
        return fluids.entrySet().stream()
                .map(e -> new FluidStack(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    public int getTotalAmount() {
        return fluids.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int getCapacity() {
        return capacity;
    }

    public boolean isEmpty() {
        return fluids.isEmpty();
    }

    public void clear() {
        fluids.clear();
    }

    // NBT serialization
    public void save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag fluidsTag = new CompoundTag();
        for (Map.Entry<Fluid, Integer> entry : fluids.entrySet()) {
            String fluidId = entry.getKey().builtInRegistryHolder().key().location().toString();
            fluidsTag.putInt(fluidId, entry.getValue());
        }
        tag.put("Fluids", fluidsTag);
    }

    public void load(CompoundTag tag, HolderLookup.Provider registries) {
        fluids.clear();
        if (!tag.contains("Fluids")) return;

        CompoundTag fluidsTag = tag.getCompound("Fluids");
        for (String key : fluidsTag.getAllKeys()) {
            ResourceLocation fluidLoc = ResourceLocation.parse(key);
            Fluid fluid = BuiltInRegistries.FLUID.get(fluidLoc);
            fluids.put(fluid, fluidsTag.getInt(key));
        }
    }

    protected abstract void onContentsChanged();

    public abstract boolean isFluidValid(FluidStack stack);
}