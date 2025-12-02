package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
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

    // Registry: Component Type -> Handler Instance
    private static final Map<DataComponentType<Boolean>, AugmentHandler> REGISTRY = new HashMap<>();

    static {
        // Register handlers from AugmentHandlers
        register(ModComponents.AUTO_SMELT_ENABLED.get(), new AugmentHandlers.AutoSmelt());
        register(ModComponents.ORE_MINER_ENABLED.get(), new AugmentHandlers.OreMiner());
        // Future: register(ModComponents.MAGNET_ENABLED.get(), new AugmentHandlers.Magnet());
        // Future: register(ModComponents.FIRE_ASPECT_ENABLED.get(), new AugmentHandlers.FireAspect());
        // Future: register(ModComponents.LIFE_STEAL_ENABLED.get(), new AugmentHandlers.LifeSteal());
    }

    private static void register(DataComponentType<Boolean> componentKey, AugmentHandler handler) {
        REGISTRY.put(componentKey, handler);
    }

    // ============= PUBLIC PROCESSING METHODS =============

    /**
     * Process block breaking with all applicable augments
     */
    public static ProcessResult processBlockBreak(ItemStack tool, ArsenalToolType toolType,
                                                  Level level, BlockState state,
                                                  BlockPos pos, LivingEntity miner) {

        List<AugmentHandler> handlers = getHandlersForTool(tool, toolType);
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

    /**
     * Process entity attacks with all applicable augments
     */
    public static ProcessResult processEntityAttack(ItemStack tool, ArsenalToolType toolType,
                                                    LivingEntity attacker, Entity target,
                                                    float damage) {

        List<AugmentHandler> handlers = getHandlersForTool(tool, toolType);
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
    public static void processToolUpdate(ItemStack tool, ArsenalToolType toolType, Player player) {
        List<AugmentHandler> handlers = getHandlersForTool(tool, toolType);

        for (AugmentHandler handler : handlers) {
            handler.onToolUpdate(tool, player);
        }
    }

    /**
     * Called when tool is equipped
     */
    public static void processToolEquip(ItemStack tool, ArsenalToolType toolType, Player player) {
        List<AugmentHandler> handlers = getHandlersForTool(tool, toolType);

        for (AugmentHandler handler : handlers) {
            handler.onToolEquip(tool, player);
        }
    }

    /**
     * Called when tool is unequipped
     */
    public static void processToolUnequip(ItemStack tool, ArsenalToolType toolType, Player player) {
        List<AugmentHandler> handlers = getHandlersForTool(tool, toolType);

        for (AugmentHandler handler : handlers) {
            handler.onToolUnequip(tool, player);
        }
    }

    // ============= HELPER METHODS =============

    /**
     * Get all active handlers for a tool, filtered by tool type
     */
    private static List<AugmentHandler> getHandlersForTool(ItemStack tool, ArsenalToolType toolType) {
        List<AugmentHandler> handlers = new ArrayList<>();

        for (Map.Entry<DataComponentType<Boolean>, AugmentHandler> entry : REGISTRY.entrySet()) {
            if (tool.getOrDefault(entry.getKey(), false)) {
                AugmentHandler handler = entry.getValue();
                if (handler.supportsToolType(toolType)) {
                    handlers.add(handler);
                }
            }
        }

        return handlers;
    }

    /**
     * Detect special combo effects
     */
    private static void detectCombos(List<AugmentHandler> handlers, ProcessResult result) {
        boolean hasAutoSmelt = handlers.stream().anyMatch(h -> h instanceof AugmentHandlers.AutoSmelt);
        boolean hasOreMiner = handlers.stream().anyMatch(h -> h instanceof AugmentHandlers.OreMiner);

        if (hasAutoSmelt && hasOreMiner) {
            result.specialCombo = "SMELT_VEIN";
            result.comboDescription = "Auto-Smelt + Ore-Miner: Smelts entire ore veins";
        }

        // Add more combos as needed
        boolean hasFireAspect = handlers.stream().anyMatch(h -> h instanceof AugmentHandlers.FireAspect);
        boolean hasLifeSteal = handlers.stream().anyMatch(h -> h instanceof AugmentHandlers.LifeSteal);

        if (hasFireAspect && hasLifeSteal) {
            result.specialCombo = "VAMPIRIC_FLAME";
            result.comboDescription = "Fire Aspect + Life Steal: Burning enemies heal you";
        }
    }

    /**
     * Check if a specific augment is active on a tool
     */
    public static boolean hasAugment(ItemStack tool, Class<? extends AugmentHandler> handlerClass) {
        for (Map.Entry<DataComponentType<Boolean>, AugmentHandler> entry : REGISTRY.entrySet()) {
            if (tool.getOrDefault(entry.getKey(), false) &&
                    handlerClass.isInstance(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all active augment names for a tool
     */
    public static List<String> getActiveAugmentNames(ItemStack tool, @Nullable ArsenalToolType toolType) {
        List<String> names = new ArrayList<>();

        for (Map.Entry<DataComponentType<Boolean>, AugmentHandler> entry : REGISTRY.entrySet()) {
            if (tool.getOrDefault(entry.getKey(), false)) {
                AugmentHandler handler = entry.getValue();
                if (toolType == null || handler.supportsToolType(toolType)) {
                    names.add(handler.getClass().getSimpleName());
                }
            }
        }

        return names;
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