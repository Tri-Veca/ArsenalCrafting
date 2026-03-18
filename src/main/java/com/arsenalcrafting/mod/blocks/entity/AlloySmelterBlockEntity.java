package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeInput;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.network.SyncAlloySmelterFluidPacket;
import com.arsenalcrafting.mod.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.Fluids;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.jetbrains.annotations.Nullable;

public class AlloySmelterBlockEntity extends BlockEntity implements MenuProvider {

    // Slots:
    // 0–8 = inputs
    // 9 = bucket input
    // 10 = output
    private final ItemStackHandler itemHandler = new ItemStackHandler(11) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 10) return false;
            return true;
        }
    };

    public void drops() {
        if (this.level == null) return;

        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());

        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public final FluidTank FLUID_TANK = new FluidTank(10000) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            System.out.printf(String.valueOf(FLUID_TANK.getFluidAmount()));
            syncFluidToClient();
        }

        @Override
        public boolean isFluidValid(FluidStack stack) {
            return stack.getFluid() == Fluids.LAVA;
        }



    };

    private void syncFluidToClient() {
        if (level != null && !level.isClientSide) {
            // Send to all players tracking this chunk
            LevelChunk chunk = level.getChunkAt(worldPosition);
            PacketHandler.sendToAllTracking(
                    new SyncAlloySmelterFluidPacket(worldPosition, FLUID_TANK.getFluid()),
                    level,
                    worldPosition
            );
        }
    }


    private int progress = 0;
    private int maxProgress = 200;
    private int fuelTime = 0;
    private int maxFuelTime = 0;

    protected final ContainerData data;

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> progress;
                    case 1 -> maxProgress;
                    case 2 -> fuelTime;
                    case 3 -> maxFuelTime;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int v) {
                switch (i) {
                    case 0 -> progress = v;
                    case 1 -> maxProgress = v;
                    case 2 -> fuelTime = v;
                    case 3 -> maxFuelTime = v;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    // 🔥 HANDLE BUCKET → TANK
    private void handleFluidInput() {
        ItemStack stack = itemHandler.getStackInSlot(9);

        if (stack.isEmpty()) return;

        var result = FluidUtil.tryEmptyContainer(
                stack,
                FLUID_TANK,
                Integer.MAX_VALUE,
                null,
                true // EXECUTE
        );

        if (result.isSuccess()) {
            itemHandler.setStackInSlot(9, result.getResult()); // TODO: HERE WE CAN CHANGE THE SLOT FOR LAVA
            setChanged();


        }
    }


    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ContainerData getData() {
        return data;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arsenalcrafting.alloy_smelter");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        FLUID_TANK.writeToNBT(registries, tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.handleUpdateTag(tag, registries);
        FLUID_TANK.readFromNBT(registries, tag);
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new AlloySmelterMenu(id, inv, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv", itemHandler.serializeNBT(registries));
        tag.putInt("prog", progress);
        tag.putInt("fuel", fuelTime);
        tag.putInt("maxfuel", maxFuelTime);
        FLUID_TANK.writeToNBT(registries, tag);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inv"));
        progress = tag.getInt("prog");
        fuelTime = tag.getInt("fuel");
        maxFuelTime = tag.getInt("maxfuel");
        FLUID_TANK.readFromNBT(registries, tag);
    }



    // In AlloySmelterBlockEntity.java - Update the tick method

    public static void tick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity be) {
        if (level.isClientSide) return;

        // Handle bucket → tank
        be.handleFluidInput();

        // Get current recipe if exists
        SimpleContainer inv = new SimpleContainer(be.itemHandler.getSlots());
        for (int i = 0; i < be.itemHandler.getSlots(); i++) {
            inv.setItem(i, be.itemHandler.getStackInSlot(i));
        }

        var recipeOpt = level.getRecipeManager().getRecipeFor(
                AlloyRecipeType.ALLOY_SMELTING.get(),
                new AlloyRecipeInput(inv), level
        );

        if (recipeOpt.isPresent()) {
            var recipe = recipeOpt.get().value();

            // Set max progress from recipe
            be.maxProgress = recipe.getCraftTime();

            // Calculate lava needed per tick
            int lavaPerTick = (int) Math.ceil((double) recipe.getLavaCost() / recipe.getCraftTime());

            // CHECK WHEN STARTING NEW CRAFT: Do we have enough lava for the ENTIRE recipe?
            if (be.progress == 0 && be.FLUID_TANK.getFluidAmount() < recipe.getLavaCost()) {
                // Not enough lava to start - do nothing
                return;
            }

            // If we're in the middle of crafting (progress > 0), we already passed the initial check
            // so just check if we have enough for this tick
            if (be.FLUID_TANK.getFluidAmount() < lavaPerTick) {
                // Out of lava mid-craft - reset progress
                be.progress = 0;
                return;
            }

            // Consume lava for this tick
            be.FLUID_TANK.drain(lavaPerTick, IFluidHandler.FluidAction.EXECUTE);
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

    private static boolean hasFuel(AlloySmelterBlockEntity e) {
        ItemStack fuel = e.itemHandler.getStackInSlot(9);
        return !fuel.isEmpty() && fuel.getBurnTime(null) > 0;
    }

    private static void consumeFuel(AlloySmelterBlockEntity e) {
        ItemStack fuel = e.itemHandler.getStackInSlot(9);
        int burn = fuel.getBurnTime(null);

        e.fuelTime = burn;
        e.maxFuelTime = burn;

        fuel.shrink(1);
        e.itemHandler.setStackInSlot(9, fuel);
    }

    private static boolean hasRecipe(AlloySmelterBlockEntity e) {
        Level level = e.level;

        SimpleContainer inv = new SimpleContainer(e.itemHandler.getSlots());
        for (int i = 0; i < e.itemHandler.getSlots(); i++) {
            inv.setItem(i, e.itemHandler.getStackInSlot(i));
        }

        var recipe = level.getRecipeManager().getRecipeFor(
                AlloyRecipeType.ALLOY_SMELTING.get(),
                new AlloyRecipeInput(inv), level
        );

        if (recipe.isEmpty()) return false;

        ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
        ItemStack out = e.itemHandler.getStackInSlot(10);

        return out.isEmpty() || (out.getItem() == result.getItem()
                && out.getCount() + result.getCount() <= out.getMaxStackSize());
    }

    private static void craftItem(AlloySmelterBlockEntity e) {
        Level level = e.level;

        SimpleContainer inv = new SimpleContainer(e.itemHandler.getSlots());
        for (int i = 0; i < e.itemHandler.getSlots(); i++) {
            inv.setItem(i, e.itemHandler.getStackInSlot(i));
        }

        var recipe = level.getRecipeManager().getRecipeFor(
                AlloyRecipeType.ALLOY_SMELTING.get(),
                new AlloyRecipeInput(inv), level
        );

        if (recipe.isEmpty()) return;

        ItemStack result = recipe.get().value().assemble(new AlloyRecipeInput(inv),
                level.registryAccess());

        for (int i = 0; i < 9; i++) {
            e.itemHandler.extractItem(i, 1, false);
        }

        ItemStack out = e.itemHandler.getStackInSlot(10);

        if (out.isEmpty()) {
            e.itemHandler.setStackInSlot(10, result.copy());
        } else {
            out.grow(result.getCount());
        }
    }
}