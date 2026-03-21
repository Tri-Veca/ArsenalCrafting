package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeInput;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.network.SyncAlloySmelterCapacityPacket;
import com.arsenalcrafting.mod.network.SyncAlloySmelterFluidPacket;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

public class AlloySmelterBlockEntity extends BlockEntity implements MenuProvider {

    // Slots: 0–8 = inputs, 9 = unused (was item output — now fluid output via OUTPUT_TANK)
    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncItemsToClient();
        }
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 9) return false; // slot 9 disabled
            return true;
        }
    };

    private void syncItemsToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void drops() {
        if (this.level == null) return;
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // ── LAVA DISPLAY TANK (existing — unchanged) ──────────────────────────────
    public final DisplayTank DISPLAY_TANK = new DisplayTank();

    public class DisplayTank extends FluidTank {
        private int currentCapacity = 0;

        public DisplayTank() { super(0); }

        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.LAVA;
        }

        public void updateCapacity(int newCapacity) {
            if (this.currentCapacity != newCapacity) {
                this.currentCapacity = newCapacity;
                if (getFluidAmount() > newCapacity) {
                    setFluid(new FluidStack(Fluids.LAVA, newCapacity));
                }
                setChanged();
                if (level != null && !level.isClientSide) {
                    level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                    PacketHandler.sendToAllTracking(
                            new SyncAlloySmelterCapacityPacket(worldPosition, currentCapacity),
                            level, worldPosition);
                }
            }
        }

        @Override public int getCapacity() { return currentCapacity; }

        @Override
        public CompoundTag writeToNBT(HolderLookup.Provider provider, CompoundTag tag) {
            super.writeToNBT(provider, tag);
            tag.putInt("Capacity", currentCapacity);
            return tag;
        }

        @Override
        public FluidTank readFromNBT(HolderLookup.Provider provider, CompoundTag tag) {
            super.readFromNBT(provider, tag);
            if (tag.contains("Capacity")) this.currentCapacity = tag.getInt("Capacity");
            return this;
        }
    }

    // ── OUTPUT TANK (new — holds molten alloy produced by recipes) ────────────
    public final FluidTank OUTPUT_TANK = new FluidTank(8000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncOutputTankToClient();
        }
        @Override
        public boolean isFluidValid(FluidStack stack) {
            // Accept any alloy fluid — validated at fill time
            return true;
        }
    };

    private void syncOutputTankToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            // Re-use existing fluid sync packet with a different position offset if needed,
            // or add a new packet. For now: block update triggers getUpdateTag() sync.
        }
    }

    // ── CONNECTED TANKS (existing — unchanged) ────────────────────────────────
    private final List<ConnectedTankInfo> connectedTanks = new ArrayList<>();
    private int refreshCooldown = 0;
    private static final int REFRESH_RATE = 20;

    private static class ConnectedTankInfo {
        final IFluidHandler handler;
        final int capacity;
        int lastKnownAmount;

        ConnectedTankInfo(IFluidHandler handler, int capacity) {
            this.handler = handler;
            this.capacity = capacity;
            this.lastKnownAmount = handler.getFluidInTank(0).getAmount();
        }
    }

    // ── PROGRESS (existing — unchanged) ───────────────────────────────────────
    private int progress    = 0;
    private int maxProgress = 200;

    protected final ContainerData data;

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER.get(), pos, state);
        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> connectedTanks.size();
                    default -> 0;
                };
            }
            @Override
            public void set(int i, int v) {
                switch (i) {
                    case 0 -> progress = v;
                    case 1 -> maxProgress = v;
                    default -> {}
                }
            }
            @Override
            public int getCount() { return 3; }
        };
    }

    // ── PUBLIC ACCESSORS (existing + new) ─────────────────────────────────────
    public ItemStackHandler getItemHandler()       { return itemHandler; }
    public ContainerData getData()                 { return data; }
    public int getConnectedTanksCount()            { return connectedTanks.size(); }
    public int getTotalTankCapacity()              { return DISPLAY_TANK.getCapacity(); }

    public List<String> getConnectedTanksInfo() {
        List<String> info = new ArrayList<>();
        for (ConnectedTankInfo tank : connectedTanks) {
            FluidStack fluid = tank.handler.getFluidInTank(0);
            info.add(String.format("Capacity: %d, Contains: %d mb %s",
                    tank.capacity, fluid.getAmount(),
                    fluid.isEmpty() ? "empty" : fluid.getFluid().toString()));
        }
        return info;
    }

    // ── FLUID SYNC (existing — unchanged) ────────────────────────────────────
    private void syncFluidToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            PacketHandler.sendToAllTracking(
                    new SyncAlloySmelterFluidPacket(worldPosition, DISPLAY_TANK.getFluid()),
                    level, worldPosition);
        }
    }

    // ── TANK SCANNING (existing — unchanged) ─────────────────────────────────
    private void refreshConnectedTanks() {
        if (level == null || level.isClientSide) return;
        List<ConnectedTankInfo> newTanks = new ArrayList<>();
        int totalCapacity = 0;

        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = worldPosition.relative(dir);
            BlockEntity be = level.getBlockEntity(neighborPos);
            if (be instanceof LavaTankBlockEntity lavaTank) {
                int cap = lavaTank.fluidTank.getCapacity();
                newTanks.add(new ConnectedTankInfo(lavaTank.fluidTank, cap));
                totalCapacity += cap;
                continue;
            }
            IFluidHandler neighborTank = level.getCapability(
                    net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.BLOCK,
                    neighborPos, level.getBlockState(neighborPos), this, dir.getOpposite());
            if (neighborTank != null && neighborTank != DISPLAY_TANK) {
                int cap = neighborTank.getTankCapacity(0);
                newTanks.add(new ConnectedTankInfo(neighborTank, cap));
                totalCapacity += cap;
            }
        }

        boolean changed = connectedTanks.size() != newTanks.size();
        if (!changed) {
            for (int i = 0; i < connectedTanks.size(); i++) {
                if (connectedTanks.get(i).handler != newTanks.get(i).handler) {
                    changed = true;
                    break;
                }
            }
        }
        if (changed) {
            connectedTanks.clear();
            connectedTanks.addAll(newTanks);
            DISPLAY_TANK.updateCapacity(totalCapacity);
            updateDisplayTank();
        }
    }

    private int getTotalLavaInTanks() {
        int total = 0;
        for (ConnectedTankInfo info : connectedTanks) {
            FluidStack fluid = info.handler.getFluidInTank(0);
            if (fluid.getFluid() == Fluids.LAVA) {
                total += fluid.getAmount();
                info.lastKnownAmount = fluid.getAmount();
            }
        }
        return total;
    }

    private void updateDisplayTank() {
        if (level == null || level.isClientSide) return;
        int totalLava = getTotalLavaInTanks();
        int totalCapacity = 0;
        for (ConnectedTankInfo info : connectedTanks) totalCapacity += info.capacity;
        if (DISPLAY_TANK.getCapacity() != totalCapacity) DISPLAY_TANK.updateCapacity(totalCapacity);
        FluidStack current = DISPLAY_TANK.getFluid();
        if (current.getAmount() != totalLava) {
            DISPLAY_TANK.setFluid(totalLava > 0
                    ? new FluidStack(Fluids.LAVA, totalLava)
                    : FluidStack.EMPTY);
            syncFluidToClient();
        }
    }

    private boolean consumeLavaFromTanks(int amount) {
        if (level == null || level.isClientSide) return false;
        int remaining = amount;
        for (ConnectedTankInfo info : connectedTanks) {
            if (remaining <= 0) break;
            FluidStack available = info.handler.drain(remaining, IFluidHandler.FluidAction.SIMULATE);
            if (!available.isEmpty() && available.getFluid() == Fluids.LAVA) {
                int drain = Math.min(remaining, available.getAmount());
                info.handler.drain(drain, IFluidHandler.FluidAction.EXECUTE);
                remaining -= drain;
            }
        }
        updateDisplayTank();
        return remaining == 0;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) refreshConnectedTanks();
    }

    public void onNeighborChange(BlockPos neighborPos) {
        if (level == null || level.isClientSide) return;
        refreshConnectedTanks();
        setChanged();
        level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arsenalcrafting.alloy_smelter");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AlloySmelterMenu(id, inv, this, data);
    }

    // ── NBT (existing + OUTPUT_TANK added) ────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        DISPLAY_TANK.writeToNBT(registries, tag);
        tag.putInt("TankCount", connectedTanks.size());
        tag.put("inv", itemHandler.serializeNBT(registries));
        // Output tank sync
        CompoundTag outputTag = new CompoundTag();
        OUTPUT_TANK.writeToNBT(registries, outputTag);
        tag.put("OutputTank", outputTag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        DISPLAY_TANK.readFromNBT(registries, tag);
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (tag.contains("inv")) {
            itemHandler.deserializeNBT(registries, tag);
        }
        if (tag.contains("OutputTank")) {
            OUTPUT_TANK.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv", itemHandler.serializeNBT(registries));
        tag.putInt("prog", progress);
        tag.putInt("maxprog", maxProgress);
        CompoundTag tankTag = new CompoundTag();
        DISPLAY_TANK.writeToNBT(registries, tankTag);
        tag.put("DisplayTank", tankTag);
        // Save output tank
        CompoundTag outputTag = new CompoundTag();
        OUTPUT_TANK.writeToNBT(registries, outputTag);
        tag.put("OutputTank", outputTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inv"));
        progress    = tag.getInt("prog");
        maxProgress = tag.getInt("maxprog");
        if (tag.contains("DisplayTank")) {
            DISPLAY_TANK.readFromNBT(registries, tag.getCompound("DisplayTank"));
        }
        if (tag.contains("OutputTank")) {
            OUTPUT_TANK.readFromNBT(registries, tag.getCompound("OutputTank"));
        }
    }

    // ── TICK (existing, updated for fluid output) ─────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity be) {
        if (level.isClientSide) return;

        boolean needRefresh = false;
        for (ConnectedTankInfo info : be.connectedTanks) {
            if (!(info.handler instanceof FluidTank)) { needRefresh = true; break; }
        }
        if (needRefresh) be.refreshConnectedTanks();

        be.refreshCooldown--;
        if (be.refreshCooldown <= 0) {
            be.refreshConnectedTanks();
            be.refreshCooldown = REFRESH_RATE;
        }
        be.updateDisplayTank();

        SimpleContainer inv = new SimpleContainer(be.itemHandler.getSlots());
        for (int i = 0; i < be.itemHandler.getSlots(); i++) {
            inv.setItem(i, be.itemHandler.getStackInSlot(i));
        }

        var recipeOpt = level.getRecipeManager().getRecipeFor(
                AlloyRecipeType.ALLOY_SMELTING.get(),
                new AlloyRecipeInput(inv), level);

        if (recipeOpt.isPresent()) {
            var recipe = recipeOpt.get().value();

            // Check output tank has room for the result
            int resultMb = recipe.getResultAmountMb();
            if (be.OUTPUT_TANK.getFluidAmount() + resultMb > be.OUTPUT_TANK.getCapacity()) {
                be.progress = 0;
                return;
            }

            be.maxProgress = recipe.getCraftTime();
            int totalLavaNeeded = recipe.getLavaCost();
            int totalTicks      = recipe.getCraftTime();

            if (be.progress == 0) {
                int totalAvailable = be.getTotalLavaInTanks();
                if (totalAvailable < totalLavaNeeded) return;
            }

            int baseLavaPerTick = totalLavaNeeded / totalTicks;
            int remainder       = totalLavaNeeded % totalTicks;
            int lavaToConsume   = baseLavaPerTick + (be.progress < remainder ? 1 : 0);

            if (!be.consumeLavaFromTanks(lavaToConsume)) {
                be.progress = 0;
                return;
            }
            be.progress++;

            if (be.progress >= be.maxProgress) {
                craftItem(be);
                be.progress = 0;
            }
        } else {
            be.progress = 0;
        }
        setChanged(level, pos, state);
    }

    private static void craftItem(AlloySmelterBlockEntity be) {
        Level level = be.level;

        SimpleContainer inv = new SimpleContainer(be.itemHandler.getSlots());
        for (int i = 0; i < be.itemHandler.getSlots(); i++) {
            inv.setItem(i, be.itemHandler.getStackInSlot(i));
        }

        var recipeOpt = level.getRecipeManager().getRecipeFor(
                AlloyRecipeType.ALLOY_SMELTING.get(),
                new AlloyRecipeInput(inv), level);
        if (recipeOpt.isEmpty()) return;

        var recipe = recipeOpt.get().value();

        // Consume inputs (slots 0-8) — unchanged
        for (int i = 0; i < 9; i++) {
            be.itemHandler.extractItem(i, 1, false);
        }

        // ── FLUID OUTPUT (new) ────────────────────────────────────────────────
        AlloyMaterial resultMat = AlloyMaterial.fromName(recipe.getResultMaterial());
        if (resultMat != null) {
            var sourceFluid = ModFluids.getSource(resultMat);
            if (sourceFluid != null) {
                FluidStack output = new FluidStack(sourceFluid, recipe.getResultAmountMb());
                be.OUTPUT_TANK.fill(output, IFluidHandler.FluidAction.EXECUTE);
            }
        }

        // ── OLD ITEM OUTPUT (commented out — uncomment to revert) ────────────
        // ItemStack result = recipe.get().value().assemble(new AlloyRecipeInput(inv), level.registryAccess());
        // ItemStack out = be.itemHandler.getStackInSlot(9);
        // if (out.isEmpty()) {
        //     be.itemHandler.setStackInSlot(9, result.copy());
        // } else {
        //     out.grow(result.getCount());
        // }

        be.setChanged();
        if (be.level != null && !be.level.isClientSide) {
            be.level.sendBlockUpdated(be.worldPosition, be.getBlockState(), be.getBlockState(), 3);
        }
    }
}