package com.arsenalcrafting.mod.events;

import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.augments.LootingAugmentItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Applies looting augment bonus drops.
 *
 * NeoForge 1.21.1: LootingLevelEvent does not exist (dropped with data-driven enchantments).
 * LivingDropsEvent also does NOT expose getLootingLevel()/setLootingLevel() in 1.21.1.
 *
 * Solution: intercept the resolved drops list and add bonus item copies per tier,
 * matching what vanilla Looting I/II/III does (+1/+2/+3 max stack size and extra rolls).
 * We duplicate a random subset of drops — simple and effective.
 */
@EventBusSubscriber(modid = "arsenalcrafting", bus = EventBusSubscriber.Bus.GAME)
public class ArsenalLootingEvent {

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getSource().getEntity() instanceof Player killer)) return;

        ItemStack weapon = killer.getMainHandItem();
        if (!(weapon.getItem() instanceof ArsenalTool)) return;
        if (!AugmentManager.isInstalled("looting", weapon)) return;

        int tier = AugmentManager.getTier("looting", weapon);
        int bonusDrops = LootingAugmentItem.getLootingLevel(tier); // 1, 2, or 3 extra rolls
        if (bonusDrops <= 0) return;

        List<ItemEntity> existing = (List<ItemEntity>) event.getDrops();
        if (existing.isEmpty()) return;

        List<ItemEntity> bonus = new ArrayList<>();
        Random rng = new Random();

        for (int i = 0; i < bonusDrops; i++) {
            // Pick a random drop from the existing list to duplicate
            ItemEntity source = existing.get(rng.nextInt(existing.size()));
            ItemStack copy = source.getItem().copy();
            copy.setCount(1);

            ItemEntity bonusEntity = new ItemEntity(
                    event.getEntity().level(),
                    event.getEntity().getX(),
                    event.getEntity().getY(),
                    event.getEntity().getZ(),
                    copy
            );
            bonus.add(bonusEntity);
        }

        existing.addAll(bonus);

        System.out.println("[ArsenalLooting] tier=" + tier
                + " +" + bonus.size() + " bonus drop(s)"
                + " from " + event.getEntity().getName().getString());
    }
}