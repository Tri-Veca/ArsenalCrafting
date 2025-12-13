package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.component.RepairKitData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.network.SyncRepairKitPreviewPacket;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
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
    private final Player player;
    private int lastStoredValue = -1;
    private String lastMaterial = "";

    // Client-side cached values
    private int clientPreviewValue = 0;
    private String clientPreviewMaterial = "";

    // CLIENT ctor
    public RepairKitMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.getMainHandItem());
    }

    // SERVER ctor
    public RepairKitMenu(int id, Inventory inv, ItemStack kitStack) {
        super(ModMenus.REPAIRKIT.get(), id);
        this.kitStack = kitStack;
        this.player = inv.player;

        RepairKitData data = RepairKitItem.getData(kitStack);
        container.setItem(0, data.tool().orElse(ItemStack.EMPTY));
        container.setItem(1, data.ingot().orElse(ItemStack.EMPTY));
        this.lastStoredValue = data.storedValue();
        this.lastMaterial = data.material();

        // Tool slot
        addSlot(new Slot(container, 0, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArsenalTool;
            }

            @Override
            public void setChanged() {
                super.setChanged();
                ItemStack tool = getItem();
                if (tool.isEmpty()) {
                    RepairKitItem.setMaterialLocked(kitStack, false);
                }
            }
        });

        // Ingot slot
        addSlot(new Slot(container, 1, 80, 16) {
            @Override
            public void setChanged() {
                super.setChanged();
            }
        });

        addPlayerInventory(inv);
    }

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

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Update the stored value calculation based on ingots in slot
        updateStoredValueCalculation();

        // Only process actual repair on server side
        if (!player.level().isClientSide()) {
            attemptRepair();

            // Sync preview data to client
            if (player instanceof ServerPlayer serverPlayer &&
                    (lastStoredValue != clientPreviewValue || !lastMaterial.equals(clientPreviewMaterial))) {

                // Send sync packet to client
                PacketHandler.sendToPlayer(serverPlayer,
                        new SyncRepairKitPreviewPacket(lastStoredValue, lastMaterial));

                clientPreviewValue = lastStoredValue;
                clientPreviewMaterial = lastMaterial;
            }
        }
    }

    private void updateStoredValueCalculation() {
        ItemStack tool = container.getItem(0);
        ItemStack ingot = container.getItem(1);

        if (ingot.isEmpty()) {
            lastStoredValue = RepairKitItem.getStoredValue(kitStack);
            lastMaterial = RepairKitItem.getMaterial(kitStack);
            return;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) {
            lastStoredValue = 0;
            lastMaterial = "";
            return;
        }

        String ingotMaterial = mat.getName();
        String currentKitMaterial = RepairKitItem.getMaterial(kitStack);
        boolean materialLocked = RepairKitItem.isMaterialLocked(kitStack);

        // Check if there's a tool and if materials match
        if (!tool.isEmpty() && tool.getItem() instanceof ArsenalTool arsenalTool) {
            String toolMaterial = arsenalTool.getMaterialName(tool);

            // If tool has a material and it doesn't match ingot, show 0 preview
            if (!toolMaterial.isEmpty() && !toolMaterial.equals(ingotMaterial)) {
                lastStoredValue = 0;
                lastMaterial = currentKitMaterial;
                return;
            }
        }

        // Check material compatibility for preview
        if (!currentKitMaterial.isEmpty() && materialLocked && !currentKitMaterial.equals(ingotMaterial)) {
            // Different material and locked - cannot use
            lastStoredValue = 0;
            lastMaterial = currentKitMaterial;
            return;
        }

        // Calculate potential value from ingots in slot
        int potentialValue = Math.min(ingot.getCount() * 2, 128);
        int existingValue = RepairKitItem.getStoredValue(kitStack);

        // For preview, show total potential (existing + new)
        lastStoredValue = Math.min(existingValue + potentialValue, 128);
        lastMaterial = currentKitMaterial.isEmpty() ? ingotMaterial : currentKitMaterial;
    }

    private void attemptRepair() {
        ItemStack tool = container.getItem(0);
        ItemStack ingot = container.getItem(1);

        // If no tool, no ingot, or tool is undamaged, do nothing
        if (tool.isEmpty() || tool.getDamageValue() <= 0 || ingot.isEmpty()) {
            return;
        }

        // Check if the ingot is a valid material
        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) return;

        String ingotMaterial = mat.getName();

        // Get the tool's material
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) return;
        String toolMaterial = arsenalTool.getMaterialName(tool);

        // CRITICAL: Check if ingot material matches tool material
        if (!toolMaterial.isEmpty() && !toolMaterial.equals(ingotMaterial)) {
            // Tool material doesn't match ingot material - CANNOT REPAIR
            return;
        }

        String currentKitMaterial = RepairKitItem.getMaterial(kitStack);
        boolean materialLocked = RepairKitItem.isMaterialLocked(kitStack);

        // KIT MATERIAL COMPATIBILITY CHECK
        if (!currentKitMaterial.isEmpty()) {
            if (materialLocked) {
                // Material is locked - must match exactly
                if (!currentKitMaterial.equals(ingotMaterial)) {
                    return; // Different material and locked - CANNOT USE
                }
            } else {
                // Material not locked - can change if different
                if (!currentKitMaterial.equals(ingotMaterial)) {
                    // Different material - change material and reset stored value
                    RepairKitItem.setMaterial(kitStack, ingotMaterial);
                    RepairKitItem.setStoredValue(kitStack, 0);
                    currentKitMaterial = ingotMaterial; // Update for later use
                }
            }
        } else {
            // No material set yet - set it to ingot material
            RepairKitItem.setMaterial(kitStack, ingotMaterial);
            currentKitMaterial = ingotMaterial;
        }

        // Now we know materials are compatible, proceed with repair
        int costPerDurability = arsenalTool.getRepairValuePerDurability(tool);

        if (costPerDurability <= 0) return;

        // Get current stored value
        int storedValue = RepairKitItem.getStoredValue(kitStack);

        // Calculate how much value we can add from ingots (but don't exceed what we need)
        int maxNeededForOneRepair = Math.max(0, costPerDurability - storedValue);
        if (maxNeededForOneRepair <= 0) {
            // We already have enough stored value for at least one repair
            repairWithStoredValue(tool, arsenalTool, costPerDurability, storedValue);
            return;
        }

        // Calculate how many ingots we need for one repair
        int valuePerIngot = 2;
        int valueNeeded = Math.min(maxNeededForOneRepair, 128 - storedValue);
        int ingotsNeeded = (valueNeeded + valuePerIngot - 1) / valuePerIngot; // Round up

        if (ingotsNeeded <= 0) return;

        // Make sure we don't consume more ingots than available
        int ingotsToConsume = Math.min(ingotsNeeded, ingot.getCount());
        if (ingotsToConsume <= 0) return;

        // Calculate actual value added
        int valueAdded = ingotsToConsume * valuePerIngot;

        // Add value to stored pool
        RepairKitItem.setStoredValue(kitStack, storedValue + valueAdded);

        // Consume ingots
        ingot.shrink(ingotsToConsume);

        // Lock material once we add value
        if (!RepairKitItem.isMaterialLocked(kitStack)) {
            RepairKitItem.setMaterialLocked(kitStack, true);
        }

        // Now try to repair with the newly added value
        storedValue = RepairKitItem.getStoredValue(kitStack);
        repairWithStoredValue(tool, arsenalTool, costPerDurability, storedValue);
    }

    private void repairWithStoredValue(ItemStack tool, ArsenalTool arsenalTool, int costPerDurability, int storedValue) {
        if (storedValue < costPerDurability) {
            return; // Not enough value to repair
        }

        // Calculate how much durability we can repair
        int durabilityToRepair = storedValue / costPerDurability;
        if (durabilityToRepair <= 0) return;

        int currentDamage = tool.getDamageValue();
        int actualRepair = Math.min(durabilityToRepair, currentDamage);

        if (actualRepair <= 0) return;

        // Repair the tool
        tool.setDamageValue(currentDamage - actualRepair);

        // Calculate value used
        int valueUsed = actualRepair * costPerDurability;

        // Deduct the cost
        RepairKitItem.setStoredValue(kitStack, storedValue - valueUsed);

        // If tool is fully repaired or we have no more stored value, unlock material
        if (tool.getDamageValue() <= 0 || RepairKitItem.getStoredValue(kitStack) == 0) {
            RepairKitItem.setMaterialLocked(kitStack, false);
        }
    }

    // Method to update client-side preview (called by packet handler)
    public void setClientPreview(int value, String material) {
        this.clientPreviewValue = value;
        this.clientPreviewMaterial = material;
    }

    // Getters for the screen
    public int getCalculatedValue() {
        // On client, use synced data; on server, use calculated data
        if (player.level().isClientSide()) {
            return clientPreviewValue;
        } else {
            return lastStoredValue;
        }
    }

    public String getCalculatedMaterial() {
        if (player.level().isClientSide()) {
            return clientPreviewMaterial;
        } else {
            return lastMaterial;
        }
    }

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