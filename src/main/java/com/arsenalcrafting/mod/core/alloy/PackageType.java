package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.component.ToolComponentType;

import java.util.List;

/**
 * Single source of truth for all casting packages.
 * Defines contents, costs, and display names.
 */
public enum PackageType {

    PICKAXE("Pickaxe Package", "pickaxe_package", 6,
            List.of(ToolComponentType.PICKAXE_HEAD, ToolComponentType.TOOL_ROD, ToolComponentType.CORE)),

    AXE("Axe Package", "axe_package", 6,
            List.of(ToolComponentType.AXE_HEAD, ToolComponentType.TOOL_ROD, ToolComponentType.CORE)),

    SHOVEL("Shovel Package", "shovel_package", 4,
            List.of(ToolComponentType.SHOVEL_HEAD, ToolComponentType.SHOVEL_TOOL_ROD, ToolComponentType.CORE)),

    BROADSWORD("Broadsword Package", "broadsword_package", 6,
            List.of(ToolComponentType.BROADSWORD_BLADE, ToolComponentType.BROADSWORD_HILT, ToolComponentType.CORE)),

    KATANA("Katana Package", "katana_package", 6,
            List.of(ToolComponentType.KATANA_BLADE, ToolComponentType.KATANA_HILT, ToolComponentType.CORE)),

    DAGGER("Dagger Package", "dagger_package", 3,
            List.of(ToolComponentType.DAGGER_BLADE, ToolComponentType.DAGGER_HILT, ToolComponentType.CORE)),

    EXTRAS("Extras Package", "extras_package", 1,
            List.of(ToolComponentType.CORE)),


    SUPER("Super Package", "super_package", 32,
          List.of(
                  ToolComponentType.PICKAXE_HEAD,
          ToolComponentType.TOOL_ROD,
          ToolComponentType.AXE_HEAD,
          ToolComponentType.SHOVEL_HEAD,
          ToolComponentType.SHOVEL_TOOL_ROD,
          ToolComponentType.BROADSWORD_BLADE,
          ToolComponentType.BROADSWORD_HILT,
          ToolComponentType.KATANA_BLADE,
          ToolComponentType.KATANA_HILT,
          ToolComponentType.DAGGER_BLADE,
          ToolComponentType.DAGGER_HILT,
          ToolComponentType.CORE
          ));

    // ingot mold added when created

    private final String displayName;
    private final String id;
    private final int ingotCost;
    private final List<ToolComponentType> molds;

    PackageType(String displayName, String id, int ingotCost, List<ToolComponentType> molds) {
        this.displayName = displayName;
        this.id          = id;
        this.ingotCost   = ingotCost;
        this.molds       = molds;
    }

    public String getDisplayName() { return displayName; }
    public String getId()          { return id; }
    public int getIngotCost()      { return ingotCost; }
    public List<ToolComponentType> getMolds() { return molds; }

    public static PackageType fromId(String id) {
        for (PackageType type : values()) {
            if (type.id.equals(id)) return type;
        }
        return null;
    }
}

