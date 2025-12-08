package com.arsenalcrafting.mod.component;

public enum ToolComponentType {
    // Heads/Blades
    PICKAXE_HEAD("pickaxe_head", "Pickaxe Head"),
    //AXE_HEAD("axe_head", "Axe Head"),
    //SHOVEL_HEAD("shovel_head", "Shovel Head"),


    // Handles/Rods
    TOOL_ROD("tool_rod", "Tool Rod"),
    /*HILT("hilt", "Hilt"),
    POMMEL("pommel", "Pommel"),
   GRIP("grip", "Grip"),

     */

     //Attachments/Bindings
    CORE("core", "core");


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
}