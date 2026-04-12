package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.ExtraMoldType;
import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.items.CastingPackageItem;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class PrefilledPackageRegistry {

        private static final Map<PackageType, Map<Boolean, ItemStack>> CACHE = new HashMap<>();

    static {
        for (PackageType type : PackageType.values()) {
            Map<Boolean, ItemStack> innerMap = new HashMap<>();
            innerMap.put(false, createPrefilledPackage(type, false));
            innerMap.put(true, createPrefilledPackage(type, true));
            CACHE.put(type, innerMap);
        }
    }

        /**
         * Creates a pre-filled package with all molds already inserted
         * @param type The package type to create
         * @param useBronze Whether to use bronze molds (true) or standard gold molds (false)
         * @return A new ItemStack with the package and all molds pre-inserted
         */
        private static ItemStack createPrefilledPackage(PackageType type, boolean useBronze) {
            var holder = ModItems.CASTING_PACKAGES.get(type);
            if (holder == null) return ItemStack.EMPTY;

            ItemStack stack = new ItemStack(holder.get());

            if (stack.getItem() instanceof CastingPackageItem packageItem) {
                // Use EMPTY registry access for static creation - molds don't need dynamic registry
                RegistryAccess registryAccess = RegistryAccess.EMPTY;
                ItemStackHandler inventory = packageItem.getInventory(stack, registryAccess);

                // Fill all tool component molds
                int slotIndex = 0;
                for (ToolComponentType moldType : type.getMolds()) {
                    ItemStack mold = getToolMoldItemStack(moldType, useBronze);
                    if (!mold.isEmpty()) {
                        inventory.insertItem(slotIndex, mold, false);
                    }
                    slotIndex++;
                }

                // Fill all extra molds (ingot mold, etc.)
                for (ExtraMoldType extraType : type.getExtraMolds()) {
                    ItemStack extraMold = new ItemStack(extraType.getMold(useBronze));
                    if (!extraMold.isEmpty()) {
                        inventory.insertItem(slotIndex, extraMold, false);
                    }
                    slotIndex++;
                }

                // Save the inventory to the stack
                packageItem.saveInventory(stack, inventory, registryAccess);
            }

            return stack;
        }

        /**
         * Gets the ItemStack for a tool component mold
         * @param moldType The type of tool component mold
         * @param useBronze Whether to use bronze mold (true) or standard gold mold (false)
         * @return The mold ItemStack
         */
        private static ItemStack getToolMoldItemStack(ToolComponentType moldType, boolean useBronze) {
            if (useBronze) {
                var bronzeMold = ModBlocks.BRONZE_MOLD_BLOCKS.get(moldType);
                if (bronzeMold != null) {
                    return new ItemStack(bronzeMold.get().asItem());
                }
            } else {
                var standardMold = ModBlocks.MOLD_BLOCKS.get(moldType);
                if (standardMold != null) {
                    return new ItemStack(standardMold.get().asItem());
                }
            }
            return ItemStack.EMPTY;
        }

        /**
         * Gets a copy of a pre-filled package
         * @param type The package type to get
         * @param useBronze Whether to get the bronze version (true) or standard gold version (false)
         * @return A copy of the pre-filled package ItemStack
         */
        public static ItemStack getPrefilledPackage(PackageType type, boolean useBronze) {
            Map<Boolean, ItemStack> innerMap = CACHE.get(type);
            if (innerMap == null) return ItemStack.EMPTY;

            ItemStack cached = innerMap.get(useBronze);
            if (cached == null || cached.isEmpty()) {
                return ItemStack.EMPTY;
            }
            return cached.copy(); // Return a copy so each player gets their own instance
        }

        /**
         * Gets the standard (gold) version of a pre-filled package
         * @param type The package type to get
         * @return A copy of the pre-filled package with standard molds
         */
        public static ItemStack getStandardPackage(PackageType type) {
            return getPrefilledPackage(type, false);
        }

        /**
         * Gets the bronze version of a pre-filled package
         * @param type The package type to get
         * @return A copy of the pre-filled package with bronze molds
         */
        public static ItemStack getBronzePackage(PackageType type) {
            return getPrefilledPackage(type, true);
        }

        /**
         * For debugging - prints all cached packages
         */
        public static void debugPrintCache() {
            System.out.println("=== PrefilledPackageRegistry Contents ===");
            for (PackageType type : PackageType.values()) {
                System.out.println("Package: " + type.getDisplayName());
                Map<Boolean, ItemStack> innerMap = CACHE.get(type);
                if (innerMap != null) {
                    ItemStack standard = innerMap.get(false);
                    ItemStack bronze = innerMap.get(true);
                    System.out.println("  Standard: " + (standard.isEmpty() ? "EMPTY" : standard.getCount() + "x " + standard.getHoverName().getString()));
                    System.out.println("  Bronze: " + (bronze.isEmpty() ? "EMPTY" : bronze.getCount() + "x " + bronze.getHoverName().getString()));
                }
            }
        }
    }