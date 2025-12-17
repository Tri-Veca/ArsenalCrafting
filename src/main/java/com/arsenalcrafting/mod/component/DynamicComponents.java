package com.arsenalcrafting.mod.component;

import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.Map;

public class DynamicComponents {

    private static final Map<String, DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>> ENABLED_COMPONENTS = new HashMap<>();
    private static final Map<String, DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>> TIER_COMPONENTS = new HashMap<>();

    // DeferredHolder for LIGHT_POS
    private static DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LIGHT_POS_HOLDER;

    /**
     * Returns the fully registered LIGHT_POS component.
     */
    public static DataComponentType<BlockPos> LIGHT_POS() {
        return LIGHT_POS_HOLDER.get();
    }

    /**
     * Registers all augment-related components plus LIGHT_POS.
     */
    public static void registerAll(DeferredRegister<DataComponentType<?>> register) {

        // Register augments
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            String id = def.id();

            // Enabled boolean
            DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> enabledHolder = register.register(
                    id + "_enabled",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .cacheEncoding()
                            .build()
            );
            ENABLED_COMPONENTS.put(id, enabledHolder);

            // Tier integer (if augment has tiers)
            if (def.hasTiers()) {
                DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> tierHolder = register.register(
                        id + "_tier",
                        () -> DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.INT)
                                .cacheEncoding()
                                .build()
                );
                TIER_COMPONENTS.put(id, tierHolder);
            }
        }
    }

    public static DataComponentType<Boolean> getEnabledComponent(String augmentId) {
        return ENABLED_COMPONENTS.get(augmentId).get();
    }

    public static DataComponentType<Integer> getTierComponent(String augmentId) {
        return TIER_COMPONENTS.get(augmentId).get();
    }
}
