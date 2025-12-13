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
        ItemStack ingot = container.getItem(1);

        if (ingot.isEmpty()) {
            lastStoredValue = 0;
            lastMaterial = "";
            return;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) {
            lastStoredValue = 0;
            lastMaterial = "";
            return;
        }

        String materialName = mat.getName();
        String currentMaterial = RepairKitItem.getMaterial(kitStack);
        boolean materialLocked = RepairKitItem.isMaterialLocked(kitStack);

        // Check material compatibility
        if (!currentMaterial.isEmpty()) {
            if (materialLocked && !currentMaterial.equals(materialName)) {
                // Different material and locked - cannot use
                lastStoredValue = 0;
                lastMaterial = currentMaterial;
                return;
            }
        }

        // Calculate potential value from ingots in slot
        // 1 ingot = 2 value, up to 128
        int potentialValue = Math.min(ingot.getCount() * 2, 128);

        // Update the display values
        lastStoredValue = potentialValue;
        lastMaterial = materialName;
    }

    private void attemptRepair() {
        ItemStack tool = container.getItem(0);
        ItemStack ingot = container.getItem(1);

        if (tool.isEmpty() || tool.getDamageValue() <= 0 || ingot.isEmpty()) {
            return;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) return;

        String materialName = mat.getName();
        String currentMaterial = RepairKitItem.getMaterial(kitStack);
        boolean materialLocked = RepairKitItem.isMaterialLocked(kitStack);

        // Material check
        if (!currentMaterial.isEmpty()) {
            if (!materialLocked && !currentMaterial.equals(materialName)) {
                RepairKitItem.setMaterial(kitStack, materialName);
                RepairKitItem.setStoredValue(kitStack, 0);
            } else if (materialLocked && !currentMaterial.equals(materialName)) {
                return;
            }
        } else {
            RepairKitItem.setMaterial(kitStack, materialName);
        }

        int storedValue = RepairKitItem.getStoredValue(kitStack);
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) return;
        int costPerDurability = arsenalTool.getRepairValuePerDurability(tool);

        if (costPerDurability <= 0) return;

        // Calculate how much value we can add from ingots
        int valueToAdd = Math.min(ingot.getCount() * 2, 128 - storedValue);
        if (valueToAdd <= 0) return;

        // Add value to stored pool
        RepairKitItem.setStoredValue(kitStack, storedValue + valueToAdd);

        // Consume ingots
        int ingotsToConsume = (valueToAdd + 1) / 2;
        ingot.shrink(ingotsToConsume);

        // Lock material once we add value
        if (!RepairKitItem.isMaterialLocked(kitStack)) {
            RepairKitItem.setMaterialLocked(kitStack, true);
        }

        // Now try to repair
        storedValue = RepairKitItem.getStoredValue(kitStack);

        if (storedValue >= costPerDurability) {
            int durabilityToRepair = storedValue / costPerDurability;
            int currentDamage = tool.getDamageValue();
            int actualRepair = Math.min(durabilityToRepair, currentDamage);

            if (actualRepair > 0) {
                tool.setDamageValue(currentDamage - actualRepair);
                int valueUsed = actualRepair * costPerDurability;
                RepairKitItem.setStoredValue(kitStack, storedValue - valueUsed);

                if (tool.getDamageValue() <= 0 || RepairKitItem.getStoredValue(kitStack) == 0) {
                    RepairKitItem.setMaterialLocked(kitStack, false);
                }
            }
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