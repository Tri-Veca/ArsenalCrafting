package com.arsenalcrafting.mod.client.color;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyItem;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem; // CHANGED
import com.arsenalcrafting.mod.registry.ModComponents;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(
        modid = ArsenalCrafting.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public class ModItemColors {

    @SubscribeEvent
    public static void registerItemColorHandlers(RegisterColorHandlersEvent.Item event) {
        // 1) Register all Arsenal Tools (Pickaxe, Shovel, etc.)
        // We register them specifically or by checking if they are instances of ArsenalTool
        event.register(ModItemColors::colorArsenalTool,
                ModItems.ARSENAL_PICKAXE.get(),
                ModItems.ARSENAL_SHOVEL.get()
                // ModItems.ARSENAL_AXE.get() // Add others here
        );

        // 2) All auto-generated tool components
        for (var materialMap : ModItems.TOOL_COMPONENTS.values()) {
            for (var holder : materialMap.values()) {
                Item item = holder.get();
                if (item != null) {
                    event.register(ModItemColors::colorToolComponent, item);
                }
            }
        }

        // 3) All auto-generated alloy items
        for (var holder : ModItems.ALLOY_ITEMS.values()) {
            Item item = holder.get();
            if (item != null) {
                event.register(ModItemColors::colorAlloyItem, item);
            }
        }
    }

    // --------- UNIVERSAL TOOL COLORING (3-layer tint) ---------

    private static int colorArsenalTool(ItemStack stack, int tintIndex) {
        // Use the base class/interface ArsenalTool instead of just Pickaxe
        if (!(stack.getItem() instanceof com.arsenalcrafting.mod.items.tool.ArsenalTool tool)) {
            return 0xFFFFFFFF;
        }

        ToolAssemblyData data = stack.get(ModComponents.TOOL_ASSEMBLY_DATA.get());

        AlloyMaterial headMat;
        AlloyMaterial rodMat;
        AlloyMaterial coreMat;

        // If data exists, use it. Otherwise, use the tool's base material
        if (data != null && !data.isEmpty()) {
            headMat = fromStoredName(data.headMaterial());
            rodMat  = fromStoredName(data.rodMaterial());
            coreMat = fromStoredName(data.coreMaterial());
        } else {
            // Ensure your ArsenalTool class has a getBaseMaterial() method
            headMat = tool.getBaseMaterial();
            rodMat  = tool.getBaseMaterial();
            coreMat = tool.getBaseMaterial();
        }

        // Map Tint Index to specific part
        // Layer 0: Head, Layer 1: Rod, Layer 2: Core
        AlloyMaterial target = switch (tintIndex) {
            case 0 -> headMat;
            case 1 -> rodMat;
            case 2 -> coreMat;
            default -> null;
        };

        return ensureOpaque(target != null ? target.getItemColor() : 0xFFFFFFFF);
    }

    // --------- TOOL COMPONENTS (1-layer tint) ---------

    private static int colorToolComponent(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return 0xFFFFFFFF;

        if (stack.getItem() instanceof ToolComponentItem toolComponent) {
            return ensureOpaque(toolComponent.getMaterial().getItemColor());
        }

        return 0xFFFFFFFF;
    }

    // --------- ALLOYS (1-layer tint) ---------

    private static int colorAlloyItem(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return 0xFFFFFFFF;

        if (stack.getItem() instanceof AlloyItem alloy) {
            return ensureOpaque(alloy.getMaterial().getItemColor());
        }

        return 0xFFFFFFFF;
    }

    // --------- Helpers ---------

    private static AlloyMaterial fromStoredName(String s) {
        if (s == null || s.isEmpty()) return null;
        for (AlloyMaterial mat : AlloyMaterial.values()) {
            if (mat.getName().equalsIgnoreCase(s)) {
                return mat;
            }
        }
        return null;
    }

    private static int ensureOpaque(int color) {
        return color | 0xFF000000;
    }
}