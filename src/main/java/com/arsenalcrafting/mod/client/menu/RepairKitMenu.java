package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.component.RepairKitData;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.network.SyncRepairKitPreviewPacket;
import com.arsenalcrafting.mod.registry.ModComponents;
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

    // Track last ingot for change detection
    private ItemStack lastIngotStack = ItemStack.EMPTY;
    private String lastIngotMaterial = "";

    // Track last tool for change detection
    private ItemStack lastToolStack = ItemStack.EMPTY;

    // Client-side cached values
    private int clientPreviewValue = 0;
    private String clientPreviewMaterial = "";

    // CLIENT ctor
    public RepairKitMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.getMainHandItem());

        // Initialize client preview with actual kit data when menu opens
        if (playerInventory.player.level().isClientSide()) {
            RepairKitData data = RepairKitItem.getData(kitStack);
            this.clientPreviewValue = data.storedValue();
            this.clientPreviewMaterial = data.material();
            System.out.println("DEBUG: Client initialized preview - Value: " + clientPreviewValue + ", Material: " + clientPreviewMaterial);
        }
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
        this.lastIngotStack = container.getItem(1).copy();
        this.lastIngotMaterial = getMaterialFromStack(container.getItem(1));
        this.lastToolStack = container.getItem(0).copy();

        // Sync initial data to client immediately when menu opens
        if (!player.level().isClientSide()) {
            syncInitialDataToClient();
        }

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

    private void syncInitialDataToClient() {
        if (player instanceof ServerPlayer serverPlayer) {
            // Send initial sync packet to client with current stored values
            PacketHandler.sendToPlayer(serverPlayer,
                    new SyncRepairKitPreviewPacket(lastStoredValue, lastMaterial));

            System.out.println("DEBUG: Initial sync sent - Value: " + lastStoredValue + ", Material: " + lastMaterial);
        }
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

    // Helper to get material name from stack
    private String getMaterialFromStack(ItemStack stack) {
        if (stack.isEmpty()) return "";
        AlloyMaterial mat = AlloyMaterial.fromItem(stack.getItem());
        return mat != null ? mat.getName() : "";
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        // Check if ingot changed (item type or count)
        ItemStack currentIngot = container.getItem(1);
        String currentMaterial = getMaterialFromStack(currentIngot);
        boolean ingotChanged = !ItemStack.isSameItemSameComponents(lastIngotStack, currentIngot) ||
                !lastIngotMaterial.equals(currentMaterial);

        // Check if tool changed
        ItemStack currentTool = container.getItem(0);
        boolean toolChanged = !ItemStack.isSameItemSameComponents(lastToolStack, currentTool);

        if (ingotChanged || toolChanged) {
            if (ingotChanged) {
                lastIngotStack = currentIngot.copy();
                lastIngotMaterial = currentMaterial;
                System.out.println("DEBUG: Ingot changed from " + lastMaterial + " to " + currentMaterial);
            }
            if (toolChanged) {
                lastToolStack = currentTool.copy();
            }

            // Update the stored value calculation based on ingots in slot
            updateStoredValueCalculation();

            // Only process actual repair on server side
            if (!player.level().isClientSide()) {
                attemptRepair();

                // Sync preview data to client
                if (player instanceof ServerPlayer serverPlayer) {
                    // Send sync packet to client
                    PacketHandler.sendToPlayer(serverPlayer,
                            new SyncRepairKitPreviewPacket(lastStoredValue, lastMaterial));

                    System.out.println("DEBUG: Syncing preview - Value: " + lastStoredValue + ", Material: " + lastMaterial);
                }
            }
        }
    }

    private void updateStoredValueCalculation() {
        ItemStack tool = container.getItem(0);
        ItemStack ingot = container.getItem(1);

        if (ingot.isEmpty()) {
            // No ingots - show actual stored value
            lastStoredValue = RepairKitItem.getStoredValue(kitStack);
            lastMaterial = RepairKitItem.getMaterial(kitStack);
            return;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) {
            // Invalid ingot - show 0
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

        // For preview when ingot slot has items:
        if (currentKitMaterial.isEmpty() || (!materialLocked && !currentKitMaterial.equals(ingotMaterial))) {
            // Kit has no material or material is unlocked and different
            lastStoredValue = potentialValue;
            lastMaterial = ingotMaterial; // Preview with new material
        } else {
            // Kit material matches or is locked
            lastStoredValue = potentialValue;
            lastMaterial = currentKitMaterial;
        }
    }

    private void attemptRepair() {
        ItemStack tool = container.getItem(0);
        ItemStack ingot = container.getItem(1);

        // If no tool or no ingot, do nothing
        if (tool.isEmpty() || ingot.isEmpty()) {
            return;
        }

        // CRITICAL FIX: Check if tool needs repair
        if (tool.getDamageValue() <= 0) {
            // Tool is fully repaired, don't consume ingots
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

        // Get current stored value
        int storedValue = RepairKitItem.getStoredValue(kitStack);

        // Check if we have room to store more value
        int maxStoredValue = 128;

        // If already at max storage, only try to repair
        if (storedValue >= maxStoredValue) {
            // Already at max storage, try to repair with existing stored value
            if (storedValue > 0) {
                repairWithStoredValue(tool, arsenalTool, storedValue);
            }
            return;
        }

        // Only add value if we need more for repair
        // Check if current stored value is enough for at least some repair
        if (storedValue > 0) {
            // Try to repair with existing value first
            repairWithStoredValue(tool, arsenalTool, storedValue);

            // After repair, check if we still need more value
            // If tool is still damaged, we can add more value
            if (tool.getDamageValue() <= 0) {
                // Tool is now fully repaired, don't add more value
                return;
            }

            // Update stored value after repair
            storedValue = RepairKitItem.getStoredValue(kitStack);
        }

        // Calculate how much more value we need for meaningful repair
        float efficiency = arsenalTool.getRepairEfficiency(tool);
        float repairPercentPerValue = 15.0f / efficiency;

        // How much percent of repair do we get per value?
        // If we need at least 10% repair to be meaningful
        float minRepairPercent = 10.0f; // Minimum 10% repair worth adding
        int minValueNeeded = (int) Math.ceil(minRepairPercent / repairPercentPerValue);

        // How much value can we add from ingots?
        int valuePerIngot = 2;
        int maxValueToAdd = maxStoredValue - storedValue;

        // Only add enough value to reach minValueNeeded OR fill storage
        int valueNeeded = Math.max(0, minValueNeeded - storedValue);
        int valueToAdd = Math.min(valueNeeded, maxValueToAdd);

        // If we don't need more value for meaningful repair, don't add any
        if (valueToAdd <= 0) {
            return;
        }

        // Calculate how many ingots to consume
        int ingotsToConsume = (valueToAdd + valuePerIngot - 1) / valuePerIngot; // Round up
        ingotsToConsume = Math.min(ingotsToConsume, ingot.getCount());

        if (ingotsToConsume <= 0) {
            return;
        }

        // Calculate actual value added
        int actualValueAdded = ingotsToConsume * valuePerIngot;

        // Add value to stored pool
        RepairKitItem.setStoredValue(kitStack, storedValue + actualValueAdded);

        // Consume ingots
        ingot.shrink(ingotsToConsume);

        // Lock material once we add value
        if (!RepairKitItem.isMaterialLocked(kitStack)) {
            RepairKitItem.setMaterialLocked(kitStack, true);
        }

        // Update stored value
        storedValue = RepairKitItem.getStoredValue(kitStack);

        // Now try to repair with the updated stored value
        if (storedValue > 0 && tool.getDamageValue() > 0) {
            repairWithStoredValue(tool, arsenalTool, storedValue);
        }
    }

    private void repairWithStoredValue(ItemStack tool, ArsenalTool arsenalTool, int storedValue) {
        if (storedValue <= 0) return;

        int currentDamage = tool.getDamageValue();
        if (currentDamage <= 0) return;

        float efficiency = arsenalTool.getRepairEfficiency(tool);
        int maxDurability = tool.getMaxDamage();

        // Check for repair reduction core
        boolean hasRepairReduction = false;
        ToolAssemblyData data = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
            if (coreMat != null) {
                CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat);
                hasRepairReduction = coreDef != null && coreDef.id().equals("repair_reduction");
            }
        }

        // SIMPLE 20% BONUS
        float repairPercentPerValue = 15.0f / efficiency; // Base repair
        if (hasRepairReduction) {
            repairPercentPerValue *= 1.20f; // 20% more repair
        }

        float totalRepairPercent = storedValue * repairPercentPerValue;
        int maxPossibleRepair = (int) (maxDurability * totalRepairPercent / 100.0f);
        int actualRepair = Math.min(maxPossibleRepair, currentDamage);

        if (actualRepair <= 0) return;

        tool.setDamageValue(currentDamage - actualRepair);

        float percentActuallyRepaired = (actualRepair * 100.0f) / maxDurability;
        int valueUsed = Math.max(1, (int) Math.ceil((percentActuallyRepaired * efficiency) / 15.0f));
        if (hasRepairReduction) {
            valueUsed = (int)(valueUsed * 0.833f); // Use 16.7% less value (inverse of 1.20)
        }
        valueUsed = Math.min(valueUsed, storedValue);

        RepairKitItem.setStoredValue(kitStack, storedValue - valueUsed);

        if (RepairKitItem.getStoredValue(kitStack) == 0) {
            RepairKitItem.setMaterialLocked(kitStack, false);
        }
    }



    // Method to update client-side preview (called by packet handler)
    public void setClientPreview(int value, String material) {
        this.clientPreviewValue = value;
        this.clientPreviewMaterial = material;
        System.out.println("DEBUG: Client preview updated - Value: " + value + ", Material: " + material);
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