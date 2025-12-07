package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.client.menu.ToolkitMenu;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
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
    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            // Trigger assembly when ANY input slot changes (0=head, 3=rod)
            if (slot == 0 || slot == 3) {
                assembleTool();
            }
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
                case 0 -> stack.getItem() instanceof ToolHeadItem; // Slot 0: Head
                case 3 -> stack.getItem() instanceof ToolRodItem;  // Slot 3: Rod (FIXED: was checking slot 1)
                case 2 -> stack.getItem() instanceof ArsenalPickaxeItem; // ALLOW putting assembled tools back!
                case 4, 6, 5, 7 -> stack.getItem() instanceof Augment; // Only let augments in augments slots!
                default -> true; // Allow other items in unused slots for now
            }) return true;
            else return false;
        }
    };

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
        ItemStack head = itemHandler.getStackInSlot(0);
        ItemStack rod = itemHandler.getStackInSlot(3);
        ItemStack outputSlot = itemHandler.getStackInSlot(2);

        if (head.getItem() instanceof ToolHeadItem && rod.getItem() instanceof ToolRodItem) {
            if (outputSlot.isEmpty() || outputSlot.getItem() instanceof ArsenalPickaxeItem) {

                ItemStack assembledTool;
                boolean isNewTool = false;

                if (outputSlot.getItem() instanceof ArsenalPickaxeItem) {
                    assembledTool = outputSlot.copy();
                } else {
                    assembledTool = new ItemStack(ModItems.ARSENAL_PICKAXE.get());
                    isNewTool = true;
                }

                // Update tool assembly data
                ToolAssemblyData assemblyData = new ToolAssemblyData(
                        head.getDisplayName().getString(),
                        rod.getDisplayName().getString()
                );
                assembledTool.set(ModComponents.TOOL_ASSEMBLY_DATA.get(), assemblyData);

                // TEST MODE: Install enabled augments on new tools
                if (isNewTool && TestModeConfig.isTestModeEnabled()) {
                    installTestModeAugments(assembledTool);
                }

                itemHandler.setStackInSlot(0, ItemStack.EMPTY); //Use the head
                itemHandler.setStackInSlot(3, ItemStack.EMPTY); //Use the rod
                itemHandler.setStackInSlot(2, assembledTool);
            }
        } else if (!outputSlot.isEmpty() && outputSlot.getItem() instanceof ArsenalPickaxeItem) {
            // Clear output logic (unchanged)
            boolean headValid = head.getItem() instanceof ToolHeadItem;
            boolean rodValid = rod.getItem() instanceof ToolRodItem;

            if (!headValid && !rodValid) {
                itemHandler.setStackInSlot(2, ItemStack.EMPTY);
            }
        }

        setChanged();
    }

    private void installTestModeAugments(ItemStack tool) {
        System.out.println("[TestMode] Installing test augments on newly crafted tool");

        for (var def : AugmentManager.getAll()) {
            // Check if this augment is enabled in test mode
            if (TestModeConfig.isAugmentEnabled(def.id())) {
                var handler = AugmentManager.getHandler(def.id());
                if (handler != null && handler.supportsToolType(ArsenalToolType.PICKAXE)) {
                    // Install the augment with max tier if applicable
                    int tier = def.hasTiers() ? def.maxTier() : 1;
                    AugmentManager.install(def.id(), tool, tier);
                    System.out.println("[TestMode] Installed: " + def.name() +
                            (def.hasTiers() ? " Tier " + tier : ""));
                }
            }
        }
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