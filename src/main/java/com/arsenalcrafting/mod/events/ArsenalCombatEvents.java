package com.arsenalcrafting.mod.events;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * Arsenal combat utilities.
 * Static helper only — no event subscriptions.
 */
public class ArsenalCombatEvents {

    /**
     * Spawn red DAMAGE_INDICATOR particles at the target on Arsenal crits.
     * Called directly from rollCrit() in each weapon — server side only.
     */
    public static void spawnRedCritParticles(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(
                ParticleTypes.DAMAGE_INDICATOR,
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                12,
                0.3,
                0.3,
                0.3,
                0.1
        );
    }
}