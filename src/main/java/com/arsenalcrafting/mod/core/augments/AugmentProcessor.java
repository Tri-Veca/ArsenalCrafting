package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;

import javax.annotation.Nullable;
import java.util.*;

public class AugmentProcessor {

    // Cache handlers to avoid repeated instantiation
    private static final Map<String, AugmentHandler> HANDLER_CACHE = new HashMap<>();

    // Get cached handler instance
    private static AugmentHandler getCachedHandler(String augmentId) {
        return HANDLER_CACHE.computeIfAbsent(augmentId, id -> {
            try {
                AugmentManager.AugmentDefinition def = AugmentManager.get(id);
                if (def != null) {
                    return def.handlerClass().newInstance();
                }
            } catch (Exception e) {
                System.err.println("Failed to instantiate handler for augment: " + augmentId);
                e.printStackTrace();
            }
            return null;
        });
    }

    public static ProcessResult processBlockBreak(ItemStack tool, ArsenalToolType toolType,
                                                  Level level, BlockState state,
                                                  BlockPos pos, LivingEntity miner) {

        List<AugmentHandler> handlers = getActiveHandlers(tool, toolType);
        handlers.sort(Comparator.comparingInt(AugmentHandler::priority));

        ProcessResult result = new ProcessResult();

        for (AugmentHandler handler : handlers) {
            if (handler.canHandleBlock(state, tool)) {
                boolean handled = handler.handleBlockBreak(tool, level, state, pos, miner);
                if (handled) {
                    result.handled = true;
                    result.lastHandler = handler.getClass().getSimpleName();
                    if (handler.isExclusive()) {
                        result.exclusiveTriggered = true;
                        break;
                    }
                }
            }
        }

        // Post-process for applicable handlers
        for (AugmentHandler handler : handlers) {
            if (!handler.isExclusive() ||
                    handler.getClass().getSimpleName().equals(result.lastHandler)) {
                handler.postProcess(tool, level, pos);
            }
        }

        // Detect special combos
        detectCombos(handlers, result);

        return result;
    }

    private static List<AugmentHandler> getActiveHandlers(ItemStack tool, ArsenalToolType toolType) {
        List<AugmentHandler> handlers = new ArrayList<>();

        // Check all augments dynamically
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            if (AugmentManager.isInstalled(def.id(), tool)) {
                AugmentHandler handler = getCachedHandler(def.id());
                if (handler != null && handler.supportsToolType(toolType)) {
                    handlers.add(handler);
                }
            }
        }

        return handlers;
    }

    /**
     * Process entity attacks with all applicable augments
     */
    public static ProcessResult processEntityAttack(ItemStack tool, ArsenalToolType toolType,
                                                    LivingEntity attacker, Entity target,
                                                    float damage) {

        List<AugmentHandler> handlers = getActiveHandlers(tool, toolType);
        handlers.sort(Comparator.comparingInt(AugmentHandler::priority));

        ProcessResult result = new ProcessResult();

        for (AugmentHandler handler : handlers) {
            if (handler.canHandleEntity(target, tool)) {
                boolean handled = handler.handleEntityAttack(tool, attacker, target, damage);
                if (handled) {
                    result.handled = true;
                    result.lastHandler = handler.getClass().getSimpleName();
                    // Combat augments usually aren't exclusive, but respect the flag
                    if (handler.isExclusive()) {
                        result.exclusiveTriggered = true;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Update tool each tick (for magnet, effects, etc.)
     */
    public static void processToolUpdate(ItemStack tool, Player player, ArsenalToolType toolType) {
        // 1. Get all active augments for this tool
        List<AugmentManager.AugmentDefinition> activeAugments = getActiveAugments(tool, toolType);

        // 2. Loop through them and trigger their per-tick update logic
        for (AugmentManager.AugmentDefinition def : activeAugments) {
            AugmentHandler handler = getCachedHandler(def.id());
            if (handler != null) {
                handler.onToolUpdate(tool, player);
            }
        }
    }

    /**
     * Called when tool is equipped
     */
    public static void processToolEquip(ItemStack tool, ArsenalToolType toolType, Player player) {
        List<AugmentHandler> handlers = getActiveHandlers(tool, toolType);

        for (AugmentHandler handler : handlers) {
            handler.onToolEquip(tool, player);
        }
    }

    /**
     * Called when tool is unequipped
     */
    public static void processToolUnequip(ItemStack tool, ArsenalToolType toolType, Player player) {
        List<AugmentHandler> handlers = getActiveHandlers(tool, toolType);

        for (AugmentHandler handler : handlers) {
            handler.onToolUnequip(tool, player);
        }
    }

    // ============= HELPER METHODS =============

    /**
     * Detect special combo effects
     */
    private static void detectCombos(List<AugmentHandler> handlers, ProcessResult result) {
        // Check combo by class name (since we don't have AugmentType enum anymore)
        boolean hasAutoSmelt = handlers.stream()
                .anyMatch(h -> h.getClass().getSimpleName().contains("AutoSmelt"));
        boolean hasOreMiner = handlers.stream()
                .anyMatch(h -> h.getClass().getSimpleName().contains("OreMiner"));

        if (hasAutoSmelt && hasOreMiner) {
            result.specialCombo = "SMELT_VEIN";
            result.comboDescription = "Auto-Smelt + Ore-Miner: Smelts entire ore veins";
        }

        // Add more combos as needed
        boolean hasFireAspect = handlers.stream()
                .anyMatch(h -> h.getClass().getSimpleName().contains("FireAspect"));
        boolean hasLifeSteal = handlers.stream()
                .anyMatch(h -> h.getClass().getSimpleName().contains("LifeSteal"));

        if (hasFireAspect && hasLifeSteal) {
            result.specialCombo = "VAMPIRIC_FLAME";
            result.comboDescription = "Fire Aspect + Life Steal: Burning enemies heal you";
        }
    }

    /**
     * Check if a specific augment is active on a tool
     */
    public static boolean hasAugment(ItemStack tool, String augmentId) {
        return AugmentManager.isInstalled(augmentId, tool);
    }

    public static boolean hasAugment(ItemStack tool, Class<? extends AugmentHandler> handlerClass) {
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            if (AugmentManager.isInstalled(def.id(), tool)) {
                AugmentHandler handler = getCachedHandler(def.id());
                if (handler != null && handlerClass.isInstance(handler)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get all active augment names for a tool
     */
    public static List<String> getActiveAugmentNames(ItemStack tool, @Nullable ArsenalToolType toolType) {
        List<String> names = new ArrayList<>();

        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            if (AugmentManager.isInstalled(def.id(), tool)) {
                AugmentHandler handler = getCachedHandler(def.id());
                if (handler != null && (toolType == null || handler.supportsToolType(toolType))) {
                    names.add(def.name());
                }
            }
        }

        return names;
    }

    /**
     * Get active augment IDs for a tool (for UI/debug)
     */
    public static List<String> getActiveAugmentIds(ItemStack tool, @Nullable ArsenalToolType toolType) {
        List<String> ids = new ArrayList<>();

        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            if (AugmentManager.isInstalled(def.id(), tool)) {
                AugmentHandler handler = getCachedHandler(def.id());
                if (handler != null && (toolType == null || handler.supportsToolType(toolType))) {
                    ids.add(def.id());
                }
            }
        }

        return ids;
    }

    /**
     * Get augment definitions for active augments on a tool
     */
    public static List<AugmentManager.AugmentDefinition> getActiveAugments(ItemStack tool, ArsenalToolType toolType) {
        List<AugmentManager.AugmentDefinition> result = new ArrayList<>();

        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            if (AugmentManager.isInstalled(def.id(), tool)) {
                AugmentHandler handler = getCachedHandler(def.id());
                if (handler != null && handler.supportsToolType(toolType)) {
                    result.add(def);
                }
            }
        }

        return result;
    }

    // Debug helper: print everything to log
    public static void debugDumpAugments(ItemStack tool, ArsenalToolType toolType) {
        List<AugmentManager.AugmentDefinition> active = getActiveAugments(tool, toolType);
        System.out.println("[Augments] Active augments (" + active.size() + "):");
        for (AugmentManager.AugmentDefinition def : active) {
            System.out.println(" - " + def.id() + " (" + def.name() + ")");
        }
    }

    public static void applyAllAugmentsForTesting(ItemStack tool, ArsenalToolType toolType) {
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            AugmentHandler handler = getCachedHandler(def.id());
            if (handler != null && handler.supportsToolType(toolType)) {
                AugmentManager.install(def.id(), tool, def.hasTiers() ? def.maxTier() : 1);
            }
        }
    }

    public static void removeAllAugments(ItemStack tool) {
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            AugmentManager.uninstall(def.id(), tool);
        }
    }

    // ============= PUBLIC RESULT CLASS =============

    public static class ProcessResult {
        public boolean handled = false;
        public boolean exclusiveTriggered = false;
        public String lastHandler = null;
        public String specialCombo = null;
        public String comboDescription = null;

        public boolean hasCombo(String comboName) {
            return comboName.equals(specialCombo);
        }
    }
}