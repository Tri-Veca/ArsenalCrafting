package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModParticles {
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(BuiltInRegistries.PARTICLE_TYPE, ArsenalCrafting.MOD_ID);

    public static final Supplier<SimpleParticleType> RED_CRIT =
            PARTICLE_TYPES.register("red_crit", () -> new SimpleParticleType(false));

    public static void register(net.neoforged.bus.api.IEventBus eventBus) {
        PARTICLE_TYPES.register(eventBus);
    }
}