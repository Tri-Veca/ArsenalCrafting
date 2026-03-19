package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.component.RepairKitData;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.items.tool.powers.RepairReduction;
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

    // Server-side display values — sent to client via packet
    private int displayValue = 0;
    private String displayMaterial = "";

    // Change detection
    private ItemStack lastIngot = ItemStack.EMPTY;
    private ItemStack lastTool  = ItemStack.EMPTY;

    // Client-side cached values — set exclusively by sync packet
    private int clientValue = 0;
    private String clientMaterial = "";

    // ── CLIENT CONSTRUCTOR ────────────────────────────────────────────────────

    public RepairKitMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.getMainHandItem());
        // Client starts at zero — server sync packet fills in the real values
    }

    // ── SERVER CONSTRUCTOR ────────────────────────────────────────────────────

    public RepairKitMenu(int id, Inventory inv, ItemStack kitStack) {
        super(ModMenus.REPAIRKIT.get(), id);
        this.kitStack = kitStack;
        this.player   = inv.player;

        // Restore slots from saved kit data
        RepairKitData data = RepairKitItem.getData(kitStack);
        container.setItem(0, data.tool().orElse(ItemStack.EMPTY).copy());
        container.setItem(1, data.ingot().orElse(ItemStack.EMPTY).copy());

        // Seed change detection so broadcastChanges() doesn't false-fire on tick 1
        lastIngot = container.getItem(1).copy();
        lastTool  = container.getItem(0).copy();

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

        // Run repair + calculate display on open, then sync to client.
        // This fixes the "reseating required after reopen" bug — the server
        // always sends fresh state when the menu is constructed.
        if (!player.level().isClientSide()) {
            attemptRepair();
            recalcDisplay();
            syncToClient();
        }
    }

    // ── BROADCAST ────────────────────────────────────────────────────────────

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (player.level().isClientSide()) return;

        ItemStack currentIngot = container.getItem(1);
        ItemStack currentTool  = container.getItem(0);

        boolean ingotChanged = !ItemStack.isSameItemSameComponents(lastIngot, currentIngot);
        boolean toolChanged  = !ItemStack.isSameItemSameComponents(lastTool,  currentTool);

        if (!ingotChanged && !toolChanged) return;

        lastIngot = currentIngot.copy();
        lastTool  = currentTool.copy();

        attemptRepair();
        recalcDisplay();
        syncToClient();
    }

    // ── DISPLAY CALCULATION ───────────────────────────────────────────────────

    /**
     * Pure read — figures out what value and material to show on the bar.
     * Never modifies kit data.
     */
    private void recalcDisplay() {
        ItemStack tool  = container.getItem(0);
        ItemStack ingot = container.getItem(1);
        RepairKitData data = RepairKitItem.getData(kitStack);

        // No ingot — show actual stored value
        if (ingot.isEmpty()) {
            displayValue    = data.storedValue();
            displayMaterial = data.material();
            return;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) {
            // Unknown item — show 0
            displayValue    = 0;
            displayMaterial = data.material();
            return;
        }

        String ingotMat = mat.getName();

        // Material mismatch with stored kit material → show 0
        String kitMat = RepairKitItem.getData(kitStack).material();
        if (!kitMat.isEmpty() && !kitMat.equals(ingotMat)) {
            displayValue    = 0;
            displayMaterial = ingotMat;
            return;
        }

// Material mismatch with tool head → show 0
        if (!tool.isEmpty() && tool.getItem() instanceof ArsenalTool arsenalTool) {
            String toolMat = arsenalTool.getMaterialName(tool);
            if (!toolMat.isEmpty() && !toolMat.equals(ingotMat)) {
                displayValue    = 0;
                displayMaterial = ingotMat;
                return;
            }
        }

        // Show theoretical value from ingots in slot
        displayValue    = Math.min(ingot.getCount() * 2, 128);
        displayMaterial = ingotMat;
    }

    // ── REPAIR LOGIC ─────────────────────────────────────────────────────────

    private void attemptRepair() {
        ItemStack tool  = container.getItem(0);
        ItemStack ingot = container.getItem(1);

        if (tool.isEmpty()) return;
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) return;
        if (tool.getDamageValue() <= 0) return; // Already full

        // ── Repair with stored value if no ingot present ──────────────────────
        if (ingot.isEmpty()) {
            int stored = RepairKitItem.getStoredValue(kitStack);
            if (stored <= 0) return;

            // Stored material must match tool
            String kitMat  = RepairKitItem.getMaterial(kitStack);
            String toolMat = arsenalTool.getMaterialName(tool);
            if (!toolMat.isEmpty() && !kitMat.equals(toolMat)) return;

            repairWithStoredValue(tool, arsenalTool, stored);
            return;
        }

        // ── Ingot present — validate material ────────────────────────────────
        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) return;

        String ingotMat = mat.getName();
        String toolMat  = arsenalTool.getMaterialName(tool);

        if (!toolMat.isEmpty() && !toolMat.equals(ingotMat)) return;

        // If kit has stored value from a DIFFERENT material, reset it
        // Player chose to swap materials — they lose any leftover value
        String kitMat = RepairKitItem.getMaterial(kitStack);
        if (!kitMat.isEmpty() && !kitMat.equals(ingotMat)) {
            RepairKitItem.setStoredValue(kitStack, 0); // withStoredValue(0) clears material too
        }

        // Repair with any existing stored value first
        int stored = RepairKitItem.getStoredValue(kitStack);
        if (stored > 0 && tool.getDamageValue() > 0) {
            repairWithStoredValue(tool, arsenalTool, stored);
            stored = RepairKitItem.getStoredValue(kitStack);
        }

        if (tool.getDamageValue() <= 0) return; // Fully repaired
        if (stored >= 128) return;               // Storage full

        // Calculate how much value we need to add from ingots
        float efficiency       = arsenalTool.getRepairEfficiency(tool);
        float repairPctPerVal  = 15.0f / efficiency;
        float minRepairPct     = 10.0f;
        int   minValNeeded     = (int) Math.ceil(minRepairPct / repairPctPerVal);

        int valueNeeded   = Math.max(0, minValNeeded - stored);
        int maxCanAdd     = 128 - stored;
        int valueToAdd    = Math.min(valueNeeded, maxCanAdd);
        if (valueToAdd <= 0) return;

        int ingotsNeeded = (int) Math.ceil(valueToAdd / 2.0f);
        ingotsNeeded     = Math.min(ingotsNeeded, ingot.getCount());
        if (ingotsNeeded <= 0) return;

        int actualAdded = ingotsNeeded * 2;
        RepairKitItem.setMaterial(kitStack, ingotMat);
        RepairKitItem.setStoredValue(kitStack, stored + actualAdded);
        ingot.shrink(ingotsNeeded);

        // Repair with the newly stored value
        stored = RepairKitItem.getStoredValue(kitStack);
        if (stored > 0 && tool.getDamageValue() > 0) {
            repairWithStoredValue(tool, arsenalTool, stored);
        }
    }

    private void repairWithStoredValue(ItemStack tool, ArsenalTool arsenalTool, int storedValue) {
        if (storedValue <= 0) return;
        int currentDamage = tool.getDamageValue();
        if (currentDamage <= 0) return;

        float efficiency    = arsenalTool.getRepairEfficiency(tool);
        int   maxDurability = tool.getMaxDamage();
        boolean hasRepairReduction = hasRepairReductionCore(tool);

        float repairPctPerVal = hasRepairReduction
                ? RepairReduction.calculateRepairPercent(efficiency)
                : 15.0f / efficiency;

        float totalRepairPct  = storedValue * repairPctPerVal;
        int   maxPossible     = (int) (maxDurability * totalRepairPct / 100.0f);
        int   actualRepair    = Math.min(maxPossible, currentDamage);
        if (actualRepair <= 0) return;

        tool.setDamageValue(currentDamage - actualRepair);
        com.arsenalcrafting.mod.items.tool.BrokenToolHandler.markRepaired(tool);

        float pctRepaired = (actualRepair * 100.0f) / maxDurability;
        int valueUsed = hasRepairReduction
                ? RepairReduction.calculateValueUsed(pctRepaired, efficiency, storedValue)
                : Math.max(1, (int) Math.ceil((pctRepaired * efficiency) / 15.0f));

        valueUsed = Math.min(valueUsed, storedValue);

        // setStoredValue(0) auto-clears material via RepairKitData.withStoredValue()
        RepairKitItem.setStoredValue(kitStack, storedValue - valueUsed);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    private boolean hasRepairReductionCore(ItemStack tool) {
        ToolAssemblyData data = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (data == null || data.isEmpty()) return false;
        AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
        if (coreMat == null) return false;
        CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
        return def != null && def.id().equals("repair_reduction");
    }

    private void syncToClient() {
        if (player instanceof ServerPlayer sp) {
            PacketHandler.sendToPlayer(sp,
                    new SyncRepairKitPreviewPacket(displayValue, displayMaterial));
        }
    }

    // ── CLIENT API ────────────────────────────────────────────────────────────

    public void setClientPreview(int value, String material) {
        this.clientValue    = value;
        this.clientMaterial = material;
    }

    public int getCalculatedValue() {
        if (player.level().isClientSide()) {
            // If packet hasn't arrived yet, read directly from kit data
            // This covers the race condition on initial open
            if (clientValue == 0 && clientMaterial.isEmpty()) {
                return RepairKitItem.getData(kitStack).storedValue();
            }
            return clientValue;
        }
        return displayValue;
    }

    public String getCalculatedMaterial() {
        if (player.level().isClientSide()) {
            // If packet hasn't arrived yet, read directly from kit data
            if (clientValue == 0 && clientMaterial.isEmpty()) {
                return RepairKitItem.getData(kitStack).material();
            }
            return clientMaterial;
        }
        return displayMaterial;
    }

    // ── LIFECYCLE ─────────────────────────────────────────────────────────────

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
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

    // ── PLAYER INVENTORY ──────────────────────────────────────────────────────

    private void addPlayerInventory(Inventory inv) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inv, col, 8 + col * 18, 142));
        }
    }
}