package com.arsenalcrafting.mod.items.tool.toolparts;

import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.world.item.Item;

public class ToolHeadItem extends Item {

    private final AlloyMaterial material;

    public ToolHeadItem(AlloyMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public AlloyMaterial getMaterial() {
        return material;
    }
}
