package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class LavaTankBlockEntity extends BlockEntity {

    private int fluidAmount;          // how much lava we have (mb)
    private final int capacity = 4000; // 4 buckets

    public LavaTankBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LAVA_TANK.get(), pos, state);
    }

    // --- simple API ---

    public int getFluidAmount() {
        return fluidAmount;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setFluidAmount(int amount) {
        int clamped = Math.max(0, Math.min(capacity, amount));

        if (clamped != this.fluidAmount) {
            System.out.println("[LavaTankBE] setFluidAmount at " + this.worldPosition +
                    " old=" + this.fluidAmount + " new=" + clamped +
                    " (serverSide=" + (level != null && !level.isClientSide) + ")");

            this.fluidAmount = clamped;
            setChanged();      // mark BE dirty for saving
            syncToClient();    // **send update to client**
        }
    }

    public void addFluid(int amount) {
        setFluidAmount(this.fluidAmount + amount);
    }

    public void removeFluid(int amount) {
        setFluidAmount(this.fluidAmount - amount);
    }

    public boolean isFull() {
        return fluidAmount >= capacity;
    }

    public boolean isEmpty() {
        return fluidAmount == 0;
    }

    public float getVisualLevel() {
        if (capacity <= 0) return 0f;
        return (float) fluidAmount / (float) capacity;  // 0.0 → 1.0
    }

    // --- NBT (world save) ---

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putInt("FluidAmount", fluidAmount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        this.fluidAmount = tag.getInt("FluidAmount");
    }

    // --- Network sync (server -> client) ---

    /**
     * Called when the server wants to send BE data to the client.
     * This is used by ClientboundBlockEntityDataPacket.create(this).
     */
    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider provider) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, provider);
        return tag;
    }

    /**
     * Called client-side when an update packet is received.
     */
    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider provider) {
        loadAdditional(tag, provider);
    }

    /**
     * Packet sent when the BE is updated.
     */
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    /**
     * Helper to actually send the packet to the client.
     */
    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }
}
