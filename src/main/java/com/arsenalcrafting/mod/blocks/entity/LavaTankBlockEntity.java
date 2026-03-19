package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

public class LavaTankBlockEntity extends BlockEntity {

    // Use NeoForge's FluidTank instead of custom int
    public final FluidTank fluidTank = new FluidTank(8000) { // 4 buckets
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncToClient();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            // Only allow lava
            return stack.getFluid() == Fluids.LAVA || stack.getFluid() == Fluids.FLOWING_LAVA;
        }
    };

    public LavaTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAVA_TANK.get(), pos, state);
    }

    // --- Convenience methods (optional, but keep for compatibility) ---

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getCapacity() {
        return fluidTank.getCapacity();
    }

    public void setFluidAmount(int amount) {
        // This is for compatibility - creates a new FluidStack
        FluidStack newStack = new FluidStack(Fluids.LAVA, Math.min(amount, getCapacity()));
        fluidTank.setFluid(newStack);
    }

    public void addFluid(int amount) {
        fluidTank.fill(new FluidStack(Fluids.LAVA, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    public void removeFluid(int amount) {
        fluidTank.drain(amount, IFluidHandler.FluidAction.EXECUTE);
    }

    public boolean isFull() {
        return fluidTank.getFluidAmount() >= fluidTank.getCapacity();
    }

    public boolean isEmpty() {
        return fluidTank.isEmpty();
    }

    public float getVisualLevel() {
        if (fluidTank.getCapacity() <= 0) return 0f;
        return (float) fluidTank.getFluidAmount() / (float) fluidTank.getCapacity();
    }

    public FluidStack getFluid() {
        return fluidTank.getFluid();
    }

    // --- NBT (world save) using FluidTank's built-in NBT methods ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        // Use FluidTank's built-in NBT serialization
        tag.put("FluidTank", fluidTank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // Use FluidTank's built-in NBT deserialization
        if (tag.contains("FluidTank")) {
            fluidTank.readFromNBT(provider, tag.getCompound("FluidTank"));
        }
    }

    // --- Network sync (server -> client) ---

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}