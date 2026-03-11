package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class SharpnessAugmentItem extends Item implements Augment, InstallableAugmentItem {

    private final int tier;

    public SharpnessAugmentItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() { return tier; }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("sharpness"), true);
        tool.set(DynamicComponents.getTierComponent("sharpness"), tier);
        System.out.println("[Toolkit] Sharpness tier " + tier + " installed!");
    }

    /**
     * Returns the flat bonus damage for the given tier.
     * Called from AugmentHandlers.Sharpness.handleEntityAttack().
     */
    public static float getBonusDamage(int tier) {
        return switch (tier) {
            case 1 -> 1.5f;
            case 2 -> 3.0f;
            case 3 -> 5.0f;
            default -> 0f;
        };
    }

    /**
     * Apply sharpness bonus damage to a target.
     * Called from AugmentHandlers.Sharpness.
     */
    public static boolean applySharpness(ItemStack tool, LivingEntity attacker, LivingEntity target) {
        if (!tool.getOrDefault(DynamicComponents.getEnabledComponent("sharpness"), false)) return false;

        int tier = tool.getOrDefault(DynamicComponents.getTierComponent("sharpness"), 1);
        float bonus = getBonusDamage(tier);
        if (bonus <= 0) return false;

        var source = attacker instanceof Player p
                ? target.level().damageSources().playerAttack(p)
                : target.level().damageSources().mobAttack(attacker);
        target.hurt(source, bonus);

        System.out.println("[SharpnessAugment] tier=" + tier
                + " bonus=" + String.format("%.1f", bonus)
                + " -> " + target.getName().getString());
        return true;
    }
}