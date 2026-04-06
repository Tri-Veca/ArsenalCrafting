package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeInput;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.fluids.MultiFluidTank;
import com.arsenalcrafting.mod.generator.AlloyRecipeDefinitions;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AlloySmelterBlockEntity extends BlockEntity implements MenuProvider {

    public Set<String> mixingBlocked = new HashSet<>();
    public Set<String> meltingBlocked = new HashSet<>();
    // Slots: 0–8 = inputs, 9 = unused (was item output — now fluid output via OUTPUT_TANK)

    public String currentRecipeType = null; // "solid" or "liquid"
    private AlloyRecipe currentSolidRecipe = null; // Changed from AlloyRecipeDefinition to AlloyRecipe
    private AlloyRecipeDefinitions.LiquidMixingRecipeDefinition currentLiquidRecipe = null;
    private int craftingProgress = 0; // Add this field for liquid recipe progress
    private int craftingTime = 0;     // Add this field for liquid recipe time
    private boolean isCrafting = false; // Add this field to track crafting state
    private int[] slotProgress = new int[9];
    private int[] slotMaxProgress = new int[9];

    public String castTargetFluid = null; // The material name of the fluid selected for casting

    private final ItemStackHandler itemHandler = new ItemStackHandler(10) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncItemsToClient();
        }

        @Override
        public int getSlotLimit(int slot) {
            if (slot < 9) return 1;
            return super.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= 9) return false;

            return AlloyRecipeDefinitions.RECIPES.values().stream()
                    .anyMatch(recipe -> recipe.ingredients().stream()
                            .anyMatch(ing -> ing instanceof Item item && item == stack.getItem()));
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

        public DisplayTank() {
            super(0);
        }

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

        @Override
        public int getCapacity() {
            return currentCapacity;
        }

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
    public final MultiFluidTank OUTPUT_TANK = new MultiFluidTank(9000) {
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

    // ── FLUID HELPER METHODS (updated to use OUTPUT_TANK) ─────────────────────

    public void drainFromOutputTank(Fluid fluid, int amount) {
      /*  System.out.println("[DEBUG] drainFromOutputTank called: " + amount + "mb of " +
                fluid.getFluidType().getDescription().getString());

       */

        int remaining = amount;
        int tanksCount = OUTPUT_TANK.getTanks();

        for (int i = 0; i < tanksCount; i++) {
            FluidStack fluidStack = OUTPUT_TANK.getFluidInTank(i);
            if (fluidStack != null && !fluidStack.isEmpty() && fluidStack.getFluid() == fluid) {
                int currentAmount = fluidStack.getAmount();
                int toDrain = Math.min(remaining, currentAmount);

                /*System.out.println("[DEBUG] Draining " + toDrain + "mb from tank " + i +
                        " (had " + currentAmount + "mb)");

                 */

                // Create a copy to drain
                FluidStack drainStack = new FluidStack(fluid, toDrain);
                OUTPUT_TANK.drain(drainStack, IFluidHandler.FluidAction.EXECUTE);

                remaining -= toDrain;

                if (remaining <= 0) break;
            }
        }

        if (remaining > 0) {
            /*System.out.println("[DEBUG] WARNING: Could only drain " + (amount - remaining) +
                    "mb of " + amount + "mb required!");

             */
        } else {
            //System.out.println("[DEBUG] Successfully drained all " + amount + "mb");
        }

        setChanged();
    }

    public void addToOutputTank(FluidStack fluidStack) {
        OUTPUT_TANK.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    public List<FluidStack> getAllOutputFluids() {
        List<FluidStack> fluids = new ArrayList<>();
        // Get all fluids from OUTPUT_TANK (MultiFluidTank handles multiple fluids)
        for (int i = 0; i < OUTPUT_TANK.getTanks(); i++) {
            FluidStack fluid = OUTPUT_TANK.getFluidInTank(i);
            if (fluid != null && !fluid.isEmpty()) {
                fluids.add(fluid.copy());
            }
        }
        return fluids;
    }

    public int getOutputFluidCapacity() {
        return OUTPUT_TANK.getCapacity(); // Use OUTPUT_TANK's capacity
    }

    public int getOutputFluidAmount() {
        return OUTPUT_TANK.getTotalAmount(); // Use MultiFluidTank's total amount
    }

    // Helper method to get total amount from output tank (for compatibility)
    public int getOutputTankTotalAmount() {
        return OUTPUT_TANK.getTotalAmount();
    }

    // Helper method to get output tank capacity (for compatibility)
    public int getOutputTankCapacity() {
        return OUTPUT_TANK.getCapacity();
    }

    // Add this to your tick method or crafting method
    public void checkAndCraftLiquidRecipe() {
        if (level == null || level.isClientSide) return;

        Optional<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> recipe = findValidLiquidRecipe();

        if (recipe.isPresent()) {
            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition liquidRecipe = recipe.get();

            // Check if we have the required fluids
            if (liquidRecipe.canCraft(getAllOutputFluids())) {
                // Consume input fluids
                for (FluidStack input : liquidRecipe.getConsumedFluids()) {
                    drainFromOutputTank(input.getFluid(), input.getAmount());
                }

                // Add output fluid
                addToOutputTank(liquidRecipe.output().copy());

                // Set crafting progress if needed
                this.craftingProgress = 0;
                this.craftingTime = liquidRecipe.craftTime();
                this.isCrafting = true;

                setChanged();
            }
        }
    }

    public Optional<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> findValidLiquidRecipe() {
        List<FluidStack> availableFluids = getAllOutputFluids();

        // Debug logging
        /*System.out.println("[DEBUG] Checking liquid recipes. Available fluids:");
        for (FluidStack fluid : availableFluids) {
            System.out.println("  - " + fluid.getAmount() + "mb " +
                    fluid.getFluid().getFluidType().getDescription().getString());
        }

         */

        Optional<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> recipe =
                AlloyRecipeDefinitions.LIQUID_MIXING_RECIPES.values().stream()
                        .filter(r -> {
                            boolean canCraft = r.canCraft(availableFluids);
                            if (canCraft) {
                                //System.out.println("[DEBUG] Recipe " + r.name() + " can craft!");
                            }
                            return canCraft;
                        })
                        .findFirst();

        if (recipe.isEmpty()) {
            //System.out.println("[DEBUG] No valid liquid recipe found");
        }

        return recipe;
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
    private int progress = 0;
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
                    default -> {
                    }
                }
            }

            @Override
            public int getCount() {
                return 3;
            }
        };
    }

    // ── PUBLIC ACCESSORS (existing + new) ─────────────────────────────────────
    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    public int getConnectedTanksCount() {
        return connectedTanks.size();
    }

    public int getTotalTankCapacity() {
        return DISPLAY_TANK.getCapacity();
    }

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

 public int getTotalLavaInTanks() {
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

    public boolean consumeLavaFromTanks(int amount) {
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
        OUTPUT_TANK.save(outputTag, registries);
        tag.put("OutputTank", outputTag);
        if (castTargetFluid != null) tag.putString("CastTarget", castTargetFluid);

        tag.putString("MeltingBlocked", String.join(",", meltingBlocked));
        tag.putString("MixingBlocked", String.join(",", mixingBlocked));
        // Save recipe type and progress
        if (currentRecipeType != null) tag.putString("RecipeType", currentRecipeType);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);

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
            OUTPUT_TANK.load(tag.getCompound("OutputTank"), registries);
        }
        if (tag.contains("RecipeType")) {
            currentRecipeType = tag.getString("RecipeType");
        }
        if (tag.contains("CastTarget")) {
            castTargetFluid = tag.getString("CastTarget");
            if (castTargetFluid.isEmpty()) castTargetFluid = null;
        }

        if (tag.contains("MixingBlocked")) {
            String raw = tag.getString("MixingBlocked");
            mixingBlocked.clear();
            if (!raw.isEmpty()) Collections.addAll(mixingBlocked, raw.split(","));
        }


        if (tag.contains("MeltingBlocked")) {
            String raw = tag.getString("MeltingBlocked");
            meltingBlocked.clear();
            if (!raw.isEmpty()) Collections.addAll(meltingBlocked, raw.split(","));
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
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
        tag.putIntArray("slotProgress", slotProgress);
        tag.putIntArray("slotMaxProgress", slotMaxProgress);
        CompoundTag tankTag = new CompoundTag();
        DISPLAY_TANK.writeToNBT(registries, tankTag);
        tag.put("DisplayTank", tankTag);
        // Save output tank
        CompoundTag outputTag = new CompoundTag();
        OUTPUT_TANK.save(outputTag, registries);
        tag.put("OutputTank", outputTag);
        if (castTargetFluid != null) tag.putString("CastTarget", castTargetFluid);

        // Save mixing blocked set
        String[] blockedArray = mixingBlocked.toArray(new String[0]);
        tag.putString("MixingBlocked", String.join(",", blockedArray));
        tag.putString("MeltingBlocked", String.join(",", meltingBlocked));
        // Save recipe type and current recipe info
        if (currentRecipeType != null) tag.putString("RecipeType", currentRecipeType);
        tag.putInt("craftingProgress", craftingProgress);
        tag.putInt("craftingTime", craftingTime);
        tag.putBoolean("isCrafting", isCrafting);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inv"));
        progress = tag.getInt("prog");
        maxProgress = tag.getInt("maxprog");
        if (tag.contains("DisplayTank")) {
            DISPLAY_TANK.readFromNBT(registries, tag.getCompound("DisplayTank"));
        }
        if (tag.contains("OutputTank")) {
            OUTPUT_TANK.load(tag.getCompound("OutputTank"), registries);
        }
        if (tag.contains("RecipeType")) {
            currentRecipeType = tag.getString("RecipeType");
        }
        // Load mixing blocked set
        if (tag.contains("MixingBlocked")) {
            String raw = tag.getString("MixingBlocked");
            mixingBlocked.clear();
            if (!raw.isEmpty()) {
                Collections.addAll(mixingBlocked, raw.split(","));
            }
        }
        if (tag.contains("CastTarget")) {
            castTargetFluid = tag.getString("CastTarget");
            if (castTargetFluid.isEmpty()) castTargetFluid = null;
        }

        if (tag.contains("MeltingBlocked")) {
            String raw = tag.getString("MeltingBlocked");
            meltingBlocked.clear();
            if (!raw.isEmpty()) Collections.addAll(meltingBlocked, raw.split(","));
        }

        if (tag.contains("slotProgress")) slotProgress = tag.getIntArray("slotProgress");
        if (tag.contains("slotMaxProgress")) slotMaxProgress = tag.getIntArray("slotMaxProgress");
        craftingProgress = tag.getInt("craftingProgress");
        craftingTime = tag.getInt("craftingTime");
        isCrafting = tag.getBoolean("isCrafting");
    }

    // ── TICK (updated for both liquid and solid recipes) ─────────────────────────────

    public static void tick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity be) {
        if (level.isClientSide) return;

        boolean needRefresh = false;
        for (ConnectedTankInfo info : be.connectedTanks) {
            if (!(info.handler instanceof FluidTank)) {
                needRefresh = true;
                break;
            }
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




        // First check for liquid mixing recipes (if not currently crafting a solid recipe)
        if (be.currentRecipeType == null || be.currentRecipeType.equals("solid")) {
            // Only start a new recipe if not already crafting
            if (be.progress == 0) {  // Add this check
                Optional<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> liquidRecipe = be.findValidLiquidRecipe();

                if (liquidRecipe.isPresent()) {
                    AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe = liquidRecipe.get();

                    // Check if we have enough room in output tank
                    int resultMb = recipe.output().getAmount();
                    int consumedMb = recipe.getConsumedFluids().stream().mapToInt(FluidStack::getAmount).sum();
                    if (be.OUTPUT_TANK.getTotalAmount() - consumedMb + resultMb > be.OUTPUT_TANK.getCapacity()) {
                        be.progress = 0;
                        setChanged(level, pos, state);
                        return;
                    }

                    // Check if the OUTPUT material is blocked from mixing
                    AlloyMaterial outputMat = AlloyMaterial.fromFluid(recipe.output().getFluid());
                    if (outputMat != null && be.mixingBlocked.contains(outputMat.getName())) {
                        be.progress = 0;
                        be.currentRecipeType = null;
                        be.currentLiquidRecipe = null;
                        setChanged(level, pos, state);
                        return;
                    }

                    // Set recipe type and max progress
                    be.currentRecipeType = "liquid";
                    be.maxProgress = recipe.craftTime();
                    be.currentLiquidRecipe = recipe;
                    be.progress = 1; // Start at 1, not incrementing immediately

                    setChanged(level, pos, state);
                    return;
                }
            }


        }

        // If we're currently crafting a liquid recipe, continue it
        if (be.currentRecipeType != null && be.currentRecipeType.equals("liquid") && be.currentLiquidRecipe != null) {
            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe = be.currentLiquidRecipe;

            // Check if the OUTPUT material is blocked from mixing
            AlloyMaterial outputMat = AlloyMaterial.fromFluid(recipe.output().getFluid());
            if (outputMat != null && be.mixingBlocked.contains(outputMat.getName())) {
                be.progress = 0;
                be.currentRecipeType = null;
                be.currentLiquidRecipe = null;
                setChanged(level, pos, state);
                return;
            }
            // Check if we still have the required fluids (in case they were drained externally)
            if (!recipe.canCraft(be.getAllOutputFluids())) {
                // Recipe is no longer valid, cancel crafting
                be.progress = 0;
                be.currentRecipeType = null;
                be.currentLiquidRecipe = null;
                be.isCrafting = false;
                setChanged(level, pos, state);
                return;
            }

            be.progress++;

            if (be.progress >= be.maxProgress) {
                craftLiquidRecipe(be);
                be.progress = 0;
                be.currentRecipeType = null;
                be.currentLiquidRecipe = null;
                be.isCrafting = false;
            }
            setChanged(level, pos, state);
            return;
        }

        // If no liquid recipe, check solid recipes


        // Process each input slot independently
        for (int slotIndex = 0; slotIndex < 9; slotIndex++) {

            ItemStack slotItem = be.itemHandler.getStackInSlot(slotIndex);
            if (slotItem.isEmpty()) {
                be.slotProgress[slotIndex] = 0;
                be.slotMaxProgress[slotIndex] = 0;
                continue;
            }
            // Check if this material's melting is blocked
            AlloyMaterial slotMat = AlloyMaterial.fromItem(slotItem.getItem());
            if (slotMat != null && be.meltingBlocked.contains(slotMat.getName())) {
                be.slotProgress[slotIndex] = 0;
                be.slotMaxProgress[slotIndex] = 0;
                continue;
            }



            SimpleContainer singleInv = new SimpleContainer(9);
            singleInv.setItem(0, slotItem);

            var recipeOpt = level.getRecipeManager().getRecipeFor(
                    AlloyRecipeType.ALLOY_SMELTING.get(),
                    new AlloyRecipeInput(singleInv), level);

            if (recipeOpt.isEmpty()) {
                be.slotProgress[slotIndex] = 0;
                be.slotMaxProgress[slotIndex] = 0;
                continue;
            }

            var recipe = recipeOpt.get().value();

            // Set max progress if not already set
            if (be.slotMaxProgress[slotIndex] == 0) {
                be.slotMaxProgress[slotIndex] = recipe.getCraftTime();
            }

            // Check output tank has room
            int resultMb = recipe.getResultAmountMb();
            if (be.OUTPUT_TANK.getTotalAmount() + resultMb > be.OUTPUT_TANK.getCapacity()) continue;


            // Check lava availability on first tick
            if (be.slotProgress[slotIndex] == 0) {
                if (be.getTotalLavaInTanks() < recipe.getLavaCost()) continue;
            }

// Drain lava gradually across craft time
            int totalLavaNeeded = recipe.getLavaCost();
            int totalTicks = be.slotMaxProgress[slotIndex];
            int baseLavaPerTick = totalLavaNeeded / totalTicks;
            int remainder = totalLavaNeeded % totalTicks;
            int totalLavaAvailable = be.getTotalLavaInTanks();
            int lavaUsedThisTick = 0;
            int lavaToConsume = baseLavaPerTick + (be.slotProgress[slotIndex] < remainder ? 1 : 0);
            if (lavaUsedThisTick + lavaToConsume > totalLavaAvailable) {
                continue; // Skip this slot, not enough lava
            }
            if (!be.consumeLavaFromTanks(lavaToConsume)) continue;
            lavaUsedThisTick += lavaToConsume;

            be.slotProgress[slotIndex]++;



            if (be.slotProgress[slotIndex] >= be.slotMaxProgress[slotIndex]) {
                // Craft
                be.itemHandler.extractItem(slotIndex, 1, false);
                AlloyMaterial resultMat = AlloyMaterial.fromName(recipe.getResultMaterial());
                if (resultMat != null) {
                    var sourceFluid = ModFluids.getSource(resultMat);
                    if (sourceFluid != null) {
                        be.OUTPUT_TANK.fill(
                                new FluidStack(sourceFluid, resultMb),
                                IFluidHandler.FluidAction.EXECUTE);
                    }
                }
                be.slotProgress[slotIndex] = 0;
                be.slotMaxProgress[slotIndex] = 0;
            }
        }

        be.currentRecipeType = null;
        be.currentSolidRecipe = null;
        setChanged(level, pos, state);
    }

    // In AlloySmelterBlockEntity.java - update craftLiquidRecipe
    private static void craftLiquidRecipe(AlloySmelterBlockEntity be) {
        if (be.currentLiquidRecipe != null) {
            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe = be.currentLiquidRecipe;

            //System.out.println("[DEBUG] Crafting liquid recipe: " + recipe.name());
            //System.out.println("[DEBUG] Before consumption - Output tank contents:");
            for (FluidStack fluid : be.getAllOutputFluids()) {
               /*System.out.println("  - " + fluid.getAmount() + "mb " +
                        fluid.getFluid().getFluidType().getDescription().getString());

                */
            }

            // Consume input fluids
            for (FluidStack input : recipe.getConsumedFluids()) {
                /*System.out.println("[DEBUG] Consuming: " + input.getAmount() + "mb " +
                        input.getFluid().getFluidType().getDescription().getString());

                 */
                be.drainFromOutputTank(input.getFluid(), input.getAmount());
            }

            // Add output fluid
            /*System.out.println("[DEBUG] Adding output: " + recipe.output().getAmount() + "mb " +
                    recipe.output().getFluid().getFluidType().getDescription().getString());

             */
            be.addToOutputTank(recipe.output().copy());

            //System.out.println("[DEBUG] After consumption - Output tank contents:");
            for (FluidStack fluid : be.getAllOutputFluids()) {
                /*System.out.println("  - " + fluid.getAmount() + "mb " +
                        fluid.getFluid().getFluidType().getDescription().getString());

                 */
            }

            be.setChanged();
        }
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
        // In craftItem() - check what fluid is being created
        AlloyMaterial resultMat = AlloyMaterial.fromName(recipe.getResultMaterial());
        if (resultMat != null) {
            var sourceFluid = ModFluids.getSource(resultMat);
            if (sourceFluid != null) {
                FluidStack output = new FluidStack(sourceFluid, recipe.getResultAmountMb());
                System.out.println("[DEBUG] Creating fluid: " + output.getAmount() + "mb " +
                        output.getFluid().getFluidType().getDescription().getString());
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