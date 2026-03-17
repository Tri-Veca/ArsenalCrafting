package com.arsenalcrafting.mod.events;

import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
import com.arsenalcrafting.mod.core.augments.ToolEquipTracker;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = "arsenalcrafting", bus = EventBusSubscriber.Bus.GAME)
public class ArsenalPlayerEvents {

    private static final Map<UUID, Integer> lastSelectedSlot = new HashMap<>();
    private static final Map<UUID, ItemStack> lastHeldTool = new HashMap<>();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return; // server only

        UUID uuid = player.getUUID();
        int currentSlot = player.getInventory().selected;
        ItemStack current = player.getInventory().getItem(currentSlot);
        ItemStack previous = lastHeldTool.get(uuid);

        boolean changed = previous == null || previous.getItem() != current.getItem();

        if (changed) {
            // Unequip old tool
            if (previous != null && !previous.isEmpty()) {
                AugmentProcessor.processToolUnequip(previous, player);
            }

            // Equip new tool
            if (current != null && !current.isEmpty()) {
                AugmentProcessor.processToolEquip(current, player);
            }

            // Update last-held snapshot
            lastHeldTool.put(uuid, current.copy());
            lastSelectedSlot.put(uuid, currentSlot);
        }
    }
}