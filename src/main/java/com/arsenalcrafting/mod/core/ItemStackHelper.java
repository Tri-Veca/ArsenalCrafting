package com.arsenalcrafting.mod.core;

import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.augments.UniversalAugmentItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem;
import com.arsenalcrafting.mod.registry.ModFluids;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ItemStackHelper {

    // Get a specific augment item by ID and tier
    public static ItemStack getUniversalAugmentStack(String augmentId, int tier) {
        for (DeferredHolder<Item, UniversalAugmentItem> holder : ModItems.UNIVERSAL_AUGMENTS) {
            UniversalAugmentItem item = holder.get();
            if (item.getAugmentId().equals(augmentId) && item.getTier() == tier) {
                return new ItemStack(item);
            }
        }
        return ItemStack.EMPTY;
    }

    // Get the highest tier of an augment
    public static ItemStack getHighestTierAugment(String augmentId) {
        int maxTier = 0;
        UniversalAugmentItem maxTierItem = null;

        for (DeferredHolder<Item, UniversalAugmentItem> holder : ModItems.UNIVERSAL_AUGMENTS) {
            UniversalAugmentItem item = holder.get();
            if (item.getAugmentId().equals(augmentId) && item.getTier() > maxTier) {
                maxTier = item.getTier();
                maxTierItem = item;
            }
        }

        return maxTierItem != null ? new ItemStack(maxTierItem) : ItemStack.EMPTY;
    }

    // Get a specific augment item by ID and tier
    public static ItemStack getAlloyStack(AlloyMaterial material) {
        var holder = ModItems.ALLOY_ITEMS.get(material);
        if (holder != null) {
            return new ItemStack(holder.get());
        }
        return ItemStack.EMPTY;
    }
    public static ItemStack getRawStack(AlloyMaterial material) {
        var holder = ModItems.RAW_ITEMS.get(material);
        if (holder != null) {
            return new ItemStack(holder.get());
        }
        return ItemStack.EMPTY;
    }

    public static FluidStack getFluidFromBucket(AlloyMaterial material, int amount) {
        //System.out.println("[DEBUG] getFluidFromBucket called for: " + material.getName());
        Fluid fluid = ModFluids.getSource(material);
        if (fluid != null) {
            //System.out.println("[DEBUG] Found fluid: " + fluid.getFluidType().getDescription().getString());
            return new FluidStack(fluid, amount);
        }
        //System.out.println("[DEBUG] No fluid found for material: " + material.getName());
        return FluidStack.EMPTY;
    }


    public static ItemStack getComponent(AlloyMaterial material, ToolComponentType componentType) {
        var materialMap = ModItems.TOOL_COMPONENTS.get(material);
        if (materialMap != null) {
            var holder = materialMap.get(componentType);
            if (holder != null && holder.isBound()) {
                return new ItemStack(holder.get()); // Use holder.get() to get the Item
            }
        }
        return ItemStack.EMPTY;
    }

    // Get pickaxe head for material
    public static ItemStack getPickaxeHead(AlloyMaterial material) {
        return getComponent(material, ToolComponentType.PICKAXE_HEAD);
    }

    // Get tool rod for material
    public static ItemStack getToolRod(AlloyMaterial material) {
        return getComponent(material, ToolComponentType.TOOL_ROD);
    }
    // Get tool core for material
    public static ItemStack getCore(AlloyMaterial material) {
        return getComponent(material, ToolComponentType.CORE);
    }

    // Get material from a component item stack
    public static Optional<AlloyMaterial> getMaterialFromComponent(ItemStack stack) {
        if (stack.getItem() instanceof ToolComponentItem componentItem) {
            return Optional.of(componentItem.getMaterial());
        }
        return Optional.empty();
    }

    // Get component type from a component item stack
    public static Optional<ToolComponentType> getComponentType(ItemStack stack) {
        if (stack.getItem() instanceof ToolComponentItem componentItem) {
            return Optional.of(componentItem.getComponentType());
        }
        return Optional.empty();
    }

    // Check if an item stack is a specific type of component
    public static boolean isComponentOfType(ItemStack stack, ToolComponentType type) {
        if (stack.getItem() instanceof ToolComponentItem componentItem) {
            return componentItem.getComponentType() == type;
        }
        return false;
    }

    // Find all components for a specific material
    public static List<ItemStack> getAllComponentsForMaterial(AlloyMaterial material) {
        List<ItemStack> components = new ArrayList<>();
        var materialMap = ModItems.TOOL_COMPONENTS.get(material);

        if (materialMap != null) {
            for (var holder : materialMap.values()) {
                if (holder.isBound()) {
                    components.add(new ItemStack(holder.get()));
                }
            }
        }

        return components;
    }

    // Find all components of a specific type across all materials
    public static List<ItemStack> getAllComponentsOfType(ToolComponentType type) {
        List<ItemStack> components = new ArrayList<>();

        for (var entry : ModItems.TOOL_COMPONENTS.entrySet()) {
            var holder = entry.getValue().get(type);
            if (holder != null && holder.isBound()) {
                components.add(new ItemStack(holder.get()));
            }
        }

        return components;
    }

    public static ItemStack createHeadStackFromMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(getPickaxeHead(AlloyMaterial.fromName(materialName)).getItem());
    }

    public static ItemStack createCoreStackFromMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(getCore(AlloyMaterial.fromName(materialName)).getItem());
    }

    public static ItemStack createRodStackFromMaterialName(String materialName) {
        if (materialName == null || materialName.isEmpty()) return ItemStack.EMPTY;
        return new ItemStack(getToolRod(AlloyMaterial.fromName(materialName)).getItem());
    }

}
