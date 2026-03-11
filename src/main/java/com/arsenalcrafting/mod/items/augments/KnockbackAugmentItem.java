package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class KnockbackAugmentItem extends Item implements Augment, InstallableAugmentItem {

    private final int tier;

    // Knockback strength per tier (vanilla sprint-attack is ~0.4)
    private static final float[] STRENGTH = { 0f, 0.4f, 0.7f, 1.1f };

    public KnockbackAugmentItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() { return tier; }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("knockback"), true);
        tool.set(DynamicComponents.getTierComponent("knockback"), tier);
        System.out.println("[Toolkit] Knockback tier " + tier + " installed!");
    }

    /**
     * Apply knockback to target away from attacker.
     * Called from AugmentHandlers.Knockback.
     */
    public static boolean applyKnockback(ItemStack tool, LivingEntity attacker, LivingEntity target) {
        if (!tool.getOrDefault(DynamicComponents.getEnabledComponent("knockback"), false)) return false;

        int tier = tool.getOrDefault(DynamicComponents.getTierComponent("knockback"), 1);
        if (tier <= 0 || tier >= STRENGTH.length) return false;

        float strength = STRENGTH[tier];

        double dx = target.getX() - attacker.getX();
        double dz = target.getZ() - attacker.getZ();
        double dist = Math.sqrt(dx * dx + dz * dz);

        if (dist < 0.001) {
            double yaw = Math.toRadians(attacker.getYRot());
            dx = -Math.sin(yaw);
            dz = Math.cos(yaw);
            dist = 1.0;
        }

        double scale = strength / dist;
        var cur = target.getDeltaMovement();
        target.setDeltaMovement(
                cur.x / 2.0 + dx * scale,
                cur.y / 2.0 + 0.1,
                cur.z / 2.0 + dz * scale
        );

        System.out.println("[KnockbackAugment] tier=" + tier
                + " strength=" + strength
                + " -> " + target.getName().getString());
        return true;
    }
}