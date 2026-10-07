package com.arsenalcrafting.mod.client.menu;

import com.arsenalcrafting.mod.component.RepairKitData;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.RepairDebug;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.items.tool.powers.RepairReduction;
import com.arsenalcrafting.mod.client.GuiDebug;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.registry.ModComponents;
import com.arsenalcrafting.mod.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * RepairKitMenu — repair kit logic (server repairs, client predicts).
 *
 * Slot 0 = tool, slot 1 = ingots.
 *   ingots -> storedValue (+2 each, cap 128)
 *   tool   -> storedValue repairs its damage every tick
 *
 * Mutates the player's actual held kit stack (not a copy) so contents
 * survive closing the GUI. Repairs are server-authoritative.
 */
public class RepairKitMenu extends AbstractContainerMenu {

    // ── FIELDS ─────────────────────────────────────────────────────────────

    private final Container container = new SimpleContainer(2);
    public final ItemStack kitStack;
    private final Player player;

    /** Last-seen slot contents, for change detection in broadcastChanges(). */
    private ItemStack lastIngot = ItemStack.EMPTY;
    private ItemStack lastTool  = ItemStack.EMPTY;

    // Vestigial — set but never read (packet now writes into the kit stack directly).
    private int clientPreviewValue = -1;
    private String clientPreviewMaterial = null;

    // ── CONSTRUCTOR ────────────────────────────────────────────────────────

    /** Menu opened from a packet: the kit is whatever is in the main hand. */
    public RepairKitMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, playerInventory.player.getMainHandItem());
    }

    public RepairKitMenu(int id, Inventory inv, ItemStack kitStack) {
        super(ModMenus.REPAIRKIT.get(), id);
        this.kitStack = kitStack;
        this.player   = inv.player;

        RepairKitData data = RepairKitItem.getData(kitStack);
        container.setItem(0, data.tool().orElse(ItemStack.EMPTY).copy());
        container.setItem(1, data.ingot().orElse(ItemStack.EMPTY).copy());

        lastIngot = container.getItem(1).copy();
        lastTool  = container.getItem(0).copy();
        if (!player.level().isClientSide()) {
            GuiDebug.slots("RepairKit", "init tool=" + lastTool.getCount() + " ingot=" + lastIngot.getCount());
        }

        addSlot(new Slot(container, 0, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return stack.getItem() instanceof ArsenalTool;
            }
        });

        addSlot(new Slot(container, 1, 80, 16));

        addPlayerInventory(inv);
    }

    // ── SLOT CHANGE HANDLING ───────────────────────────────────────────────

    /** Called every tick; reacts to slot changes (polling, not button-driven). */
    @Override
    public void broadcastChanges() {
        super.broadcastChanges();

        ItemStack currentIngot = container.getItem(1);
        ItemStack currentTool  = container.getItem(0);

        boolean ingotChanged = !ItemStack.isSameItemSameComponents(lastIngot, currentIngot)
                || lastIngot.getCount() != currentIngot.getCount();
        boolean toolChanged  = !ItemStack.isSameItemSameComponents(lastTool,  currentTool);

        if (!ingotChanged && !toolChanged) return;

        GuiDebug.slots("RepairKit", "changed tool=" + currentTool.getCount() + " ingot=" + currentIngot.getCount());

        lastIngot = currentIngot.copy();
        lastTool  = currentTool.copy();

        if (ingotChanged) consumeIngot();
        if (toolChanged || ingotChanged || !player.level().isClientSide()) repairTool();
    }

    // ── INGOT → VALUE ──────────────────────────────────────────────────────

    /** Slot 1 changed: convert ingots to stored value. Same material → add; different → wipe. */
    private void consumeIngot() {
        ItemStack ingot = container.getItem(1);
        ItemStack countedIngot = RepairKitItem.getIngot(kitStack);

        if (ItemStack.isSameItemSameComponents(ingot, countedIngot) && ingot.getCount() == countedIngot.getCount()) {
            return;
        }

        if (player.level().isClientSide()) {
            updateClientPreview();
            return;
        }

        if (ingot.isEmpty()) {
            if (!countedIngot.isEmpty()) {
                RepairKitItem.setStoredValue(kitStack, 0);
                RepairKitItem.setIngot(kitStack, ItemStack.EMPTY);
            }
            return;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) return;

        String ingotMat = mat.getName();
        String storedMat = RepairKitItem.getMaterial(kitStack);

        if (!storedMat.isEmpty() && !storedMat.equals(ingotMat)) {
            RepairKitItem.setMaterial(kitStack, "");
            RepairKitItem.setStoredValue(kitStack, 0);
        }

        int storedVal = RepairKitItem.getStoredValue(kitStack);
        int valueToAdd = Math.min(ingot.getCount() * 2, 128 - storedVal);
        if (valueToAdd <= 0) return;

        int ingotsToCount = (int) Math.ceil(valueToAdd / 2.0f);

        RepairKitItem.setMaterial(kitStack, ingotMat);
        RepairKitItem.setStoredValue(kitStack, storedVal + (ingotsToCount * 2));
        RepairKitItem.setIngot(kitStack, ingot.copy());
    }

    // HONEST FLAG: the ingot→value math lives 3x (consumeIngot, updateClientPreview,
    // calculateClientValue). Change all three together.

    /** Client-only mirror of consumeIngot() for the bar — math duplicated, keep in sync. */
    private void updateClientPreview() {
        ItemStack ingot = container.getItem(1);
        int realValue = RepairKitItem.getStoredValue(kitStack);
        String realMaterial = RepairKitItem.getMaterial(kitStack);

        if (ingot.isEmpty()) {
            clientPreviewValue = realValue;
            clientPreviewMaterial = realMaterial;
            GuiDebug.slots("RepairKit", "preview reset value=" + realValue + " material=" + realMaterial);
            return;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) {
            clientPreviewValue = realValue;
            clientPreviewMaterial = realMaterial;
            GuiDebug.slots("RepairKit", "preview null mat value=" + realValue + " material=" + realMaterial);
            return;
        }

        String ingotMat = mat.getName();

        if (!realMaterial.isEmpty() && !realMaterial.equals(ingotMat)) {
            int space = 128;
            int valueToAdd = Math.min(ingot.getCount() * 2, space);
            int ingotsToCount = (int) Math.ceil(valueToAdd / 2.0f);
            clientPreviewValue = ingotsToCount * 2;
            clientPreviewMaterial = ingotMat;
            GuiDebug.slots("RepairKit", "preview override value=" + clientPreviewValue + " material=" + clientPreviewMaterial);
            return;
        }

        int valueToAdd = Math.min(ingot.getCount() * 2, 128 - realValue);
        int ingotsToCount = (int) Math.ceil(valueToAdd / 2.0f);
        clientPreviewValue = realValue + (ingotsToCount * 2);
        clientPreviewMaterial = ingotMat;
        GuiDebug.slots("RepairKit", "preview add value=" + clientPreviewValue + " material=" + clientPreviewMaterial);
    }

    // ── REPAIR ─────────────────────────────────────────────────────────────

    /** Repairs the tool from stored value if its material matches the kit. */
    private void repairTool() {
        ItemStack tool = container.getItem(0);
        if (tool.isEmpty() || !(tool.getItem() instanceof ArsenalTool arsenalTool)) return;
        if (tool.getDamageValue() <= 0) return;

        String toolMat = arsenalTool.getMaterialName(tool);
        String kitMat = RepairKitItem.getMaterial(kitStack);
        int stored = RepairKitItem.getStoredValue(kitStack);

        if (stored <= 0) return;

        if (!toolMat.isEmpty() && !kitMat.isEmpty() && !toolMat.equals(kitMat)) return;

        repairWithStoredValue(tool, arsenalTool, stored);
    }

    /**
     * Core repair math: 1 value ≈ 15%/efficiency of max durability.
     * repair_reduction core swaps in RepairReduction's numbers.
     * Server repairs, then syncs the client via SyncRepairKitPreviewPacket.
     */
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

        float totalRepairPct = storedValue * repairPctPerVal;
        int   maxPossible    = (int) (maxDurability * totalRepairPct / 100.0f);
        int   actualRepair   = Math.min(maxPossible, currentDamage);
        if (actualRepair <= 0) {
            RepairDebug.repair("StoredValue", "No repair needed or possible (storedValue=" + storedValue + ", actualRepair=" + actualRepair + ")");
            return;
        }

        tool.setDamageValue(currentDamage - actualRepair);
        if (!player.level().isClientSide()) {
            com.arsenalcrafting.mod.items.tool.BrokenToolHandler.markRepaired(tool);
        }

        float pctRepaired = (actualRepair * 100.0f) / maxDurability;
        int valueUsed = hasRepairReduction
                ? RepairReduction.calculateValueUsed(pctRepaired, efficiency, storedValue)
                : Math.max(1, (int) Math.ceil((pctRepaired * efficiency) / 15.0f));

        valueUsed = Math.min(valueUsed, storedValue);
        int remaining = storedValue - valueUsed;
        RepairKitItem.setStoredValue(kitStack, remaining);
        if (remaining <= 0) {
            RepairKitItem.setMaterial(kitStack, "");
        }

        int ingotsToShrink = (int) Math.ceil(valueUsed / 2.0f);
        ItemStack countedIngot = RepairKitItem.getIngot(kitStack);
        if (!countedIngot.isEmpty()) {
            ingotsToShrink = Math.min(ingotsToShrink, countedIngot.getCount());
            countedIngot.shrink(ingotsToShrink);
            RepairKitItem.setIngot(kitStack, countedIngot.isEmpty() ? ItemStack.EMPTY : countedIngot);

            ItemStack slotIngot = container.getItem(1);
            if (!slotIngot.isEmpty()) {
                int slotShrink = Math.min(ingotsToShrink, slotIngot.getCount());
                slotIngot.shrink(slotShrink);
            }
        }

        if (!player.level().isClientSide()) {
            PacketHandler.sendToPlayer(
                    (net.minecraft.server.level.ServerPlayer) player,
                    new com.arsenalcrafting.mod.network.SyncRepairKitPreviewPacket(
                            RepairKitItem.getStoredValue(kitStack),
                            RepairKitItem.getMaterial(kitStack))
            );
        }

        RepairDebug.repair("Repaired", "durability " + (maxDurability - currentDamage) + " -> " + (maxDurability - currentDamage + actualRepair) + " (repaired " + actualRepair + " for " + valueUsed + " value)");
    }

    /** True if the tool's core is the repair-reduction core. */
    private boolean hasRepairReductionCore(ItemStack tool) {
        ToolAssemblyData data = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (data == null || data.isEmpty()) return false;
        AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
        if (coreMat == null) return false;
        CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
        return def != null && def.id().equals("repair_reduction");
    }

    // ── CLIENT PREVIEW ─────────────────────────────────────────────────────

    /** Server pushes authoritative value+material to the client (from packet). */
    public void setClientPreview(int storedValue, String material) {
        RepairKitItem.setMaterial(kitStack, material);
        RepairKitItem.setStoredValue(kitStack, storedValue);
    }

    /** Value/material the screen should show — client predicts, server reads real. */
    public int getCalculatedValue() {
        if (player.level().isClientSide()) {
            return calculateClientValue();
        }
        return RepairKitItem.getStoredValue(kitStack);
    }

    /** Value/material the screen should show — client predicts, server reads real. */
    public String getCalculatedMaterial() {
        if (player.level().isClientSide()) {
            return calculateClientMaterial();
        }
        return RepairKitItem.getMaterial(kitStack);
    }

    /** Client prediction of what storedValue WILL be with the current slot 1. */
    private int calculateClientValue() {
        ItemStack ingot = container.getItem(1);
        int realValue = RepairKitItem.getStoredValue(kitStack);

        if (ingot.isEmpty()) {
            return realValue;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) {
            return realValue;
        }

        String realMaterial = RepairKitItem.getMaterial(kitStack);
        String ingotMat = mat.getName();

        if (!realMaterial.isEmpty() && !realMaterial.equals(ingotMat)) {
            int valueToAdd = Math.min(ingot.getCount() * 2, 128);
            int ingotsToCount = (int) Math.ceil(valueToAdd / 2.0f);
            return ingotsToCount * 2;
        }

        int valueToAdd = Math.min(ingot.getCount() * 2, 128 - realValue);
        int ingotsToCount = (int) Math.ceil(valueToAdd / 2.0f);
        return realValue + (ingotsToCount * 2);
    }

    /** Client prediction of what material WILL be with the current slot 1. */
    private String calculateClientMaterial() {
        ItemStack ingot = container.getItem(1);
        String realMaterial = RepairKitItem.getMaterial(kitStack);

        if (ingot.isEmpty()) {
            return realMaterial;
        }

        AlloyMaterial mat = AlloyMaterial.fromItem(ingot.getItem());
        if (mat == null) {
            return realMaterial;
        }

        return mat.getName();
    }

    // ── LIFECYCLE ──────────────────────────────────────────────────────────

    /** Close: persist slot contents back into the kit (server only). */
    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            RepairKitItem.setTool(kitStack, container.getItem(0));
            RepairKitItem.setIngot(kitStack, container.getItem(1));
        }
    }

    /** Shift-click not implemented — always returns EMPTY. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** Kit never invalidates, even if moved out of the inventory. */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** Standard 36-slot player inventory layout. */
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
