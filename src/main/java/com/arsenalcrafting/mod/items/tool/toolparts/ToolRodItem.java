package com.arsenalcrafting.mod.items.tool.toolparts;

import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.world.item.Item;

public class ToolRodItem extends Item {

    private final AlloyMaterial material;

    public ToolRodItem(AlloyMaterial material, Properties properties) {
        super(properties);
        this.material = material;
    }

    public AlloyMaterial getMaterial() {
        return material;
    }
}
