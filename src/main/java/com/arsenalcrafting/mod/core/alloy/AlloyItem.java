package com.arsenalcrafting.mod.core.alloy;

import net.minecraft.world.item.Item;
//Generic Alloy
public class AlloyItem extends Item {
    private final AlloyMaterial material;

    public AlloyItem(AlloyMaterial material, Properties props) {
        super(props);
        this.material = material;
    }


    public AlloyMaterial getMaterial() {
        return material;
    }
}

