package com.arsenalcrafting.mod.power;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.CableBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Listens for cable placement/removal to trigger network rebuilds,
 * and drives the per-tick network energy distribution.
 */
@EventBusSubscriber(modid = ArsenalCrafting.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public class PowerNetworkEvents {

    /** Cable placed — rebuild network at that position. */
    @SubscribeEvent
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        if (event.getPlacedBlock().getBlock() instanceof CableBlock) {
            // Rebuild network for the cable itself
            PowerNetworkSavedData data = PowerNetworkSavedData.get(level);
            data.onCableChanged(level, event.getPos());
        } else {
            // Non-cable placed — check if any adjacent cable needs to re-scan endpoints
            // This handles placing a power bank next to an existing cable
            PowerNetworkSavedData data = PowerNetworkSavedData.get(level);
            BlockPos placed = event.getPos();
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = placed.relative(dir);
                if (level.isLoaded(neighbor)
                        && level.getBlockState(neighbor).getBlock() instanceof CableBlock) {
                    data.onCableChanged(level, neighbor);
                }
            }
        }
    }

    /** Cable broken — rebuild network at that position. */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        if (!(event.getState().getBlock() instanceof CableBlock)) return;

        PowerNetworkSavedData.get(level).onCableChanged(level, event.getPos());
    }

    /**
     * Server tick — distribute energy across all networks once per tick.
     * Using LevelTickEvent.Post so all block entities have already ticked.
     */
    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;
        // Only tick overworld-equivalent dimensions to avoid redundant ticks
        // (each dimension has its own SavedData instance anyway)
        PowerNetworkSavedData.get(level).tickNetworks(level);
    }
}