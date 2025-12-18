package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.component.DynamicComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

public final class Momentum {
    private Momentum() {}

    private static final int TIER_1 = 10;
    private static final int TIER_2 = 30;
    private static final int TIER_3 = 60;

    // Scaling Leeway (Ticks: 20 ticks = 1 second)
    private static final int LEEWAY_TIER_0 = 25; // 1.25s (Building up to Tier 1)
    private static final int LEEWAY_TIER_1 = 25; // 1.25s  (Building up to Tier 2)
    private static final int LEEWAY_TIER_2 = 25; // 1.25s  (Building up to Tier 3+)

    public static void onBlockMined(ItemStack stack, LivingEntity player) {
        if (player.level().isClientSide) return;

        long currentTime = player.level().getGameTime();
        DynamicComponents.MomentumData data = stack.getOrDefault(
                DynamicComponents.MOMENTUM(),
                DynamicComponents.MomentumData.EMPTY
        );

        // Determine how much time they are allowed based on their CURRENT combo
        int allowedLeeway = LEEWAY_TIER_0;
        if (data.combo() >= TIER_2) {
            allowedLeeway = LEEWAY_TIER_2; // 3 seconds at high tiers
        } else if (data.combo() >= TIER_1) {
            allowedLeeway = LEEWAY_TIER_1; // 2 seconds at mid tiers
        }

        int newCombo;
        if (currentTime - data.lastTime() <= allowedLeeway) {
            newCombo = data.combo() + 1;
        } else {
            newCombo = 1; // Reset if they exceed their current tier's leeway
        }

        // Save back to item
        stack.set(DynamicComponents.MOMENTUM(), new DynamicComponents.MomentumData(newCombo, currentTime));

        applyEffect(player, newCombo);
    }

    private static void applyEffect(LivingEntity player, int combo) {

        // to ensure the Haste doesn't run out before the combo does.
        if (combo >= TIER_3) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 2, true, false)); // Haste 3
        } else if (combo >= TIER_2) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 1, true, false)); // Haste 2
        } else if (combo >= TIER_1) {
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 80, 0, true, false)); // Haste 1
        }
    }
}