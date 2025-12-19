package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.core.alloy.AlloyRecipeInput;
import com.arsenalcrafting.mod.core.alloy.AlloyRecipeType;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.blocks.menu.AlloySmelterMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class AlloySmelterBlockEntity extends BlockEntity implements MenuProvider {
    // 9 input slots + 1 fuel slot + 1 output slot = 11 total
    private final ItemStackHandler itemHandler = new ItemStackHandler(11) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            // Slot 9 is fuel, slot 10 is output
            if (slot == 10) return false; // Output slot
            return true;
        }
    };

    // Container data for syncing with client
    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 200; // 10 seconds at 20 ticks/sec
    private int fuelTime = 0;
    private int maxFuelTime = 0;

    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER.get(), pos, state);

        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> AlloySmelterBlockEntity.this.progress;
                    case 1 -> AlloySmelterBlockEntity.this.maxProgress;
                    case 2 -> AlloySmelterBlockEntity.this.fuelTime;
                    case 3 -> AlloySmelterBlockEntity.this.maxFuelTime;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> AlloySmelterBlockEntity.this.progress = value;
                    case 1 -> AlloySmelterBlockEntity.this.maxProgress = value;
                    case 2 -> AlloySmelterBlockEntity.this.fuelTime = value;
                    case 3 -> AlloySmelterBlockEntity.this.maxFuelTime = value;
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
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

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new AlloySmelterMenu(containerId, playerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", itemHandler.serializeNBT(registries));
        tag.putInt("alloy_smelter.progress", progress);
        tag.putInt("alloy_smelter.fuel_time", fuelTime);
        tag.putInt("alloy_smelter.max_fuel_time", maxFuelTime);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        itemHandler.deserializeNBT(registries, tag.getCompound("inventory"));
        progress = tag.getInt("alloy_smelter.progress");
        fuelTime = tag.getInt("alloy_smelter.fuel_time");
        maxFuelTime = tag.getInt("alloy_smelter.max_fuel_time");
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, AlloySmelterBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        if (hasRecipe(blockEntity)) {
            // Try to consume fuel if we need it
            if (blockEntity.fuelTime <= 0) {
                if (hasFuel(blockEntity)) {
                    consumeFuel(blockEntity);
                } else {
                    blockEntity.progress = 0;
                    setChanged(level, pos, state);
                    return;
                }
            }

            // Decrease fuel time
            blockEntity.fuelTime--;

            // Increase progress
            blockEntity.progress++;
            setChanged(level, pos, state);

            if (blockEntity.progress >= blockEntity.maxProgress) {
                craftItem(blockEntity);
                blockEntity.progress = 0;
            }
        } else {
            blockEntity.progress = 0;
            setChanged(level, pos, state);
        }
    }

    private static boolean hasFuel(AlloySmelterBlockEntity entity) {
        ItemStack fuelStack = entity.itemHandler.getStackInSlot(9);
        return !fuelStack.isEmpty() && fuelStack.getBurnTime(null) > 0;
    }

    private static void consumeFuel(AlloySmelterBlockEntity entity) {
        ItemStack fuelStack = entity.itemHandler.getStackInSlot(9);
        int burnTime = fuelStack.getBurnTime(null);

        entity.maxFuelTime = burnTime;
        entity.fuelTime = burnTime;

        fuelStack.shrink(1);
        entity.itemHandler.setStackInSlot(9, fuelStack);
    }

    private static boolean hasRecipe(AlloySmelterBlockEntity entity) {
        Level level = entity.level;
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
        for (int i = 0; i < entity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }

        // Find matching recipe
        var recipe = level.getRecipeManager()
                .getRecipeFor(AlloyRecipeType.ALLOY_SMELTING.get(),
                        new AlloyRecipeInput(inventory), level);

        if (recipe.isEmpty()) {
            return false;
        }

        ItemStack result = recipe.get().value().getResultItem(level.registryAccess());
        ItemStack outputSlot = entity.itemHandler.getStackInSlot(10);

        // Check if output slot can accept result
        return outputSlot.isEmpty() ||
                (outputSlot.getItem() == result.getItem() &&
                        outputSlot.getCount() + result.getCount() <= outputSlot.getMaxStackSize());
    }

    private static void craftItem(AlloySmelterBlockEntity entity) {
        Level level = entity.level;
        SimpleContainer inventory = new SimpleContainer(entity.itemHandler.getSlots());
        for (int i = 0; i < entity.itemHandler.getSlots(); i++) {
            inventory.setItem(i, entity.itemHandler.getStackInSlot(i));
        }

        var recipe = level.getRecipeManager()
                .getRecipeFor(AlloyRecipeType.ALLOY_SMELTING.get(),
                        new AlloyRecipeInput(inventory), level);

        if (recipe.isEmpty()) {
            return;
        }

        ItemStack result = recipe.get().value().assemble(new AlloyRecipeInput(inventory),
                level.registryAccess());
        ItemStack outputSlot = entity.itemHandler.getStackInSlot(10);

        // Consume ingredients
        for (int i = 0; i < 9; i++) {
            entity.itemHandler.extractItem(i, 1, false);
        }

        // Add to output
        if (outputSlot.isEmpty()) {
            entity.itemHandler.setStackInSlot(10, result.copy());
        } else {
            outputSlot.grow(result.getCount());
        }
    }
}