package com.arsenalcrafting.mod.items;

import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.ArrayList;
import java.util.List;

public class CastingPackageItem extends Item {

    private final PackageType packageType;

    public CastingPackageItem(PackageType packageType, Properties properties) {
        super(properties.stacksTo(1));
        this.packageType = packageType;
    }

    public PackageType getPackageType() {
        return packageType;
    }

    // ========== INVENTORY HANDLER ==========

    public ItemStackHandler getInventory(ItemStack stack, RegistryAccess registryAccess) {
        int maxSlots = packageType.getMolds().size() + (packageType.includesIngotMold() ? 1 : 0);
        ItemStackHandler handler = new ItemStackHandler(maxSlots) {
            @Override
            protected void onContentsChanged(int slot) {
                saveInventory(stack, this, registryAccess);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isValidMold(stack);
            }
        };

        // Load existing data
        var customData = stack.get(DataComponents.CUSTOM_DATA);
        if (customData != null) {
            CompoundTag nbt = customData.copyTag();
            if (nbt.contains("Inventory")) {
                CompoundTag invTag = nbt.getCompound("Inventory");
                handler.deserializeNBT(registryAccess, invTag);
            }
        }

        return handler;
    }

    public void saveInventory(ItemStack stack, ItemStackHandler handler, RegistryAccess registryAccess) {
        CompoundTag nbt = new CompoundTag();
        nbt.put("Inventory", handler.serializeNBT(registryAccess));
        stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
    }

    public boolean isValidMold(ItemStack stack) {
        // Check ingot molds first
        if (packageType.includesIngotMold()) {
            if (stack.getItem() == ModBlocks.INGOT_MOLD.get().asItem()) return true;
            if (stack.getItem() == ModBlocks.BRONZE_INGOT_MOLD.get().asItem()) return true;
        }

        for (ToolComponentType type : packageType.getMolds()) {
            var standard = ModBlocks.MOLD_BLOCKS.get(type);
            var bronze = ModBlocks.BRONZE_MOLD_BLOCKS.get(type);

            if (standard != null && stack.getItem() == standard.get().asItem()) return true;
            if (bronze != null && stack.getItem() == bronze.get().asItem()) return true;
        }
        return false;
    }

    // ========== VORTEX METHODS ==========

    private void pullMoldsFromInventory(ItemStack stack, Player player, RegistryAccess registryAccess) {
        ItemStackHandler inventory = getInventory(stack, registryAccess);
        int required = packageType.getMolds().size() + (packageType.includesIngotMold() ? 1 : 0);

        // First, find empty slots in the package
        List<Integer> emptySlots = new ArrayList<>();
        for (int i = 0; i < required; i++) {
            if (inventory.getStackInSlot(i).isEmpty()) {
                emptySlots.add(i);
            }
        }

        if (emptySlots.isEmpty()) {
            player.displayClientMessage(Component.literal("Package is full!"), true);
            return;
        }

        // Scan player inventory for molds
        int pulled = 0;
        for (int slotIndex : emptySlots) {
            if (pulled >= emptySlots.size()) break;

            // Handle ingot mold slot separately
            if (packageType.includesIngotMold() && slotIndex == packageType.getMolds().size()) {
                for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                    ItemStack invStack = player.getInventory().getItem(i);
                    if (invStack.isEmpty()) continue;
                    if (invStack.getItem() == ModBlocks.INGOT_MOLD.get().asItem() ||
                            invStack.getItem() == ModBlocks.BRONZE_INGOT_MOLD.get().asItem()) {
                        ItemStack moldToAdd = invStack.copy();
                        moldToAdd.setCount(1);
                        invStack.shrink(1);
                        inventory.insertItem(slotIndex, moldToAdd, false);
                        pulled++;
                        break;
                    }
                }
                continue;
            }

            ToolComponentType expectedType = packageType.getMolds().get(slotIndex);
            var standard = ModBlocks.MOLD_BLOCKS.get(expectedType);
            var bronze = ModBlocks.BRONZE_MOLD_BLOCKS.get(expectedType);

            for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
                ItemStack invStack = player.getInventory().getItem(i);
                if (invStack.isEmpty()) continue;

                ItemStack moldToAdd = ItemStack.EMPTY;
                if (standard != null && invStack.getItem() == standard.get().asItem()) {
                    moldToAdd = invStack.copy();
                    moldToAdd.setCount(1);
                    invStack.shrink(1);
                } else if (bronze != null && invStack.getItem() == bronze.get().asItem()) {
                    moldToAdd = invStack.copy();
                    moldToAdd.setCount(1);
                    invStack.shrink(1);
                }

                if (!moldToAdd.isEmpty()) {
                    inventory.insertItem(slotIndex, moldToAdd, false);
                    pulled++;
                    break;
                }
            }
        }

        saveInventory(stack, inventory, registryAccess);

        if (pulled > 0) {
            player.displayClientMessage(Component.literal("Pulled " + pulled + " molds into package"), true);
        } else {
            player.displayClientMessage(Component.literal("No matching molds found in inventory"), true);
        }
    }

    private void pushMoldsToInventory(ItemStack stack, Player player, RegistryAccess registryAccess) {
        ItemStackHandler inventory = getInventory(stack, registryAccess);
        int required = packageType.getMolds().size() + (packageType.includesIngotMold() ? 1 : 0);

        int pushed = 0;
        for (int i = 0; i < required; i++) {
            ItemStack mold = inventory.getStackInSlot(i);
            if (!mold.isEmpty()) {
                ItemStack extracted = inventory.extractItem(i, 1, false);
                if (!extracted.isEmpty()) {
                    if (!player.getInventory().add(extracted)) {
                        player.drop(extracted, false);
                    }
                    pushed++;
                }
            }
        }

        saveInventory(stack, inventory, registryAccess);

        if (pushed > 0) {
            player.displayClientMessage(Component.literal("Pushed " + pushed + " molds to inventory"), true);
        } else {
            player.displayClientMessage(Component.literal("No molds in package to push"), true);
        }
    }

    // ========== CORE METHODS ==========

    public boolean isComplete(ItemStack stack, RegistryAccess registryAccess) {
        ItemStackHandler inv = getInventory(stack, registryAccess);
        int required = packageType.getMolds().size() + (packageType.includesIngotMold() ? 1 : 0);
        int filled = 0;

        for (int i = 0; i < required && filled < required; i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                filled++;
            }
        }

        return filled >= required;
    }

    public int getFilledCount(ItemStack stack, RegistryAccess registryAccess) {
        ItemStackHandler inv = getInventory(stack, registryAccess);
        int required = packageType.getMolds().size() + (packageType.includesIngotMold() ? 1 : 0);
        int filled = 0;

        for (int i = 0; i < required; i++) {
            if (!inv.getStackInSlot(i).isEmpty()) {
                filled++;
            }
        }

        return filled;
    }

    // ========== INTERACTION ==========

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        RegistryAccess registryAccess = level.registryAccess();

        // VORTEX FUNCTIONALITY - Shift+click
        if (player.isShiftKeyDown()) {
            // Check if package has any molds
            boolean hasMolds = getFilledCount(stack, registryAccess) > 0;

            if (hasMolds) {
                // Push molds from package to player inventory
                pushMoldsToInventory(stack, player, registryAccess);
            } else {
                // Pull molds from player inventory to package
                pullMoldsFromInventory(stack, player, registryAccess);
            }
            return InteractionResultHolder.success(stack);
        }

        // Normal right-click - open GUI
        player.openMenu(new SimpleMenuProvider(
                (id, inv, p) -> new com.arsenalcrafting.mod.client.menu.CastingPackageMenu(id, inv, stack),
                Component.literal(packageType.getDisplayName())
        ));

        return InteractionResultHolder.success(stack);
    }

    // ========== TOOLTIP ==========

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        RegistryAccess registryAccess = (RegistryAccess) context.registries();
        int filled = getFilledCount(stack, registryAccess);
        int required = packageType.getMolds().size() + (packageType.includesIngotMold() ? 1 : 0);

        tooltipComponents.add(Component.literal(filled + "/" + required + " Molds")
                .withStyle(net.minecraft.ChatFormatting.GRAY));
        tooltipComponents.add(Component.literal("Shift+Right-click to transfer molds")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.literal("Right-click to open")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }
}