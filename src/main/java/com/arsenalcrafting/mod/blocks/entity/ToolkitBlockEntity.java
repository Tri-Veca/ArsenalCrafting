package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.client.menu.ToolkitMenu;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
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
    // Inventory slots: Head, Rod, Binding, Output, Augments (4 slots)
    // For MVP, we only use Head, Rod, and Output (slots 0, 1, 2)
    public static final int SLOTS = 8;
    private static final int AUGMENT_SLOT_START = 4; // BE Index 4 (AugSlot1)
    private static final int AUGMENT_SLOT_END = 8;   // BE Index 7 (AugSlot4)

    private boolean isAssembling = false;

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();

            // Only trigger assembly if we are NOT already assembling,
            // and only for the component/output slots.
            if (!isAssembling && (slot == 0 || slot == 1 || slot == 2 || slot == 3)) {
                assembleTool();
            }

            // Augment slots still behave like before
            if (slot >= AUGMENT_SLOT_START && slot < AUGMENT_SLOT_END) {
                installAugments();
            }
        }


        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= AUGMENT_SLOT_START && slot < AUGMENT_SLOT_END) {
                // Only items that implement the InstallableAugmentItem interface are valid
                return stack.getItem() instanceof InstallableAugmentItem;
            }
            if (switch (slot) {
                case 0 -> isToolHead(stack.getItem()); // Slot 0: Head (supports both old and new)
                case 1 -> isCore(stack.getItem()); // Slot 0: Head (supports both old and new)
                case 3 -> isToolRod(stack.getItem());  // Slot 3: Rod (supports both old and new)
                case 2 -> stack.getItem() instanceof ArsenalPickaxeItem; // ALLOW putting assembled tools back!
                case 4, 6, 5, 7 -> stack.getItem() instanceof Augment; // Only let augments in augments slots!
                default -> false; // Dont allow other items in unused slots for now
            }) return true;
            else return false;
        }
    };

    // Helper method to check if item is a tool head (old or new system)
    private boolean isToolHead(Item item) {
        // Old system: ToolHeadItem
        if (item instanceof ToolHeadItem) return true;

        // New system: ToolComponentItem with appropriate component type
        if (item instanceof ToolComponentItem toolComponent) {
            ToolComponentType type = toolComponent.getComponentType();
            return type == ToolComponentType.PICKAXE_HEAD;
                    /*type == ToolComponentType.AXE_HEAD ||
                    type == ToolComponentType.SHOVEL_HEAD ||
                    type == ToolComponentType.SWORD_BLADE ||
                    type == ToolComponentType.HOE_HEAD ||
                    type == ToolComponentType.HAMMER_HEAD ||
                    type == ToolComponentType.EXCAVATOR_HEAD ||
                    type == ToolComponentType.SAW_BLADE;

                     */
        }
        return false;
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
        // Old system: ToolRodItem
        if (item instanceof ToolRodItem) return true;

        // New system: ToolComponentItem with TOOL_ROD type
        if (item instanceof ToolComponentItem toolComponent) {
            return toolComponent.getComponentType() == ToolComponentType.TOOL_ROD;
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

    public void installAugments() {
        ItemStack outputTool = itemHandler.getStackInSlot(2);

        // Only proceed if we have a valid assembled tool
        if (outputTool.isEmpty() || !(outputTool.getItem() instanceof ArsenalPickaxeItem)) {
            return;
        }

        // Check each augment slot
        for (int i = AUGMENT_SLOT_START; i < AUGMENT_SLOT_END; i++) {
            ItemStack augmentStack = itemHandler.getStackInSlot(i);

            if (!augmentStack.isEmpty() && augmentStack.getItem() instanceof InstallableAugmentItem installable) {
                // Install the augment
                installable.installOnTool(outputTool);

                // Consume the augment item
                itemHandler.setStackInSlot(i, ItemStack.EMPTY);

                System.out.println("[Toolkit] Augment installed from slot " + i);
            }
        }

        setChanged();
    }

    public ToolkitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TOOLKIT.get(), pos, state);
    }

    // --- Tool Assembly Logic (MVP) ---
    private void assembleTool() {
        // Prevent recursive calls from onContentsChanged
        if (isAssembling) {
            return;
        }
        isAssembling = true;

        try {
            // Slots:
            // 0 = head
            // 1 = core
            // 2 = output (Arsenal Pickaxe)
            // 3 = rod
            ItemStack headStack   = itemHandler.getStackInSlot(0);
            ItemStack coreStack   = itemHandler.getStackInSlot(1);
            ItemStack outputStack = itemHandler.getStackInSlot(2);
            ItemStack rodStack    = itemHandler.getStackInSlot(3);

            boolean hasHeadInput = !headStack.isEmpty();
            boolean hasCoreInput = !coreStack.isEmpty();
            boolean hasRodInput  = !rodStack.isEmpty();

            // Nothing to do if no input and no tool
            if (!hasHeadInput && !hasCoreInput && !hasRodInput && outputStack.isEmpty()) {
                return;
            }

            // Output must be empty or an Arsenal Pickaxe
            if (!outputStack.isEmpty() && !(outputStack.getItem() instanceof ArsenalPickaxeItem)) {
                return;
            }

            boolean isNewTool = outputStack.isEmpty();

            // For a brand-new tool we require all 3 parts present
            if (isNewTool) {
                if (!hasHeadInput || !hasCoreInput || !hasRodInput) {
                    return;
                }
            }

            // Validate types (only for slots that have items)
            if (hasHeadInput && !isToolHead(headStack.getItem())) {
                return;
            }
            if (hasCoreInput && !isCore(coreStack.getItem())) {
                return;
            }
            if (hasRodInput && !isToolRod(rodStack.getItem())) {
                return;
            }

            // --- Read existing assembly data from the pickaxe (if any) ---

            String headMatName = null;
            String rodMatName  = null;
            String coreMatName = null;

            ToolAssemblyData existingData = null;
            if (!outputStack.isEmpty()) {
                existingData = outputStack.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
            }

            if (existingData != null) {
                headMatName = existingData.headMaterial();
                rodMatName  = existingData.rodMaterial();
                coreMatName = existingData.coreMaterial();
            }

            // --- Prepare variables for "swap out" items ---

            ItemStack swapHead = ItemStack.EMPTY;
            ItemStack swapCore = ItemStack.EMPTY;
            ItemStack swapRod  = ItemStack.EMPTY;

            // --- Handle HEAD replacement ---

            if (hasHeadInput) {
                com.arsenalcrafting.mod.core.alloy.AlloyMaterial newHeadMat =
                        getMaterialFromComponent(headStack.getItem());
                if (newHeadMat == null) {
                    return;
                }

                // If there was an old head, create the old head item to give back
                if (headMatName != null && !headMatName.isEmpty()) {
                    swapHead = findComponentStack(ToolComponentType.PICKAXE_HEAD, headMatName);
                }

                headMatName = newHeadMat.getName();
            } else if (isNewTool) {
                // For a brand new tool, all parts must come from inputs
                return;
            }

            // --- Handle CORE replacement ---

            if (hasCoreInput) {
                com.arsenalcrafting.mod.core.alloy.AlloyMaterial newCoreMat =
                        getMaterialFromComponent(coreStack.getItem());
                if (newCoreMat == null) {
                    return;
                }

                if (coreMatName != null && !coreMatName.isEmpty()) {
                    swapCore = findComponentStack(ToolComponentType.CORE, coreMatName);
                }

                coreMatName = newCoreMat.getName();
            } else if (isNewTool) {
                return;
            }

            // --- Handle ROD replacement ---

            if (hasRodInput) {
                com.arsenalcrafting.mod.core.alloy.AlloyMaterial newRodMat =
                        getMaterialFromComponent(rodStack.getItem());
                if (newRodMat == null) {
                    return;
                }

                if (rodMatName != null && !rodMatName.isEmpty()) {
                    swapRod = findComponentStack(ToolComponentType.TOOL_ROD, rodMatName);
                }

                rodMatName = newRodMat.getName();
            } else if (isNewTool) {
                return;
            }

            // At this point we must have material names for all 3 parts
            if (headMatName == null || rodMatName == null || coreMatName == null) {
                return;
            }

            // --- Build or copy the pickaxe ---

            ItemStack assembledTool;
            if (outputStack.isEmpty()) {
                assembledTool = new ItemStack(ModItems.ARSENAL_PICKAXE.get());
            } else {
                assembledTool = outputStack.copy();
            }

            ToolAssemblyData newData = new ToolAssemblyData(
                    headMatName,
                    rodMatName,
                    coreMatName
            );
            assembledTool.set(ModComponents.TOOL_ASSEMBLY_DATA.get(), newData);

            // --- Consume inputs & put swapped-out parts back ---

            // HEAD slot (0)
            if (hasHeadInput) {
                if (!swapHead.isEmpty()) {
                    // Give old head back
                    itemHandler.setStackInSlot(0, swapHead);
                } else {
                    // No old head -> consume input
                    itemHandler.setStackInSlot(0, ItemStack.EMPTY);
                }
            }

            // CORE slot (1)
            if (hasCoreInput) {
                if (!swapCore.isEmpty()) {
                    itemHandler.setStackInSlot(1, swapCore);
                } else {
                    itemHandler.setStackInSlot(1, ItemStack.EMPTY);
                }
            }

            // ROD slot (3)
            if (hasRodInput) {
                if (!swapRod.isEmpty()) {
                    itemHandler.setStackInSlot(3, swapRod);
                } else {
                    itemHandler.setStackInSlot(3, ItemStack.EMPTY);
                }
            }

            // Put the final tool in the output slot
            itemHandler.setStackInSlot(2, assembledTool);

            // Sync
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

    /**
     * Called by the game when the Block Entity's custom data is loaded from the world's CompoundTag.
     * This protected hook is the preferred method for loading custom NBT data like inventories.
     */
    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        // We use the CompoundTag's "Inventory" tag to deserialize our ItemStackHandler.
        itemHandler.deserializeNBT(provider, tag.getCompound("Inventory"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        // We serialize the ItemStackHandler and put it into the main tag under the key "Inventory".
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
}