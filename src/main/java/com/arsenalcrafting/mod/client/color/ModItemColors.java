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
        System.out.println("[ModItemColors] Starting color registration...");

        // 1) Arsenal Pickaxe (combo tint: head + rod)
        event.register(ModItemColors::colorArsenalPickaxe, ModItems.ARSENAL_PICKAXE.get());
        System.out.println("[ModItemColors] Registered: Arsenal Pickaxe");

        // 2) All auto-generated tool components
        int toolComponentCount = 0;
        for (var holder : ModItems.TOOL_COMPONENTS.values()) {
            Item item = holder.get();
            if (item != null) {
                event.register(ModItemColors::colorToolComponent, item);
                toolComponentCount++;
                System.out.println("[ModItemColors] Registered tool component: " + holder.getId());
            }
        }
        System.out.println("[ModItemColors] Registered " + toolComponentCount + " tool components");

        // 3) All auto-generated alloy items
        int alloyCount = 0;
        for (var holder : ModItems.ALLOY_ITEMS.values()) {
            Item item = holder.get();
            if (item != null) {
                event.register(ModItemColors::colorAlloyItem, item);
                alloyCount++;
                System.out.println("[ModItemColors] Registered alloy: " + holder.getId());
            }
        }
        System.out.println("[ModItemColors] Registered " + alloyCount + " alloy items");
    }

    // --------- PICKAXE (2-layer tint: head + rod) ---------

    private static int colorArsenalPickaxe(ItemStack stack, int tintIndex) {
        Item item = stack.getItem();
        if (!(item instanceof ArsenalPickaxeItem pickaxe)) {
            return 0xFFFFFFFF;
        }

        ToolAssemblyData data = stack.getOrDefault(
                ModComponents.TOOL_ASSEMBLY_DATA.get(),
                ToolAssemblyData.EMPTY
        );

        AlloyMaterial headMat;
        AlloyMaterial rodMat;

        if (!data.isEmpty()) {
            headMat = fromStoredName(data.headMaterial());
            rodMat  = fromStoredName(data.rodMaterial());
        } else {
            headMat = pickaxe.getBaseMaterial();
            rodMat  = pickaxe.getBaseMaterial();
        }

        AlloyMaterial target = switch (tintIndex) {
            case 0 -> headMat; // layer0 = head
            case 1 -> rodMat;  // layer1 = rod
            default -> null;
        };

        return ensureOpaque(target != null ? target.getItemColor() : 0xFFFFFFFF);
    }

    // --------- TOOL COMPONENTS (1-layer tint) ---------
    // UPDATED: Now handles ToolComponentItem instead of ToolHeadItem/ToolRodItem

    private static int colorToolComponent(ItemStack stack, int tintIndex) {
        // only layer0 is tinted on component items
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }

        Item item = stack.getItem();

        // CHANGED: Check for ToolComponentItem
        if (item instanceof ToolComponentItem toolComponent) {
            AlloyMaterial material = toolComponent.getMaterial();
            return ensureOpaque(material.getItemColor());
        }

        return 0xFFFFFFFF;
    }

    // --------- ALLOYS (1-layer tint) ---------

    private static int colorAlloyItem(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return 0xFFFFFFFF;

        Item item = stack.getItem();

        if (item instanceof AlloyItem alloy) {
            return ensureOpaque(alloy.getMaterial().getItemColor());
        }

        return 0xFFFFFFFF;
    }

    // --------- helpers ---------

    private static AlloyMaterial fromStoredName(String s) {
        if (s == null || s.isEmpty()) return null;
        for (AlloyMaterial mat : AlloyMaterial.values()) {
            if (mat.getName().equalsIgnoreCase(s) || mat.name().equalsIgnoreCase(s)) {
                return mat;
            }
        }
        return null;
    }

    private static int ensureOpaque(int color) {
        // If alpha is 0, force FF
        if ((color & 0xFF000000) == 0) {
            color |= 0xFF000000;
        }
        return color;
    }
}