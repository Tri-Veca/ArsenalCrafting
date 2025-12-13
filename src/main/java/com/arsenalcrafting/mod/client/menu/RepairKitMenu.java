package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.component.RepairKitData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;

import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class RepairKitMenu extends AbstractContainerMenu {

    private final Container container = new SimpleContainer(2);
    public final ItemStack kitStack;

    // CLIENT ctor
    public RepairKitMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.getMainHandItem());
    }

    // SERVER ctor
    public RepairKitMenu(int id, Inventory inv, ItemStack kitStack) {
        super(ModMenus.REPAIRKIT.get(), id);
        this.kitStack = kitStack;

        // Load stored data
        RepairKitData data = RepairKitItem.getData(kitStack);
        // In RepairKitMenu constructor - fix the data.tool() and data.ingot() calls
        container.setItem(0, data.tool().orElse(ItemStack.EMPTY));
        container.setItem(1, data.ingot().orElse(ItemStack.EMPTY));





        // Tool slot
        addSlot(new Slot(container, 0, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArsenalTool;
            }
        });

        // Ingot slot
        addSlot(new Slot(container, 1, 80, 16));

        addPlayerInventory(inv);
    }

    // ---------------- INVENTORY ----------------

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9,
                        8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col,
                    8 + col * 18, 142));
        }
    }

    // ---------------- LOGIC ----------------

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        processRepair();
    }


    private void processRepair() {
        ItemStack tool = container.getItem(0);
        ItemStack ingot = container.getItem(1);

        if (tool.isEmpty() || ingot.isEmpty()) return;
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) return;
        if (tool.getDamageValue() <= 0) return;

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) return;

        String locked = RepairKitItem.getMaterial(kitStack);
        if (!locked.isEmpty() && !locked.equals(mat.getName())) {
            // Material mismatch - don't process
            return;
        }

        // Set material if not already set
        if (locked.isEmpty()) {
            RepairKitItem.setMaterial(kitStack, mat.getName());
        }

        int stored = RepairKitItem.getStoredValue(kitStack);
        int cost = arsenalTool.getRepairValuePerDurability(tool);

        // Debug output
        System.out.println("Debug - Stored: " + stored + ", Cost: " + cost + ", Tool Damage: " + tool.getDamageValue());

        // Add value from ingot
        if (stored < cost) {
            int valueToAdd = 2; // 1 ingot = 2 value
            RepairKitItem.setStoredValue(kitStack, stored + valueToAdd);
            ingot.shrink(1);
            return;
        }

        // If we have enough stored value, repair the tool
        if (stored >= cost) {
            // Repair one durability point
            tool.setDamageValue(tool.getDamageValue() - 1);
            // Deduct the cost
            RepairKitItem.setStoredValue(kitStack, stored - cost);
            // NO ingot shrink here - ingot was already consumed when adding value
        }
    }

    // ---------------- SAVE ----------------

    @Override
    public void removed(Player player) {
        super.removed(player);

        if (!player.level().isClientSide) {
            RepairKitItem.setTool(kitStack, container.getItem(0));
            RepairKitItem.setIngot(kitStack, container.getItem(1));
        }
    }


    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
