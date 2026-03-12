package com.arsenalcrafting.mod.component;
// We loop through this we Data Generating.

/**
 * Single Source Of Truth for Components
 */
public enum ToolComponentType {
    // Heads/Blades
    PICKAXE_HEAD("pickaxe_head", "Pickaxe Head"),
    AXE_HEAD("axe_head", "Axe Head"),
    SHOVEL_HEAD("shovel_head", "Shovel Head"),
    BROADSWORD_BLADE("broadsword_blade_head", "Broadsword Blade"),


    // Handles/Rods
    TOOL_ROD("tool_rod", "Tool Rod"),
    SHOVEL_TOOL_ROD("shovel_tool_rod", "Shovel Tool Rod"),
    // Weapon Blades
    KATANA_BLADE("katana_blade_head", "Katana Blade"),
    KATANA_HILT("katana_hilt_rod", "Katana Hilt"),
    BROADSWORD_HILT("broadsword_hilt_rod", "Broadsword Hilt"),
    DAGGER_HILT("dagger_hilt_rod", "Dagger Hilt"),
    DAGGER_BLADE("dagger_blade_head", "Dagger Blade"),
     //Attachments/Bindings
    CORE("core", "Core");


    private final String id;
    private final String displayName;

    ToolComponentType(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static ToolComponentType fromId(String id) {
        for (ToolComponentType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    public boolean isHead() {
        return this.id.contains("head");
    }

    public boolean isRod() {
        return this.id.contains("rod");
    }

}
