package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.world.item.ItemStack;

/**
 * Burn Protection core power - THE single source of truth for all burn protection logic.
 */
public final class BurnProtection {

    private BurnProtection() {}

    public static boolean hasBurnProtection(ItemStack stack) {
        if (!(stack.getItem() instanceof ArsenalTool)) {
            return false;
        }

        ToolAssemblyData data = stack.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (data == null || data.isEmpty()) {
            return false;
        }

        AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
        if (coreMat == null) {
            return false;
        }

        CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
        return def != null && "burn_protection".equals(def.id());

    }
}
