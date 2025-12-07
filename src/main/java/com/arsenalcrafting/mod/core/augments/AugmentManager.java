package com.arsenalcrafting.mod.core.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.function.BooleanSupplier;

public class AugmentManager {
    // Registry of all augment definitions
    private static final Map<String, AugmentDefinition> DEFINITIONS = new LinkedHashMap<>();

    public record AugmentDefinition(
            String id,
            String name,
            String description,
            Class<? extends AugmentHandler> handlerClass,
            boolean hasTiers,
            int maxTier
    ) {}

    static {
        // Register all augments - SINGLE SOURCE OF TRUTH
        register(new AugmentDefinition(
                "auto_smelt", "Auto Smelt", "Smelts ores automatically",
                AugmentHandlers.AutoSmelt.class, false, 1
        ));

        register(new AugmentDefinition(
                "ore_miner", "Ore Miner", "Vein mines ores",
                AugmentHandlers.OreMiner.class, false, 1
        ));

        register(new AugmentDefinition(
                "fortune", "Fortune", "Multiplies ore drops",
                AugmentHandlers.Fortune.class, true, 3
        ));
        register(new AugmentDefinition(
                "silk_touch", "Silk Touch", "Get the exact block",
                AugmentHandlers.SilkTouch.class, false, 1
        ));


        // Future augments go here ONLY

    }

    private static void register(AugmentDefinition def) {
        DEFINITIONS.put(def.id(), def);
    }

    // ========== PUBLIC API ==========

    public static Collection<AugmentDefinition> getAll() {
        return Collections.unmodifiableCollection(DEFINITIONS.values());
    }

    public static AugmentDefinition get(String id) {
        return DEFINITIONS.get(id);
    }

    public static boolean isInstalled(String augmentId, ItemStack tool) {
        return tool.getOrDefault(getEnabledComponent(augmentId), false);
    }

    public static int getTier(String augmentId, ItemStack tool) {
        AugmentDefinition def = get(augmentId);
        if (def != null && def.hasTiers()) {
            return tool.getOrDefault(getTierComponent(augmentId), 1);
        }
        return 1;
    }

    public static void install(String augmentId, ItemStack tool, int tier) {
        tool.set(getEnabledComponent(augmentId), true);

        AugmentDefinition def = get(augmentId);
        if (def != null && def.hasTiers()) {
            tool.set(getTierComponent(augmentId), Math.min(tier, def.maxTier()));
        }
    }

    public static void uninstall(String augmentId, ItemStack tool) {
        tool.remove(getEnabledComponent(augmentId));

        AugmentDefinition def = get(augmentId);
        if (def != null && def.hasTiers()) {
            tool.remove(getTierComponent(augmentId));
        }
    }

    public static DataComponentType<Boolean> getEnabledComponent(String augmentId) {
        return DynamicComponents.getEnabledComponent(augmentId);
    }

    public static DataComponentType<Integer> getTierComponent(String augmentId) {
        return DynamicComponents.getTierComponent(augmentId);
    }

    public static AugmentHandler getHandler(String augmentId) {
        AugmentDefinition def = get(augmentId);
        if (def != null) {
            try {
                return def.handlerClass().newInstance();
            } catch (Exception e) {
                throw new RuntimeException("Failed to instantiate handler for " + augmentId, e);
            }
        }
        return null;
    }

    public static String getTranslationKey(String augmentId) {
        return "augment.arsenalcrafting." + augmentId;
    }

    public static BooleanSupplier getTestModeSupplier(String augmentId) {
        return () -> com.arsenalcrafting.mod.config.TestModeConfig.isAugmentEnabled(augmentId);
    }
}