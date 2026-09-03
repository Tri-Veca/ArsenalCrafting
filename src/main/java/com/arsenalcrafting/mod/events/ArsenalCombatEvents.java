package com.arsenalcrafting.mod.events;

import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;

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

        // BIGGER, MORE VISIBLE PARTICLES
        serverLevel.sendParticles(
                ModParticles.RED_CRIT.get(),
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
    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        event.setDamageMultiplier(1.0F); // no bonus damage
    }
}