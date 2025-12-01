package com.arsenalcrafting.mod.core.augments;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

/**
 * Augment for logic that executes when an entity is successfully hit (e.g., Lifesteal, Poison Damage).
 */
public interface IEntityHitAugment extends Augment {
    /**
     * Executes the augment logic when the tool successfully hits an entity.
     *
     * @param toolStack The tool ItemStack used for the attack.
     * @param target The target entity that was hit.
     * @param attacker The entity (usually the player) performing the attack.
     * @return True if the augment applied a significant effect.
     */
    boolean onHitEntity(@NotNull ItemStack toolStack, @NotNull LivingEntity target, @NotNull LivingEntity attacker);
}