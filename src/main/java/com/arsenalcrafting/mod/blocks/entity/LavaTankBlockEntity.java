package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.blocks.LavaTankBlock;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModFluids;
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

    public final FluidTank fluidTank = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncToClient();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.LAVA || stack.getFluid() == Fluids.FLOWING_LAVA;
        }
    };
    public void updateTier(com.arsenalcrafting.mod.blocks.LavaTankBlock.TankTier tier) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof LavaTankBlock)) return;
        if (state.getValue(LavaTankBlock.TIER) == tier) return;
        level.setBlock(worldPosition, state.setValue(LavaTankBlock.TIER, tier), 3);
    }

    @Override
    public void onLoad() {
        super.onLoad();

        com.arsenalcrafting.mod.core.smeltery.SmelteryDebug.log(
                "Tank",
                "onLoad at " + worldPosition
        );
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        com.arsenalcrafting.mod.core.smeltery.SmelteryDebug.log(
                "Tank",
                "removed at " + worldPosition
        );
    }

    public LavaTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAVA_TANK.get(), pos, state);
    }

    public int getFluidAmount() {
        return fluidTank.getFluidAmount();
    }

    public int getCapacity() {
        return fluidTank.getCapacity();
    }

    public void setFluidAmount(int amount) {
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

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.put("FluidTank", fluidTank.writeToNBT(provider, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        if (tag.contains("FluidTank")) {
            fluidTank.readFromNBT(provider, tag.getCompound("FluidTank"));
        }
    }

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