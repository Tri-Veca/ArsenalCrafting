package com.arsenalcrafting.mod.items.tool;

public enum ArsenalToolType {

    // Mining tools
    PICKAXE,
    AXE,
    SHOVEL,
    HOE,

    // Combat weapons
    CHAKRAM, //probably not gonna use
    KATANA,
    DAGGER,
    BROADSWORD;



    public boolean isMiningTool() {
        return this == PICKAXE || this == AXE || this == SHOVEL || this == HOE;
    }

    public boolean isWeapon() {
        return this == CHAKRAM || this == KATANA || this == DAGGER || this == BROADSWORD;
    }
}
