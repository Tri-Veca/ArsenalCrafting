package com.arsenalcrafting.mod.component;

import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.HashMap;
import java.util.Map;

public class DynamicComponents {

    private static final Map<String, DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>>> ENABLED_COMPONENTS = new HashMap<>();
    private static final Map<String, DeferredHolder<DataComponentType<?>, DataComponentType<Integer>>> TIER_COMPONENTS = new HashMap<>();

    private static DeferredHolder<DataComponentType<?>, DataComponentType<MomentumData>> MOMENTUM_HOLDER;
    private static DeferredHolder<DataComponentType<?>, DataComponentType<MomentumData>> WEAPON_MOMENTUM_HOLDER;

    private static DeferredHolder<DataComponentType<?>, DataComponentType<BlockPos>> LIGHT_POS_HOLDER;

    // Getter for Momentum Component
    public static DataComponentType<MomentumData> MOMENTUM() {
        return MOMENTUM_HOLDER.get();
    }
    public static DataComponentType<MomentumData> WEAPON_MOMENTUM() {
        return WEAPON_MOMENTUM_HOLDER.get();
    }



    public static DataComponentType<BlockPos> LIGHT_POS() {
        return LIGHT_POS_HOLDER.get();
    }

    public record MomentumData(int combo, long lastTime) {
        public static final Codec<MomentumData> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.INT.fieldOf("combo").forGetter(MomentumData::combo),
                Codec.LONG.fieldOf("last_time").forGetter(MomentumData::lastTime)
        ).apply(inst, MomentumData::new));

        public static final StreamCodec<ByteBuf, MomentumData> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, MomentumData::combo,
                ByteBufCodecs.VAR_LONG, MomentumData::lastTime,
                MomentumData::new
        );

        public static final MomentumData EMPTY = new MomentumData(0, 0L);
    }



    public static void registerAll(DeferredRegister<DataComponentType<?>> register) {

        // 1. Register standalone components OUTSIDE the loop
        MOMENTUM_HOLDER = register.register("momentum_data",
                () -> DataComponentType.<MomentumData>builder()
                        .persistent(MomentumData.CODEC)
                        .networkSynchronized(MomentumData.STREAM_CODEC)
                        .build()
        );

        WEAPON_MOMENTUM_HOLDER = register.register("weapon_momentum_data",
                () -> DataComponentType.<MomentumData>builder()
                        .persistent(MomentumData.CODEC)
                        .networkSynchronized(MomentumData.STREAM_CODEC)
                        .build()
        );


        // 2. Register augments inside the loop
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            String id = def.id();

            ENABLED_COMPONENTS.put(id, register.register(
                    id + "_enabled",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .cacheEncoding()
                            .build()
            ));

            if (def.hasTiers()) {
                TIER_COMPONENTS.put(id, register.register(
                        id + "_tier",
                        () -> DataComponentType.<Integer>builder()
                                .persistent(Codec.INT)
                                .networkSynchronized(ByteBufCodecs.INT)
                                .cacheEncoding()
                                .build()
                ));
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