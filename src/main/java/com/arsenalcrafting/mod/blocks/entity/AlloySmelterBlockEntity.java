package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.blocks.AlloySmelterBlock;
import com.arsenalcrafting.mod.blocks.LavaTankBlock;
import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipe;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeInput;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.core.smeltery.SmelteryData;
import com.arsenalcrafting.mod.core.smeltery.SmelteryDebug;
import com.arsenalcrafting.mod.core.smeltery.SmelteryFormation;
import com.arsenalcrafting.mod.core.smeltery.SmelteryTier;
import com.arsenalcrafting.mod.fluids.MultiFluidTank;
import com.arsenalcrafting.mod.generator.AlloyRecipeDefinitions;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.network.SyncAlloySmelterCapacityPacket;
import com.arsenalcrafting.mod.network.SyncAlloySmelterFluidPacket;
import com.arsenalcrafting.mod.network.SyncOutputTankCapacityPacket;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
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

    public String currentRecipeType = null;
    private AlloyRecipe currentSolidRecipe = null;
    private AlloyRecipeDefinitions.LiquidMixingRecipeDefinition currentLiquidRecipe = null;
    private int craftingProgress = 0;
    private int craftingTime = 0;
    private boolean isCrafting = false;

    // Max 45 slots (Titanium tier max)
    private int[] slotProgress = new int[45];
    private int[] slotMaxProgress = new int[45];

    public String castTargetFluid = null;

    // ── SLOT COUNT ────────────────────────────────────────────────────────────
    private int currentSlotCount = 9;

    public void updateSlotCount() {
        if (smelteryData != null && smelteryData.isValid()) {
            currentSlotCount = smelteryData.getTotalSlots();
        } else {
            currentSlotCount = 9;
        }
        /*System.out.println("[SlotCount] smelteryData=" + smelteryData +
                " isValid=" + (smelteryData != null ? smelteryData.isValid() : "null") +
                " height=" + (smelteryData != null ? smelteryData.height() : "null") +
                " tier=" + (smelteryData != null ? smelteryData.tier() : "null") +
                " getTotalSlots=" + (smelteryData != null ? smelteryData.getTotalSlots() : "null") +
                " result=" + currentSlotCount);

         */
    }

    public int getCurrentSlotCount() {
        return currentSlotCount;
    }


    // ── ITEM HANDLER ──────────────────────────────────────────────────────────
    private final ItemStackHandler itemHandler = new ItemStackHandler(45) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            syncItemsToClient();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= currentSlotCount) return false;
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

    private int scrolledRow = 0;

    public void setScrolledRow(int row) {
        int maxRow = getMaxScrollRow();
        this.scrolledRow = row;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }
    public void setScrolledRowClientOnly(int row) {
        this.scrolledRow = Math.max(0, Math.min(getMaxScrollRow(), row));


    }

    public int getScrolledRow() {
        return scrolledRow;
    }

    public int getMaxScrollRow() {
        // Each scroll step shows 9 slots (full 3x3 page)
        int totalPages = (int) Math.ceil((double) currentSlotCount / 9.0);
        return Math.max(0, totalPages - 1);
    }

    public boolean isSlotVisible(int slotIndex) {
        int pageStart = scrolledRow * 9;
        int pageEnd   = pageStart + 9;
        boolean visible = slotIndex >= pageStart && slotIndex < pageEnd;

        return visible;
    }

    // ── NUGGET ACCUMULATOR ────────────────────────────────────────────────────
    private final Map<String, Integer> nuggetAccumulator = new HashMap<>();

    public int addNuggets(String materialName, int nuggets) {
        int current = nuggetAccumulator.getOrDefault(materialName, 0);
        int total = current + nuggets;
        int fullIngots = total / 9;
        int remainder = total % 9;
        nuggetAccumulator.put(materialName, remainder);
        setChanged();
        return fullIngots;
    }

    public int getNuggetCount(String materialName) {
        return nuggetAccumulator.getOrDefault(materialName, 0);
    }

    public void dropAccumulatedNuggets() {
        if (level == null) return;
        for (Map.Entry<String, Integer> entry : nuggetAccumulator.entrySet()) {
            int nuggets = entry.getValue();
            if (nuggets >= 5) {
                AlloyMaterial mat = AlloyMaterial.fromName(entry.getKey());
                if (mat != null) {
                    ItemStack ingot = ItemStackHelper.getAlloyStack(mat);
                    Block.popResource(level, worldPosition, ingot);
                }
            }
        }
        nuggetAccumulator.clear();
    }

    public void drops() {
        if (this.level == null) return;
        dropAccumulatedNuggets();
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // ── LAVA DISPLAY TANK ─────────────────────────────────────────────────────
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

    // ── OUTPUT TANK ───────────────────────────────────────────────────────────
    public final MultiFluidTank OUTPUT_TANK = new MultiFluidTank(9000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            syncOutputTankToClient();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return true;
        }
    };

    private void syncOutputTankToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    // ── FLUID HELPER METHODS ──────────────────────────────────────────────────

    public void drainFromOutputTank(Fluid fluid, int amount) {
        int remaining = amount;
        int tanksCount = OUTPUT_TANK.getTanks();
        for (int i = 0; i < tanksCount; i++) {
            FluidStack fluidStack = OUTPUT_TANK.getFluidInTank(i);
            if (fluidStack != null && !fluidStack.isEmpty() && fluidStack.getFluid() == fluid) {
                int toDrain = Math.min(remaining, fluidStack.getAmount());
                OUTPUT_TANK.drain(new FluidStack(fluid, toDrain), IFluidHandler.FluidAction.EXECUTE);
                remaining -= toDrain;
                if (remaining <= 0) break;
            }
        }
        setChanged();
    }

    public void addToOutputTank(FluidStack fluidStack) {
        OUTPUT_TANK.fill(fluidStack, IFluidHandler.FluidAction.EXECUTE);
        setChanged();
    }

    public List<FluidStack> getAllOutputFluids() {
        List<FluidStack> fluids = new ArrayList<>();
        for (int i = 0; i < OUTPUT_TANK.getTanks(); i++) {
            FluidStack fluid = OUTPUT_TANK.getFluidInTank(i);
            if (fluid != null && !fluid.isEmpty()) {
                fluids.add(fluid.copy());
            }
        }
        return fluids;
    }

    public int getOutputFluidCapacity() { return OUTPUT_TANK.getCapacity(); }
    public int getOutputFluidAmount() { return OUTPUT_TANK.getTotalAmount(); }
    public int getOutputTankTotalAmount() { return OUTPUT_TANK.getTotalAmount(); }
    public int getOutputTankCapacity() { return OUTPUT_TANK.getCapacity(); }

    public void checkAndCraftLiquidRecipe() {
        if (level == null || level.isClientSide) return;
        Optional<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> recipe = findValidLiquidRecipe();
        if (recipe.isPresent()) {
            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition liquidRecipe = recipe.get();
            if (liquidRecipe.canCraft(getAllOutputFluids())) {
                for (FluidStack input : liquidRecipe.getConsumedFluids()) {
                    drainFromOutputTank(input.getFluid(), input.getAmount());
                }
                addToOutputTank(liquidRecipe.output().copy());
                this.craftingProgress = 0;
                this.craftingTime = liquidRecipe.craftTime();
                this.isCrafting = true;
                setChanged();
            }
        }
    }

    public Optional<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> findValidLiquidRecipe() {
        List<FluidStack> availableFluids = getAllOutputFluids();
        return AlloyRecipeDefinitions.LIQUID_MIXING_RECIPES.values().stream()
                .filter(r -> r.canCraft(availableFluids))
                .findFirst();
    }

    // ── CONNECTED TANKS ───────────────────────────────────────────────────────
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

    // ── PROGRESS ──────────────────────────────────────────────────────────────
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
                    default -> {
                        int rel = i - 3;
                        int slot = rel / 2;
                        if (slot >= 0 && slot < slotProgress.length) {
                            yield (rel % 2 == 0) ? slotProgress[slot] : slotMaxProgress[slot];
                        }
                        yield 0;
                    }
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
            public int getCount() { return 3 + 45 * 2; }
        };
    }

    // ── PUBLIC ACCESSORS ──────────────────────────────────────────────────────
    public ItemStackHandler getItemHandler() { return itemHandler; }
    public ContainerData getData() { return data; }
    public int getConnectedTanksCount() { return connectedTanks.size(); }
    public int getTotalTankCapacity() { return DISPLAY_TANK.getCapacity(); }

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

    // ── FLUID SYNC ────────────────────────────────────────────────────────────
    private void syncFluidToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            PacketHandler.sendToAllTracking(
                    new SyncAlloySmelterFluidPacket(worldPosition, DISPLAY_TANK.getFluid()),
                    level, worldPosition);
        }
    }

    // ── TANK SCANNING ─────────────────────────────────────────────────────────
    private void refreshConnectedTanks() {
        if (level == null || level.isClientSide) return;
        List<ConnectedTankInfo> newTanks = new ArrayList<>();
        int totalCapacity = 0;

        if (smelteryData != null && smelteryData.isValid()) {
            // Scan entire multiblock bounds for lava tanks
            BlockPos origin = smelteryData.interiorOrigin();
            int minX = origin.getX() - 1;
            int maxX = origin.getX() + smelteryData.width();
            int minZ = origin.getZ() - 1;
            int maxZ = origin.getZ() + smelteryData.depth();
            int minY = origin.getY() - 1;
            int maxY = origin.getY() + smelteryData.height() - 1;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        BlockPos scanPos = new BlockPos(x, y, z);
                        if (level.getBlockEntity(scanPos) instanceof LavaTankBlockEntity lavaTank) {
                            int cap = lavaTank.fluidTank.getCapacity();
                            newTanks.add(new ConnectedTankInfo(lavaTank.fluidTank, cap));
                            totalCapacity += cap;
                        }
                    }
                }
            }
        } else {
            // Fallback — scan adjacent only when no multiblock data
            for (Direction dir : Direction.values()) {
                BlockPos neighborPos = worldPosition.relative(dir);
                if (level.getBlockEntity(neighborPos) instanceof LavaTankBlockEntity lavaTank) {
                    int cap = lavaTank.fluidTank.getCapacity();
                    newTanks.add(new ConnectedTankInfo(lavaTank.fluidTank, cap));
                    totalCapacity += cap;
                }
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
        if (level != null && !level.isClientSide) {
            com.arsenalcrafting.mod.core.smeltery.SmelteryDebug.formation(
                    "Controller", "onLoad at " + worldPosition + " — refreshing and validating");
            refreshConnectedTanks();
            validateIfNeeded();
        }
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

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        DISPLAY_TANK.writeToNBT(registries, tag);
        tag.putInt("TankCount", connectedTanks.size());
        tag.put("inv", itemHandler.serializeNBT(registries));
        CompoundTag outputTag = new CompoundTag();
        OUTPUT_TANK.save(outputTag, registries);
        tag.put("OutputTank", outputTag);
        if (castTargetFluid != null) tag.putString("CastTarget", castTargetFluid);
        tag.putString("MeltingBlocked", String.join(",", meltingBlocked));
        tag.putString("MixingBlocked", String.join(",", mixingBlocked));
        if (currentRecipeType != null) tag.putString("RecipeType", currentRecipeType);
        tag.putInt("Progress", progress);
        tag.putInt("MaxProgress", maxProgress);
        tag.putInt("CurrentSlotCount", currentSlotCount);
        tag.putInt("ScrolledRow", scrolledRow);
        tag.putInt("OutputTankCapacity", OUTPUT_TANK.getCapacity());
        if (smelteryData != null) {
            CompoundTag sd = new CompoundTag();
            sd.putLong("ControllerPos", smelteryData.controllerPos().asLong());
            sd.putLong("InteriorOrigin", smelteryData.interiorOrigin().asLong());
            sd.putInt("Width", smelteryData.width());
            sd.putInt("Depth", smelteryData.depth());
            sd.putInt("Height", smelteryData.height());
            sd.putBoolean("IsValid", smelteryData.isValid());
            sd.putString("Tier", smelteryData.tier().name());
            tag.put("SmelteryData", sd);
        }
        //System.out.println("[Server getUpdateTag] OutputTankCapacity=" + OUTPUT_TANK.getCapacity());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        DISPLAY_TANK.readFromNBT(registries, tag);
        if (level != null && level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
        if (tag.contains("inv")) itemHandler.deserializeNBT(registries, tag);
        if (tag.contains("OutputTank")) OUTPUT_TANK.load(tag.getCompound("OutputTank"), registries);
        if (tag.contains("OutputTankCapacity")) {
            OUTPUT_TANK.setCapacity(tag.getInt("OutputTankCapacity"));
            System.out.println("[Client handleUpdateTag] set capacity=" + OUTPUT_TANK.getCapacity());
        }

        if (tag.contains("RecipeType")) currentRecipeType = tag.getString("RecipeType");
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

        if (tag.contains("SmelteryData")) {
            CompoundTag sd = tag.getCompound("SmelteryData");
            SmelteryTier tier = SmelteryTier.COPPER;
            try { tier = SmelteryTier.valueOf(sd.getString("Tier")); } catch (Exception ignored) {}
            smelteryData = new SmelteryData(
                    BlockPos.of(sd.getLong("ControllerPos")),
                    BlockPos.of(sd.getLong("InteriorOrigin")),
                    sd.getInt("Width"),
                    sd.getInt("Depth"),
                    sd.getInt("Height"),
                    sd.getBoolean("IsValid"),
                    false,
                    tier,
                    null
            );
        }
        progress = tag.getInt("Progress");
        maxProgress = tag.getInt("MaxProgress");
        if (tag.contains("CurrentSlotCount")) currentSlotCount = tag.getInt("CurrentSlotCount");
        if (tag.contains("ScrolledRow")) scrolledRow = tag.getInt("ScrolledRow");
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
        CompoundTag outputTag = new CompoundTag();
        OUTPUT_TANK.save(outputTag, registries);
        tag.put("OutputTank", outputTag);
        if (castTargetFluid != null) tag.putString("CastTarget", castTargetFluid);
        tag.putString("MixingBlocked", String.join(",", mixingBlocked));
        tag.putString("MeltingBlocked", String.join(",", meltingBlocked));
        if (currentRecipeType != null) tag.putString("RecipeType", currentRecipeType);
        tag.putInt("craftingProgress", craftingProgress);
        tag.putInt("craftingTime", craftingTime);
        tag.putBoolean("isCrafting", isCrafting);
        tag.putInt("CurrentSlotCount", currentSlotCount);

        if (smelteryData != null) {
            CompoundTag sd = new CompoundTag();
            sd.putLong("ControllerPos", smelteryData.controllerPos().asLong());
            sd.putLong("InteriorOrigin", smelteryData.interiorOrigin().asLong());
            sd.putInt("Width", smelteryData.width());
            sd.putInt("Depth", smelteryData.depth());
            sd.putInt("Height", smelteryData.height());
            sd.putBoolean("IsValid", smelteryData.isValid());
            sd.putBoolean("NeedsRecheck", smelteryData.needsRecheck());
            sd.putString("Tier", smelteryData.tier().name());
            if (smelteryData.failReason() != null) sd.putString("FailReason", smelteryData.failReason());
            tag.put("SmelteryData", sd);
        }

        CompoundTag nuggetTag = new CompoundTag();
        for (Map.Entry<String, Integer> entry : nuggetAccumulator.entrySet()) {
            nuggetTag.putInt(entry.getKey(), entry.getValue());
        }
        tag.put("NuggetAccumulator", nuggetTag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inv"));
        progress = tag.getInt("prog");
        maxProgress = tag.getInt("maxprog");
        if (tag.contains("slotProgress")) slotProgress = tag.getIntArray("slotProgress");
        if (tag.contains("slotMaxProgress")) slotMaxProgress = tag.getIntArray("slotMaxProgress");
        craftingProgress = tag.getInt("craftingProgress");
        craftingTime = tag.getInt("craftingTime");
        isCrafting = tag.getBoolean("isCrafting");
        if (tag.contains("CurrentSlotCount")) currentSlotCount = tag.getInt("CurrentSlotCount");

        if (tag.contains("DisplayTank")) DISPLAY_TANK.readFromNBT(registries, tag.getCompound("DisplayTank"));
        if (tag.contains("OutputTank")) OUTPUT_TANK.load(tag.getCompound("OutputTank"), registries);
        if (tag.contains("RecipeType")) currentRecipeType = tag.getString("RecipeType");

        if (tag.contains("MixingBlocked")) {
            String raw = tag.getString("MixingBlocked");
            mixingBlocked.clear();
            if (!raw.isEmpty()) Collections.addAll(mixingBlocked, raw.split(","));
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

        nuggetAccumulator.clear();
        if (tag.contains("NuggetAccumulator")) {
            CompoundTag nuggetTag = tag.getCompound("NuggetAccumulator");
            for (String key : nuggetTag.getAllKeys()) {
                nuggetAccumulator.put(key, nuggetTag.getInt(key));
            }
        }

        if (tag.contains("SmelteryData")) {
            CompoundTag sd = tag.getCompound("SmelteryData");
            SmelteryTier tier = SmelteryTier.COPPER;
            try { tier = SmelteryTier.valueOf(sd.getString("Tier")); } catch (Exception ignored) {}
            String failReason = sd.contains("FailReason") ? sd.getString("FailReason") : null;
            smelteryData = new SmelteryData(
                    BlockPos.of(sd.getLong("ControllerPos")),
                    BlockPos.of(sd.getLong("InteriorOrigin")),
                    sd.getInt("Width"),
                    sd.getInt("Depth"),
                    sd.getInt("Height"),
                    sd.getBoolean("IsValid"),
                    sd.getBoolean("NeedsRecheck"),
                    tier,
                    failReason
            );
        }
    }

    // ── TICK ──────────────────────────────────────────────────────────────────

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
        be.validateIfNeeded();

        if (be.smelteryData != null && be.smelteryData.isValid()) {
            int correctCapacity = be.smelteryData.height() * 9000;
            be.OUTPUT_TANK.setCapacity(correctCapacity);
            PacketHandler.sendToAllTracking(
                    new SyncOutputTankCapacityPacket(pos, correctCapacity),
                    level, pos);
        }
        be.updateDisplayTank();

        // Check for liquid mixing recipes
        if (be.currentRecipeType == null || be.currentRecipeType.equals("solid")) {
            if (be.progress == 0) {
                Optional<AlloyRecipeDefinitions.LiquidMixingRecipeDefinition> liquidRecipe = be.findValidLiquidRecipe();
                if (liquidRecipe.isPresent()) {
                    AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe = liquidRecipe.get();
                    int resultMb = recipe.output().getAmount();
                    int consumedMb = recipe.getConsumedFluids().stream().mapToInt(FluidStack::getAmount).sum();
                    if (be.OUTPUT_TANK.getTotalAmount() - consumedMb + resultMb > be.OUTPUT_TANK.getCapacity()) {
                        be.progress = 0;
                        setChanged(level, pos, state);
                        return;
                    }
                    AlloyMaterial outputMat = AlloyMaterial.fromFluid(recipe.output().getFluid());
                    if (outputMat != null && be.mixingBlocked.contains(outputMat.getName())) {
                        be.progress = 0;
                        be.currentRecipeType = null;
                        be.currentLiquidRecipe = null;
                        setChanged(level, pos, state);
                        return;
                    }
                    be.currentRecipeType = "liquid";
                    be.maxProgress = recipe.craftTime();
                    be.currentLiquidRecipe = recipe;
                    be.progress = 1;
                    setChanged(level, pos, state);
                    return;
                }
            }
        }

        // Continue liquid recipe
        if (be.currentRecipeType != null && be.currentRecipeType.equals("liquid") && be.currentLiquidRecipe != null) {
            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe = be.currentLiquidRecipe;
            AlloyMaterial outputMat = AlloyMaterial.fromFluid(recipe.output().getFluid());
            if (outputMat != null && be.mixingBlocked.contains(outputMat.getName())) {
                be.progress = 0;
                be.currentRecipeType = null;
                be.currentLiquidRecipe = null;
                setChanged(level, pos, state);
                return;
            }
            if (!recipe.canCraft(be.getAllOutputFluids())) {
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

        // ── SOLID RECIPE PROCESSING ───────────────────────────────────────────────

        int lavaRemaining = be.getTotalLavaInTanks(); // snapshot once, decrement manually

        for (int slotIndex = 0; slotIndex < be.currentSlotCount; slotIndex++) {
            ItemStack slotItem = be.itemHandler.getStackInSlot(slotIndex);
            if (slotItem.isEmpty()) {
                be.slotProgress[slotIndex] = 0;
                be.slotMaxProgress[slotIndex] = 0;
                continue;
            }

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

            int baseCraftTime = recipe.getCraftTime();
            int adjustedCraftTime = (be.smelteryData != null && be.smelteryData.isValid())
                    ? be.smelteryData.tier().getCraftTime(baseCraftTime)
                    : baseCraftTime;

            if (be.slotMaxProgress[slotIndex] == 0) {
                be.slotMaxProgress[slotIndex] = adjustedCraftTime;
            }

            int resultMb = recipe.getResultAmountMb();
            int adjustedMb = resultMb;
            if (recipe.isOreRecipe() && be.smelteryData != null && be.smelteryData.isValid()) {
                adjustedMb = Math.round(resultMb * be.smelteryData.tier().getOreMultiplier());
            }
            if (be.OUTPUT_TANK.getTotalAmount() + adjustedMb > be.OUTPUT_TANK.getCapacity()) continue;

            // Only consume lava on the first tick of a new recipe
            if (be.slotProgress[slotIndex] == 0) {
                int lavaNeeded = recipe.getLavaCost();
                if (lavaNeeded > lavaRemaining) continue;
                if (!be.consumeLavaFromTanks(lavaNeeded)) continue;
                lavaRemaining -= lavaNeeded;
            }

            be.slotProgress[slotIndex]++;

            if (be.slotProgress[slotIndex] >= be.slotMaxProgress[slotIndex]) {
                System.out.println("[MELT] slot=" + slotIndex
                        + " baseCraftTime=" + recipe.getCraftTime()
                        + " adjustedCraftTime=" + be.slotMaxProgress[slotIndex]
                        + " tier=" + (be.smelteryData != null ? be.smelteryData.tier() : "null")
                        + " multiplier=" + (be.smelteryData != null && be.smelteryData.isValid()
                        ? be.smelteryData.tier().getSpeedMultiplier() : "none"));

                be.itemHandler.extractItem(slotIndex, 1, false);
                AlloyMaterial resultMat = AlloyMaterial.fromName(recipe.getResultMaterial());
                if (resultMat != null) {
                    var sourceFluid = ModFluids.getSource(resultMat);
                    System.out.println("[ORE] isOreRecipe=" + recipe.isOreRecipe()
                            + " tier=" + (be.smelteryData != null ? be.smelteryData.tier() : "null")
                            + " multiplier=" + (be.smelteryData != null && be.smelteryData.isValid()
                            ? be.smelteryData.tier().getOreMultiplier() : "none")
                            + " resultMb=" + resultMb
                            + " adjustedMb=" + adjustedMb);
                    if (sourceFluid != null) {
                        be.OUTPUT_TANK.fill(
                                new FluidStack(sourceFluid, adjustedMb),
                                IFluidHandler.FluidAction.EXECUTE);
                    }
                }
                be.slotProgress[slotIndex] = 0;
                be.slotMaxProgress[slotIndex] = 0;
            }
        }
        boolean anyActive = false;
        for (int i = 0; i < be.currentSlotCount; i++) {
            if (be.slotProgress[i] > 0) { anyActive = true; break; }
        }
        be.setActive(anyActive);

        be.currentRecipeType = null;
        be.currentSolidRecipe = null;
        setChanged(level, pos, state);
    }

    private static void craftLiquidRecipe(AlloySmelterBlockEntity be) {
        if (be.currentLiquidRecipe != null) {
            AlloyRecipeDefinitions.LiquidMixingRecipeDefinition recipe = be.currentLiquidRecipe;
            for (FluidStack input : recipe.getConsumedFluids()) {
                be.drainFromOutputTank(input.getFluid(), input.getAmount());
            }
            be.addToOutputTank(recipe.output().copy());
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
        for (int i = 0; i < 9; i++) be.itemHandler.extractItem(i, 1, false);
        AlloyMaterial resultMat = AlloyMaterial.fromName(recipe.getResultMaterial());
        if (resultMat != null) {
            var sourceFluid = ModFluids.getSource(resultMat);
            if (sourceFluid != null) {
                be.OUTPUT_TANK.fill(
                        new FluidStack(sourceFluid, recipe.getResultAmountMb()),
                        IFluidHandler.FluidAction.EXECUTE);
            }
        }
        be.setChanged();
        if (be.level != null && !be.level.isClientSide) {
            be.level.sendBlockUpdated(be.worldPosition, be.getBlockState(), be.getBlockState(), 3);
        }
    }

    // ── SMELTERY MULTIBLOCK ───────────────────────────────────────────────────
    private SmelteryData smelteryData = null;

    public void markNeedsRecheck() {
        if (smelteryData != null) {
            smelteryData = smelteryData.withNeedsRecheck();
        } else {
            smelteryData = SmelteryData.invalid(worldPosition).withNeedsRecheck();
        }
        setChanged();
    }

    private void updateBlockStateFromData() {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof AlloySmelterBlock)) return;

        AlloySmelterBlock.SmelterTier tier = (smelteryData != null && smelteryData.isValid())
                ? AlloySmelterBlock.SmelterTier.fromSmelteryTier(smelteryData.tier())
                : AlloySmelterBlock.SmelterTier.UNFORMED;

        level.setBlock(worldPosition, state.setValue(AlloySmelterBlock.TIER, tier), 3);

        // Update all lava tanks in multiblock bounds
        if (smelteryData != null && smelteryData.isValid()) {
            LavaTankBlock.TankTier tankTier = LavaTankBlock.TankTier.valueOf(smelteryData.tier().name());
            BlockPos origin = smelteryData.interiorOrigin();
            int minX = origin.getX() - 1;
            int maxX = origin.getX() + smelteryData.width();
            int minZ = origin.getZ() - 1;
            int maxZ = origin.getZ() + smelteryData.depth();
            int minY = origin.getY() - 1;
            int maxY = origin.getY() + smelteryData.height() - 1;

            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    for (int z = minZ; z <= maxZ; z++) {
                        if (level.getBlockEntity(new BlockPos(x, y, z)) instanceof LavaTankBlockEntity lavaTank) {
                            lavaTank.updateTier(tankTier);
                        }
                    }
                }
            }
        } else {
            // Reset all adjacent lava tanks to unformed
            for (Direction dir : Direction.values()) {
                if (level.getBlockEntity(worldPosition.relative(dir)) instanceof LavaTankBlockEntity lavaTank) {
                    lavaTank.updateTier(LavaTankBlock.TankTier.UNFORMED);
                }
            }
        }
    }

    public void setActive(boolean active) {
        if (level == null || level.isClientSide) return;
        BlockState state = level.getBlockState(worldPosition);
        if (!(state.getBlock() instanceof AlloySmelterBlock)) return;
        if (state.getValue(AlloySmelterBlock.ACTIVE) == active) return;
        level.setBlock(worldPosition, state.setValue(AlloySmelterBlock.ACTIVE, active), 3);
    }

    public void validateIfNeeded() {
        if (level == null || level.isClientSide) return;
        if (smelteryData == null || smelteryData.needsRecheck()) {
            smelteryData = SmelteryFormation.validate(level, worldPosition);
            SmelteryDebug.formation("Validate",
                    "valid=" + smelteryData.isValid()
                            + " h=" + smelteryData.height()
                            + " tier=" + smelteryData.tier()
                            + " slots=" + smelteryData.getTotalSlots());
            int volume = smelteryData.isValid() ? smelteryData.volume() : 0;
            OUTPUT_TANK.setCapacity(smelteryData.isValid() ? smelteryData.height() * 9000 : 9000);
            updateSlotCount();
            updateBlockStateFromData();
            setChanged();
        }

    }

    public boolean isFormed() {
        validateIfNeeded();
        return smelteryData != null && smelteryData.isValid();
    }

    public SmelteryData getSmelteryData() {
        return smelteryData;
    }
}