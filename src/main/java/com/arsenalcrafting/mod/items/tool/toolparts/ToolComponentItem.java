package com.arsenalcrafting.mod.items.tool.toolparts;

import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.world.item.Item;

public class ToolComponentItem extends Item {
    private final AlloyMaterial material;
    private final ToolComponentType componentType;

    public ToolComponentItem(AlloyMaterial material, ToolComponentType componentType, Properties properties) {
        super(properties);
        this.material = material;
        this.componentType = componentType;
    }

    public AlloyMaterial getMaterial() {
        return material;
    }

    public ToolComponentType getComponentType() {
        return componentType;
    }
}