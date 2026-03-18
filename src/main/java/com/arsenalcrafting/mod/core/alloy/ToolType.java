package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.world.item.Item;

public enum ToolType {
    PICKAXE("pickaxe", ToolComponentType.PICKAXE_HEAD, ToolComponentType.TOOL_ROD),
    AXE("axe", ToolComponentType.AXE_HEAD, ToolComponentType.TOOL_ROD),
    SHOVEL("shovel", ToolComponentType.SHOVEL_HEAD, ToolComponentType.SHOVEL_TOOL_ROD),
    KATANA("katana", ToolComponentType.KATANA_BLADE, ToolComponentType.KATANA_HILT),
    BROADSWORD("broadsword", ToolComponentType.BROADSWORD_BLADE, ToolComponentType.BROADSWORD_HILT),
    DAGGER("dagger", ToolComponentType.DAGGER_BLADE, ToolComponentType.DAGGER_HILT);

    private final String name;
    private final ToolComponentType headType;
    private final ToolComponentType rodType;

    ToolType(String name, ToolComponentType headType, ToolComponentType rodType) {
        this.name = name;
        this.headType = headType;
        this.rodType = rodType;
    }

    public String getName() { return name; }
    public ToolComponentType getHeadType() { return headType; }
    public ToolComponentType getRodType() { return rodType; }

    public Item getToolItem() {
        return switch (this) {
            case PICKAXE -> ModItems.ARSENAL_PICKAXE.get();
            case AXE -> ModItems.ARSENAL_AXE.get();
            case SHOVEL -> ModItems.ARSENAL_SHOVEL.get();
            case KATANA -> ModItems.ARSENAL_KATANA.get();
            case BROADSWORD -> ModItems.ARSENAL_BROADSWORD.get();
            case DAGGER -> ModItems.ARSENAL_DAGGER.get();
        };
    }

    // In your ToolType enum, add this method:
    public ArsenalToolType getToolType() {
        return switch (this) {
            case PICKAXE -> ArsenalToolType.PICKAXE;
            case AXE -> ArsenalToolType.AXE;
            case SHOVEL -> ArsenalToolType.SHOVEL;
            case KATANA -> ArsenalToolType.KATANA;
            case BROADSWORD -> ArsenalToolType.BROADSWORD;
            case DAGGER -> ArsenalToolType.DAGGER;
        };
    }
}