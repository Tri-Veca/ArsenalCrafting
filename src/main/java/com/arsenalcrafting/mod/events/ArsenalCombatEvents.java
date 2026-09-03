package com.arsenalcrafting.mod.events;

import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.registry.ModParticles;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = "arsenalcrafting")
public class ArsenalCombatEvents {

    public static void spawnRedCritParticles(LivingEntity target) {
        if (!(target.level() instanceof ServerLevel serverLevel)) return;

        serverLevel.sendParticles(
                ModParticles.RED_CRIT.get(),
                target.getX(),
                target.getY() + target.getBbHeight() * 0.5,
                target.getZ(),
                12,
                0.3, 0.3, 0.3,
                0.1
        );
    }

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        ItemStack weapon = player.getMainHandItem();

        if (weapon.getItem() instanceof ArsenalTool && event.isCriticalHit()) {
            event.setCriticalHit(false);
            event.setDamageMultiplier(1.0F);
        }
    }
}
