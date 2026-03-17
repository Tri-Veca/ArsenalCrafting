package com.arsenalcrafting.mod.core.augments;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ToolEquipTracker {
    private static final Map<UUID, ItemStack> LAST_TOOL = new HashMap<>();

    public static void tick(Player player) {
        ItemStack current = player.getMainHandItem();

        // Skip if nothing in hand
        if (current == null || current.isEmpty()) return;

        ItemStack last = LAST_TOOL.get(player.getUUID());

        // Treat null as empty
        if (last == null) last = ItemStack.EMPTY;

        // Only compare if last is not null
        if (!ItemStack.matches(current, last)) {
            // Tool switched
            if (!last.isEmpty()) {
                // Handle unequip logic here
            }

            LAST_TOOL.put(player.getUUID(), current.copy());
        }
    }
}