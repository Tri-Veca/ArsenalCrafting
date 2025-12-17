package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MagnetismAugmentItem extends Item implements Augment, InstallableAugmentItem {

    public MagnetismAugmentItem(Properties properties) {
        super(properties);
    }

    public static void tickMagnet(ItemStack tool, Player player) {
        // Logic only runs on server and if augment is active
        if (player.level().isClientSide || !AugmentManager.isActive("magnetism", tool)) return;

        // Fixed radius of 6 blocks
        double range = 6.0;

        AABB area = player.getBoundingBox().inflate(range);
        List<ItemEntity> items = player.level().getEntitiesOfClass(ItemEntity.class, area);

        for (ItemEntity item : items) {
            // Check pickup delay to avoid items "sticking" to the ground immediately after dropping
            if (item.getItem().isEmpty() || item.hasPickUpDelay()) continue;

            // Calculate vector toward player's center
            Vec3 targetPos = player.position().add(0, 0.75, 0);
            Vec3 motion = targetPos.subtract(item.position()).normalize().scale(0.2);

            item.setDeltaMovement(item.getDeltaMovement().add(motion));
            item.hasImpulse = true;
        }
    }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("magnetism"), true);
    }
}