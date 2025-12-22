package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.client.menu.ToolkitMenu;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import com.arsenalcrafting.mod.items.augments.UniversalAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.ToolAugmentSlots;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolHeadItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolRodItem;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModComponents;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

// Toolkit Block Entity handles the inventory and the tool assembly logic.
public class ToolkitBlockEntity extends BlockEntity implements MenuProvider {
    // Inventory slots: Head, Core, Output, Rod, Augments (4 slots)
    public static final int SLOTS = 8;
    private static final int AUGMENT_SLOT_START = 4; // BE Index 4 (AugSlot1)
    private static final int AUGMENT_SLOT_END = 8;   // BE Index 7 (AugSlot4)

    // Slot indices
    private static final int HEAD_SLOT = 0;
    private static final int CORE_SLOT = 1;
    private static final int OUTPUT_SLOT = 2;
    private static final int ROD_SLOT = 3;

    private boolean isAssembling = false;
    private boolean isSyncing = false; // Prevents the recursive loop

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {

            setChanged();
            if (isAssembling || isSyncing) return;

            // Only trigger assembly if we are NOT already assembling,
            // and only for the component/output slots.
            if (!isAssembling && (slot == HEAD_SLOT || slot == CORE_SLOT || slot == ROD_SLOT || slot == OUTPUT_SLOT)) {
                assembleTool();
            }

            // Augment slots trigger augment installation
            if (slot >= AUGMENT_SLOT_START && slot < AUGMENT_SLOT_END) {
                installAugments();
            }

            // When output slot changes, sync augments to GUI
            if (slot == OUTPUT_SLOT) {
                syncAugmentsFromTool();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= AUGMENT_SLOT_START && slot < AUGMENT_SLOT_END) {
                // Only allow empty or augment items in augment slots
                // When a tool is in output, we populate these automatically
                return stack.isEmpty() || stack.getItem() instanceof InstallableAugmentItem;
            }
            if (switch (slot) {
                case HEAD_SLOT -> isToolHead(stack.getItem()); // Slot 0: Head
                case CORE_SLOT -> isCore(stack.getItem()); // Slot 1: Core
                case ROD_SLOT -> isToolRod(stack.getItem());  // Slot 3: Rod
                case OUTPUT_SLOT -> stack.getItem() instanceof ArsenalTool; // ALLOW putting assembled tools back!
                default -> false; // Don't allow other items in unused slots for now
            }) return true;
            else return false;
        }
    };

    // Helper method to check if item is a tool head (old or new system)
    private boolean isToolHead(Item item) {
        if (item instanceof ToolComponentItem toolComponent) {
            // This ensures Shovel, Axe, and Pickaxe heads are all recognized
            return toolComponent.getComponentType().isHead();
        }
        return false;
    }


    private Item getResultingTool(ToolComponentType headType) {
        return switch (headType) {
            case SHOVEL_HEAD -> ModItems.ARSENAL_SHOVEL.get();
            //case AXE_HEAD    -> ModItems.ARSENAL_AXE.get();
            case PICKAXE_HEAD -> ModItems.ARSENAL_PICKAXE.get();
            default           -> ModItems.ARSENAL_PICKAXE.get();
        };
    }

    private boolean isCore(Item item) {
        // Old system: ToolHeadItem
        if (item instanceof ToolHeadItem) return true;

        // New system: ToolComponentItem with appropriate component type
        if (item instanceof ToolComponentItem toolComponent) {
            ToolComponentType type = toolComponent.getComponentType();
            return type == ToolComponentType.CORE;
        }
        return false;
    }

    // Helper method to check if item is a tool rod (old or new system)
    private boolean isToolRod(Item item) {
        if (item instanceof ToolComponentItem toolComponent) {
            // This ensures Shovel, Axe, and Pickaxe rods are all recognized
            return toolComponent.getComponentType().isRod();
        }
        return false;
    }

    // Helper method to get material from any tool component (old or new system)
    private com.arsenalcrafting.mod.core.alloy.AlloyMaterial getMaterialFromComponent(Item item) {
        if (item instanceof ToolHeadItem headItem) {
            return headItem.getMaterial();
        }
        if (item instanceof ToolRodItem rodItem) {
            return rodItem.getMaterial();
        }
        if (item instanceof ToolComponentItem toolComponent) {
            return toolComponent.getMaterial();
        }
        return null;
    }

    /**
     * Sync augments from tool to GUI slots
     */
    public void syncAugmentsFromTool() {
        if (isSyncing) return; // Guard against recursive loops
        isSyncing = true;

        try {
            ItemStack outputTool = itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (outputTool.isEmpty() || !(outputTool.getItem() instanceof ArsenalTool arsenalTool)) {
                for (int i = AUGMENT_SLOT_START; i < AUGMENT_SLOT_END; i++) {
                    itemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
                return;
            }

            ToolAugmentSlots slots = arsenalTool.getAugmentSlots(outputTool);

            for (int i = 0; i < 4; i++) {
                int toolkitSlot = i + AUGMENT_SLOT_START;
                String augmentId = slots.getAugmentInSlot(i);

                if (augmentId != null && !augmentId.isEmpty()) {
                    int tier = AugmentManager.getTier(augmentId, outputTool);
                    ItemStack augmentItem = findAugmentItem(augmentId, tier);

                    // Using setStackInSlot triggers onContentsChanged, but our flag will block the loop
                    itemHandler.setStackInSlot(toolkitSlot, augmentItem);
                } else {
                    itemHandler.setStackInSlot(toolkitSlot, ItemStack.EMPTY);
                }
            }

            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        } finally {
            isSyncing = false; // Always release the flag
        }
    }

    /**
     * Install augments from Toolkit slots to the tool
     */
    public void installAugments() {
        ItemStack outputTool = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (outputTool.isEmpty() || !(outputTool.getItem() instanceof ArsenalTool arsenalTool)) {
            return;
        }

        ToolAugmentSlots slots = arsenalTool.getAugmentSlots(outputTool);
        boolean changed = false;

        for (int toolkitSlot = AUGMENT_SLOT_START; toolkitSlot < AUGMENT_SLOT_END; toolkitSlot++) {
            ItemStack augmentStack = itemHandler.getStackInSlot(toolkitSlot);
            int toolSlot = toolkitSlot - AUGMENT_SLOT_START;

            // FIX: Check for the general interface first so tier-less augs aren't ignored
            if (!augmentStack.isEmpty() && augmentStack.getItem() instanceof InstallableAugmentItem installable) {
                String augmentId = "";
                int tier = 0;

                // If it's a UniversalAugmentItem, we can safely grab ID and Tier
                if (installable instanceof UniversalAugmentItem uni) {
                    augmentId = uni.getAugmentId();
                    tier = uni.getTier();
                }
                // Add other specific item classes here if they don't extend UniversalAugmentItem

                if (augmentId.isEmpty()) continue;

                if (!AugmentManager.canInstallOnToolType(augmentId, arsenalTool.getToolType())) {
                    continue;
                }

                String currentAugmentId = slots.getAugmentInSlot(toolSlot);
                int currentTier = currentAugmentId != null ? AugmentManager.getTier(currentAugmentId, outputTool) : 0;

                if (!augmentId.equals(currentAugmentId) || tier != currentTier) {
                    if (currentAugmentId != null && !currentAugmentId.equals(augmentId)) {
                        AugmentManager.uninstall(currentAugmentId, outputTool);
                    }

                    slots = slots.withSlot(toolSlot, augmentId);
                    AugmentManager.install(augmentId, outputTool, tier);
                    changed = true;
                }
            } else if (augmentStack.isEmpty()) {
                String currentAugmentId = slots.getAugmentInSlot(toolSlot);
                if (currentAugmentId != null) {
                    slots = slots.clearSlot(toolSlot);
                    AugmentManager.uninstall(currentAugmentId, outputTool);
                    changed = true;
                }
            }
        }

        if (changed) {
            arsenalTool.setAugmentSlots(outputTool, slots);
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    /**
     * Remove an augment from a specific slot and return it to the Toolkit
     */
    public void removeAugmentFromTool(int toolSlotIndex) {
        if (toolSlotIndex < 0 || toolSlotIndex >= 4) return;

        ItemStack outputTool = itemHandler.getStackInSlot(OUTPUT_SLOT);

        if (outputTool.isEmpty() || !(outputTool.getItem() instanceof ArsenalTool arsenalTool)) {
            return;
        }

        ToolAugmentSlots slots = arsenalTool.getAugmentSlots(outputTool);
        String augmentId = slots.getAugmentInSlot(toolSlotIndex);

        if (augmentId != null) {
            // Get the tier of the augment
            int tier = AugmentManager.getTier(augmentId, outputTool);

            // Create the augment item to return
            ItemStack augmentItem = findAugmentItem(augmentId, tier);

            if (!augmentItem.isEmpty()) {
                int toolkitSlot = toolSlotIndex + AUGMENT_SLOT_START;

                // Check if Toolkit slot is empty
                if (itemHandler.getStackInSlot(toolkitSlot).isEmpty()) {
                    // Return augment to Toolkit slot
                    itemHandler.setStackInSlot(toolkitSlot, augmentItem);

                    // Remove from tool
                    slots = slots.clearSlot(toolSlotIndex);
                    AugmentManager.uninstall(augmentId, outputTool);

                    arsenalTool.setAugmentSlots(outputTool, slots);
                    setChanged();

                    System.out.println("[Toolkit] Augment " + augmentId + " removed from slot " + toolSlotIndex);
                } else {
                    System.out.println("[Toolkit] Cannot remove augment - Toolkit slot " + toolkitSlot + " is occupied");
                }
            }
        }
    }

    private ItemStack findAugmentItem(String augmentId, int tier) {
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof UniversalAugmentItem uniItem) {
                if (uniItem.getAugmentId().equals(augmentId)) {
                    // If the tool has a tier (1, 2, 3), we MUST match the item tier exactly.
                    // If the tool has no tier (0), we just take the first item with that ID.
                    if (tier <= 1 || uniItem.getTier() == tier) {
                        return new ItemStack(uniItem);
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    public ToolkitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOOLKIT.get(), pos, state);
    }

    // --- Tool Assembly Logic ---
    private void assembleTool() {
        if (isAssembling) return;
        isAssembling = true;

        try {
            ItemStack headStack   = itemHandler.getStackInSlot(HEAD_SLOT);
            ItemStack coreStack   = itemHandler.getStackInSlot(CORE_SLOT);
            ItemStack outputStack = itemHandler.getStackInSlot(OUTPUT_SLOT);
            ItemStack rodStack    = itemHandler.getStackInSlot(ROD_SLOT);

            boolean hasHeadInput = !headStack.isEmpty();
            boolean hasCoreInput = !coreStack.isEmpty();
            boolean hasRodInput  = !rodStack.isEmpty();

            if (!hasHeadInput && !hasCoreInput && !hasRodInput && outputStack.isEmpty()) return;
            if (!outputStack.isEmpty() && !(outputStack.getItem() instanceof ArsenalTool)) return;

            boolean isNewTool = outputStack.isEmpty();
            if (isNewTool && (!hasHeadInput || !hasCoreInput || !hasRodInput)) return;

            // --- Read existing assembly data for SWAPPING ---
            String oldHeadMat = "";
            String oldRodMat  = "";
            String oldCoreMat = "";
            ToolAssemblyData existingData = outputStack.get(ModComponents.TOOL_ASSEMBLY_DATA.get());

            if (existingData != null) {
                oldHeadMat = existingData.headMaterial();
                oldRodMat  = existingData.rodMaterial();
                oldCoreMat = existingData.coreMaterial();
            }

            // --- Prepare material names for the NEW tool ---
            String headMatName = (hasHeadInput) ? getMaterialFromComponent(headStack.getItem()).getName() : oldHeadMat;
            String coreMatName = (hasCoreInput) ? getMaterialFromComponent(coreStack.getItem()).getName() : oldCoreMat;
            String rodMatName  = (hasRodInput)  ? getMaterialFromComponent(rodStack.getItem()).getName()  : oldRodMat;

            if (headMatName == null || rodMatName == null || coreMatName == null) return;

            // --- Build or copy the tool ---
            ItemStack assembledTool;
            if (isNewTool) {
                ToolComponentItem headComp = (ToolComponentItem) headStack.getItem();
                assembledTool = new ItemStack(getResultingTool(headComp.getComponentType()));
            } else {
                assembledTool = outputStack.copy();
            }

            assembledTool.set(ModComponents.TOOL_ASSEMBLY_DATA.get(), new ToolAssemblyData(headMatName, rodMatName, coreMatName));

            // --- Handle Input Consumption and Swapping ---

            // 1. ROD SWAP (Index 3)
            if (hasRodInput) {
                ItemStack swapRod = ItemStack.EMPTY;
                if (!oldRodMat.isEmpty() && !outputStack.isEmpty()) {
                    // Use the Tool's RodType to ensure we get the correct rod back (Shovel Rod, Pickaxe Rod, etc.)
                    ToolComponentType oldRodType = ((ArsenalTool)outputStack.getItem()).getRodType();
                    swapRod = findComponentStack(oldRodType, oldRodMat);
                }
                itemHandler.setStackInSlot(ROD_SLOT, swapRod);
            }

            // 2. CORE SWAP (Index 1)
            if (hasCoreInput) {
                ItemStack swapCore = ItemStack.EMPTY;
                if (!oldCoreMat.isEmpty()) {
                    swapCore = findComponentStack(ToolComponentType.CORE, oldCoreMat);
                }
                itemHandler.setStackInSlot(CORE_SLOT, swapCore);
            }

            // 3. HEAD SWAP (Index 0)
            if (hasHeadInput) {
                ItemStack swapHead = ItemStack.EMPTY;
                if (!oldHeadMat.isEmpty() && !outputStack.isEmpty()) {
                    // Use the Tool's HeadType to ensure we get a Shovel Head back if it was a Shovel
                    ToolComponentType oldType = ((ArsenalTool)outputStack.getItem()).getHeadType();
                    swapHead = findComponentStack(oldType, oldHeadMat);
                }
                itemHandler.setStackInSlot(HEAD_SLOT, swapHead);
            }

            // Finalize Output
            itemHandler.setStackInSlot(OUTPUT_SLOT, assembledTool);

            syncAugmentsFromTool();
            setChanged();
            if (level != null && !level.isClientSide) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }

        } finally {
            isAssembling = false;
        }
    }

    private ItemStack findComponentStack(ToolComponentType type, String materialName) {
        if (materialName == null || materialName.isEmpty()) {
            return ItemStack.EMPTY;
        }

        // Search all mod items for a matching ToolComponentItem
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof ToolComponentItem comp) {
                if (comp.getComponentType() == type &&
                        comp.getMaterial().getName().equals(materialName)) {

                    return new ItemStack(item);
                }
            }
        }

        // Fallback: nothing found
        return ItemStack.EMPTY;
    }

    // --- Data Handling (NBT Signatures) ---
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        tag.put("Inventory", itemHandler.serializeNBT(provider));
        super.saveAdditional(tag, provider);
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ToolkitBlockEntity entity) {
        // Simple tick logic goes here
    }

    // --- GUI/Menu Provider ---
    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arsenalcrafting.toolkit");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ToolkitMenu(id, inventory, this);
    }

    public IItemHandler getItemHandler() {
        return itemHandler;
    }

    /**
     * Get the augment slot index in the tool (0-3) from Toolkit slot index (4-7)
     */
    public static int getToolSlotFromToolkitSlot(int toolkitSlot) {
        if (toolkitSlot >= AUGMENT_SLOT_START && toolkitSlot < AUGMENT_SLOT_END) {
            return toolkitSlot - AUGMENT_SLOT_START;
        }
        return -1;
    }

    /**
     * Get the Toolkit slot index (4-7) from tool slot index (0-3)
     */
    public static int getToolkitSlotFromToolSlot(int toolSlot) {
        if (toolSlot >= 0 && toolSlot < 4) {
            return toolSlot + AUGMENT_SLOT_START;
        }
        return -1;
    }
}