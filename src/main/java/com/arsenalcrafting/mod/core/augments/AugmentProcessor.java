package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.items.augments.EfficiencyAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Central augment processor.
 * Manages augment execution order, exclusivity, and interactions.
 * This is the ONLY class that should orchestrate augment behavior.
 */
public class AugmentProcessor {

    private static final Map<String, AugmentHandler> HANDLER_CACHE = new HashMap<>();

    // ==================== MAIN PROCESSING METHODS ====================

    /**
     * Process block breaking with augments.
     * Returns true if augments handled the break (skip vanilla drops).
     */
    public static boolean processBlockBreak(ItemStack tool, Level level, BlockState state,
                                            BlockPos pos, LivingEntity miner) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return false;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());
        if (augments.isEmpty()) {
            return false;
        }

        // Sort by priority (lower = earlier)
        augments.sort(Comparator.comparingInt(a -> a.handler.priority()));

        boolean anyHandled = false;

        for (ActiveAugment augment : augments) {
            // REMOVED ALL SKIP LOGIC - Let every handler run in priority order
            // Each handler is responsible for checking other augments internally

            // Execute the handler
            boolean handled = augment.handler.handleBlockBreak(tool, level, state, pos, miner);

            if (handled) {
                anyHandled = true;

                // If exclusive, stop processing
                if (augment.handler.isExclusive()) {
                    System.out.println("[AugmentProcessor] Exclusive augment '" + augment.id + "' handled break, stopping chain");
                    break;
                }
            }
        }

        return anyHandled;
    }

    /**
     * Process entity attack with augments.
     * Returns true if augments handled the attack.
     */
    public static boolean processEntityAttack(ItemStack tool, LivingEntity attacker,
                                              Entity target, float damage) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return false;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());
        if (augments.isEmpty()) {
            return false;
        }

        augments.sort(Comparator.comparingInt(a -> a.handler.priority()));

        boolean anyHandled = false;

        for (ActiveAugment augment : augments) {
            if (!augment.handler.canHandleEntity(target, tool)) {
                continue;
            }

            boolean handled = augment.handler.handleEntityAttack(tool, attacker, target, damage);

            if (handled) {
                anyHandled = true;

                if (augment.handler.isExclusive()) {
                    System.out.println("[AugmentProcessor] Exclusive augment '" + augment.id + "' handled attack, stopping chain");
                    break;
                }
            }
        }

        return anyHandled;
    }

    /**
     * Tick all augments on held tool.
     */
    public static void processToolTick(ItemStack tool, Player player) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());

        for (ActiveAugment augment : augments) {
            try {
                augment.handler.onToolUpdate(tool, player);
            } catch (Exception e) {
                System.err.println("[AugmentProcessor] Error ticking augment '" + augment.id + "': " + e.getMessage());
            }
        }
    }

    /**
     * Called when tool is equipped.
     */
    public static void processToolEquip(ItemStack tool, Player player) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());

        for (ActiveAugment augment : augments) {
            try {
                augment.handler.onToolEquip(tool, player);
            } catch (Exception e) {
                System.err.println("[AugmentProcessor] Error equipping augment '" + augment.id + "': " + e.getMessage());
            }
        }
    }

    /**
     * Called when tool is unequipped.
     */
    public static void processToolUnequip(ItemStack tool, Player player) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());

        for (ActiveAugment augment : augments) {
            try {
                augment.handler.onToolUnequip(tool, player);
            } catch (Exception e) {
                System.err.println("[AugmentProcessor] Error unequipping augment '" + augment.id + "': " + e.getMessage());
            }
        }
    }

    // ==================== STAT MODIFICATION METHODS ====================

    /**
     * Apply all augment mining speed modifiers.
     * Returns modified speed.
     */
    public static float applyMiningSpeedModifiers(ItemStack tool, float baseSpeed, BlockState state) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return baseSpeed;
        }

        float speed = baseSpeed;
        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());

        for (ActiveAugment augment : augments) {
            // Check if augment affects mining speed
            if ("efficiency".equals(augment.id)) {
                int tier = AugmentManager.getTier(augment.id, tool);
                speed += getEfficiencyBonus(tier, speed);
            }
        }

        return speed;
    }

    /**
     * Check if any augment modifies tool correctness for drops.
     * Returns true if augments say tool is correct.
     */
    public static boolean checkToolCorrectness(ItemStack tool, BlockState state) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return false;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());

        // Currently no augments modify this, but the hook is here for future use
        return false;
    }

    // ==================== HELPER METHODS ====================

    /**
     * Get all active (installed + enabled) augments on a tool.
     */
    private static List<ActiveAugment> getActiveAugments(ItemStack tool, ArsenalToolType toolType) {
        List<ActiveAugment> result = new ArrayList<>();

        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return result;
        }

        var slots = arsenalTool.getAugmentSlots(tool);
        if (slots == null) {
            return result;
        }

        // Check each slot
        for (int i = 0; i < 4; i++) {
            String augmentId = slots.getAugmentInSlot(i);
            if (augmentId == null || augmentId.isEmpty()) {
                continue;
            }

            // Must be installed and enabled
            if (!AugmentManager.isInstalled(augmentId, tool)) {
                continue;
            }

            // Get handler
            AugmentHandler handler = getCachedHandler(augmentId);
            if (handler == null) {
                continue;
            }

            // Must support this tool type
            if (!handler.supportsToolType(toolType)) {
                continue;
            }

            int tier = AugmentManager.getTier(augmentId, tool);
            result.add(new ActiveAugment(augmentId, handler, tier));
        }

        return result;
    }

    /**
     * Get cached handler instance.
     */
    private static AugmentHandler getCachedHandler(String augmentId) {
        return HANDLER_CACHE.computeIfAbsent(augmentId, id -> {
            AugmentManager.AugmentDefinition def = AugmentManager.get(id);
            if (def != null) {
                try {
                    return def.handlerClass().getDeclaredConstructor().newInstance();
                } catch (Exception e) {
                    System.err.println("[AugmentProcessor] Failed to create handler for '" + id + "': " + e.getMessage());
                }
            }
            return null;
        });
    }

    /**
     * Calculate efficiency bonus based on tier.
     * Tier 1 = +2, Tier 2 = +10, Tier 3 = +26
     */
    private static float getEfficiencyBonus(int tier, float baseSpeed) {
        return switch (tier) {
            case 1 -> 2.0f;
            case 2 -> 10.0f;
            case 3 -> 26.0f;
            default -> 0.0f;
        };
    }

    // ==================== INTERNAL CLASSES ====================

    /**
     * Represents an active augment on a tool.
     */
    private static class ActiveAugment {
        final String id;
        final AugmentHandler handler;
        final int tier;

        ActiveAugment(String id, AugmentHandler handler, int tier) {
            this.id = id;
            this.handler = handler;
            this.tier = tier;
        }
    }

    // ==================== DEBUG UTILITIES ====================

    /**
     * Debug: Print all active augments on a tool.
     */
    public static void debugPrintAugments(ItemStack tool) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            System.out.println("[AugmentProcessor] Not an Arsenal tool");
            return;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());
        System.out.println("[AugmentProcessor] Active augments (" + augments.size() + "):");
        for (ActiveAugment aug : augments) {
            System.out.println("  - " + aug.id + " (tier " + aug.tier + ", priority " + aug.handler.priority() + ")");
        }
    }

    /**
     * Process entity hit with augments (on-hit effects: fire, freeze, bleed, etc.)
     * Called from UnifiedProcessor.processEntityHit() BEFORE crit is rolled.
     * Returns true if any augment handled the hit.
     */
    public static boolean processEntityHit(ItemStack tool, LivingEntity attacker,
                                           Entity target, float damage) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return false;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());
        if (augments.isEmpty()) {
            return false;
        }

        augments.sort(Comparator.comparingInt(a -> a.handler.priority()));

        boolean anyHandled = false;

        for (ActiveAugment augment : augments) {
            if (!augment.handler.canHandleEntity(target, tool)) {
                continue;
            }

            // Routes to AugmentHandler.handleEntityAttack — weapon augments implement this
            // for on-hit effects. Rename to handleEntityHit in AugmentHandler when
            // weapon augments are added, for now reuses the existing hook.
            boolean handled = augment.handler.handleEntityAttack(tool, attacker, target, damage);

            if (handled) {
                anyHandled = true;

                if (augment.handler.isExclusive()) {
                    System.out.println("[AugmentProcessor] Exclusive augment '" + augment.id + "' handled entity hit, stopping chain");
                    break;
                }
            }
        }

        return anyHandled;
    }

    /**
     * Process entity kill with augments (on-kill effects: looting, soul harvest, etc.)
     * Called from UnifiedProcessor.processEntityKill().
     */
    public static void processEntityKill(ItemStack tool, LivingEntity attacker,
                                         LivingEntity killed) {
        if (!(tool.getItem() instanceof ArsenalTool arsenalTool)) {
            return;
        }

        List<ActiveAugment> augments = getActiveAugments(tool, arsenalTool.getToolType());
        if (augments.isEmpty()) {
            return;
        }

        augments.sort(Comparator.comparingInt(a -> a.handler.priority()));

        for (ActiveAugment augment : augments) {
            try {
                // On-kill hook — add onEntityKill() to AugmentHandler interface
                // when weapon augments are implemented. Stub here is safe to call.
                augment.handler.onEntityKill(tool, attacker, killed);
            } catch (Exception e) {
                System.err.println("[AugmentProcessor] Error processing kill for augment '"
                        + augment.id + "': " + e.getMessage());
            }
        }
    }

}