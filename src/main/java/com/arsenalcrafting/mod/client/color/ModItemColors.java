package com.arsenalcrafting.mod.client.color;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyItem;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.ArsenalPickaxeItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolHeadItem;
import com.arsenalcrafting.mod.items.tool.toolparts.ToolRodItem;
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
        // 1) Arsenal Pickaxe (combo tint: head + rod)
        event.register(ModItemColors::colorArsenalPickaxe, ModItems.ARSENAL_PICKAXE.get());

        // 2) All tool part items (single-layer tint)
        event.register(
                ModItemColors::colorToolPart,
                // TODO: put ALL your head/rod items here:
                ModItems.BRONZE_PICKAXE_HEAD.get(),
                //ModItems.STEEL_PICKAXE_HEAD.get(),
                ModItems.TITANIUM_PICKAXE_HEAD.get(),
                ModItems.BRONZE_TOOL_ROD.get()
               // ModItems.STEEL_TOOL_ROD.get(),
                //ModItems.TITANIUM_TOOL_ROD.get()
                // ...add more as you create them
        );

        event.register(ModItemColors::colorAlloyItem,
                ModItems.BRONZE_ALLOY.get()
                //ModItems.STEEL_ALLOY.get(), //TODO
                //ModItems.TITANIUM_ALLOY.get()
        );
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

    // --------- TOOL PARTS (1-layer tint) ---------

    private static int colorToolPart(ItemStack stack, int tintIndex) {
        // only layer0 is tinted on part items
        if (tintIndex != 0) {
            return 0xFFFFFFFF;
        }

        Item item = stack.getItem();

        if (item instanceof ToolHeadItem head) {
            return ensureOpaque(head.getMaterial().getItemColor());
        }

        if (item instanceof ToolRodItem rod) {
            return ensureOpaque(rod.getMaterial().getItemColor());
        }

        return 0xFFFFFFFF;
    }


    // --------- ALLOYS (1-layer tint) ---------

    private static int colorAlloyItem(ItemStack stack, int tintIndex) {
        if (tintIndex != 0) return 0xFFFFFFFF;

        Item item = stack.getItem();

        // however you store the material for alloys
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
