package com.arsenalcrafting.mod.items.tool.toolparts;

import net.minecraft.world.item.Item;

// Base class for all modular tool heads (e.g., Pickaxe Head, Axe Head).
// This simple extension satisfies the registry requirements and the isItemValid check in the Block Entity.
public class ToolHeadItem extends Item {
    public ToolHeadItem(Properties properties) {
        super(properties);
    }
}