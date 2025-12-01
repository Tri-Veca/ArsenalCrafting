package com.arsenalcrafting.mod.items.tool.toolparts;

import net.minecraft.world.item.Item;

// Base class for all modular tool rods/handles.
// This simple extension satisfies the registry requirements and the isItemValid check in the Block Entity.
public class ToolRodItem extends Item {
    public ToolRodItem(Properties properties) {
        super(properties);
    }
}