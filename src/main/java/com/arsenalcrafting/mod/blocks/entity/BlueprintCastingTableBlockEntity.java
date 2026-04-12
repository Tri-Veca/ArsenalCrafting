package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.core.alloy.PrefilledPackageRegistry;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

public class BlueprintCastingTableBlockEntity extends BlockEntity implements MenuProvider, Container {

    // Slot 0: blueprint, Slot 1: gold/bronze ingot, Slot 2: output
    public final ItemStackHandler itemHandler = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
                if ((slot == 0 || slot == 1) && !isCrafting) {
                    tryCrafting();
                }
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot == 0) return stack.getItem() == ModItems.BLUEPRINT.get();
            if (slot == 1) return stack.getItem() == Items.GOLD_INGOT || isBronzeIngot(stack);
            return false;
        }

        @Override
        protected void onLoad() {
            super.onLoad();
            if (level != null && !level.isClientSide) {
                tryCrafting();
            }
        }
    };

    private PackageType selectedPackageType = null;
    private boolean isCrafting = false;

    public BlueprintCastingTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BLUEPRINT_CASTING_TABLE.get(), pos, state);
    }

    private boolean isBronzeIngot(ItemStack stack) {
        var bronzeHolder = ModItems.ALLOY_ITEMS.get(AlloyMaterial.BRONZE);
        return bronzeHolder != null && stack.getItem() == bronzeHolder.get();
    }

    private boolean isUsingBronze() {
        ItemStack ingot = itemHandler.getStackInSlot(1);
        if (ingot.isEmpty()) return false;
        return isBronzeIngot(ingot);
    }

    public void setSelectedPackage(PackageType packageType) {
        this.selectedPackageType = packageType;
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            tryCrafting();
        }
    }

    public void tryCrafting() {
        if (level == null || level.isClientSide) return;
        if (isCrafting) return;
        if (selectedPackageType == null) return;
        if (!itemHandler.getStackInSlot(2).isEmpty()) return;
        if (!canCraft(selectedPackageType)) return;

        isCrafting = true;
        try {
            boolean useBronze = isUsingBronze();

            // Extract payment
            itemHandler.extractItem(0, 1, false);
            itemHandler.extractItem(1, selectedPackageType.getIngotCost(), false);

            // Get pre-filled package from registry
            ItemStack result = PrefilledPackageRegistry.getPrefilledPackage(selectedPackageType, useBronze);
            itemHandler.setStackInSlot(2, result);

            selectedPackageType = null;
            setChanged();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        } finally {
            isCrafting = false;
        }
    }

    public boolean canCraft(PackageType packageType) {
        ItemStack blueprint = itemHandler.getStackInSlot(0);
        ItemStack ingot = itemHandler.getStackInSlot(1);

        if (blueprint.isEmpty() || blueprint.getItem() != ModItems.BLUEPRINT.get()) return false;
        if (ingot.isEmpty()) return false;
        if (ingot.getItem() != Items.GOLD_INGOT && !isBronzeIngot(ingot)) return false;
        if (ingot.getCount() < packageType.getIngotCost()) return false;

        return true;
    }

    public PackageType getSelectedPackage() {
        return selectedPackageType;
    }

    public boolean canCraftSelected() {
        return selectedPackageType != null && canCraft(selectedPackageType);
    }

    public void drops() {
        if (level == null) return;
        SimpleContainer inv = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inv.setItem(i, itemHandler.getStackInSlot(i));
        }
        net.minecraft.world.Containers.dropContents(level, worldPosition, inv);
    }

    // ========== MenuProvider ==========
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arsenalcrafting.blueprint_casting_table");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new com.arsenalcrafting.mod.client.menu.BlueprintCastingTableMenu(id, inv, this);
    }

    // ========== Container Implementation ==========
    @Override
    public int getContainerSize() {
        return itemHandler.getSlots();
    }

    @Override
    public boolean isEmpty() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            if (!itemHandler.getStackInSlot(i).isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return itemHandler.getStackInSlot(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        return itemHandler.extractItem(slot, amount, false);
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack stack = itemHandler.getStackInSlot(slot);
        itemHandler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        itemHandler.setStackInSlot(slot, stack);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            itemHandler.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    // ========== NBT ==========
    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inv", itemHandler.serializeNBT(registries));
        if (selectedPackageType != null) {
            tag.putString("selectedPackage", selectedPackageType.getId());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inv")) {
            itemHandler.deserializeNBT(registries, tag.getCompound("inv"));
        }
        if (tag.contains("selectedPackage")) {
            selectedPackageType = PackageType.fromId(tag.getString("selectedPackage"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = super.getUpdateTag(registries);
        tag.put("inv", itemHandler.serializeNBT(registries));
        if (selectedPackageType != null) {
            tag.putString("selectedPackage", selectedPackageType.getId());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}