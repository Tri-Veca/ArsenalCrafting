package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.blocks.entity.BlueprintCastingTableBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.registry.ModItems;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.NotNull;

public class BlueprintCastingTableMenu extends AbstractContainerMenu {

    public final BlueprintCastingTableBlockEntity blockEntity;

    public BlueprintCastingTableMenu(int containerId, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(containerId, playerInventory, (BlueprintCastingTableBlockEntity) playerInventory.player.level().getBlockEntity(extraData.readBlockPos()));
    }

    public BlueprintCastingTableMenu(int containerId, Inventory playerInventory, BlockEntity entity) {
        super(ModMenus.BLUEPRINT_CASTING_TABLE_MENU.get(), containerId);
        this.blockEntity = (BlueprintCastingTableBlockEntity) entity;

        // Slot 0: Blueprint - using vanilla Slot with Container
        this.addSlot(new Slot(blockEntity, 0, 16, 34) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == ModItems.BLUEPRINT.get();
            }
        });

        // Slot 1: Ingot - using vanilla Slot with Container
        this.addSlot(new Slot(blockEntity, 1, 40, 34) {
            private boolean isBronzeIngot(ItemStack stack) {
                var bronzeHolder = ModItems.ALLOY_ITEMS.get(AlloyMaterial.BRONZE);
                return bronzeHolder != null && stack.getItem() == bronzeHolder.get();
            }

            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return stack.getItem() == Items.GOLD_INGOT || isBronzeIngot(stack);
            }
        });

        // Slot 2: Output - using vanilla Slot with Container
        this.addSlot(new Slot(blockEntity, 2, 75, 35) {
            @Override
            public boolean mayPlace(@NotNull ItemStack stack) {
                return false;
            }

            @Override
            public boolean mayPickup(Player player) {
                return true;  // YOU MISSED THIS - lets player take item
            }
        });

        addPlayerInventory(playerInventory);
        addPlayerHotbar(playerInventory);
    }

    public void setSelectedPackage(PackageType packageType) {
        blockEntity.setSelectedPackage(packageType);
    }

    public PackageType getSelectedPackage() {
        return blockEntity.getSelectedPackage();
    }

    public boolean canCraftSelected() {
        return blockEntity.canCraftSelected();
    }

    public boolean hasBlueprint() {
        return !blockEntity.getItem(0).isEmpty();
    }

    public int getIngotCount() {
        return blockEntity.getItem(1).getCount();
    }

    private boolean isBronzeIngot(ItemStack stack) {
        var bronzeHolder = ModItems.ALLOY_ITEMS.get(AlloyMaterial.BRONZE);
        return bronzeHolder != null && stack.getItem() == bronzeHolder.get();
    }

    public ItemStack getResultItem() {
        return blockEntity.getItem(2);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        if (index == 2) {
            if (!moveItemStackTo(stack, 3, 39, true)) return ItemStack.EMPTY;
            slot.set(ItemStack.EMPTY);
        } else if (index < 2) {
            if (!moveItemStackTo(stack, 3, 39, false)) return ItemStack.EMPTY;
        } else {
            if (stack.getItem() == ModItems.BLUEPRINT.get()) {
                if (!moveItemStackTo(stack, 0, 1, false)) return ItemStack.EMPTY;
            } else if (stack.getItem() == Items.GOLD_INGOT || isBronzeIngot(stack)) {
                if (!moveItemStackTo(stack, 1, 2, false)) return ItemStack.EMPTY;
            } else {
                return ItemStack.EMPTY;
            }
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();

        return copy;
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }

    private void addPlayerInventory(Inventory inv) {
        for (int i = 0; i < 3; i++)
            for (int j = 0; j < 9; j++)
                this.addSlot(new Slot(inv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
    }

    private void addPlayerHotbar(Inventory inv) {
        for (int i = 0; i < 9; i++)
            this.addSlot(new Slot(inv, i, 8 + i * 18, 142));
    }
}