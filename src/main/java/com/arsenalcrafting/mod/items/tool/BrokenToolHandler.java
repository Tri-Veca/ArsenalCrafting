package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.BrokenState;
import com.arsenalcrafting.mod.registry.ModComponents;
import javax.annotation.Nullable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles broken tool state transitions and checks.
 *
 * Call checkAndBreak() from hurtDurability() overrides or anywhere
 * durability is consumed. Call isBroken() before any use logic.
 */
public final class BrokenToolHandler {

    private BrokenToolHandler() {}

    // ── STATE CHECKS ──────────────────────────────────────────────────────────

    public static boolean isBroken(ItemStack stack) {
        BrokenState state = stack.get(ModComponents.BROKEN_STATE.get());
        return state != null && state.isBroken();
    }

    // ── BREAK / REPAIR ────────────────────────────────────────────────────────

    /**
     * Call this instead of letting durability hit 0.
     * If durability would reach 0, marks the tool broken, plays break sound,
     * and sets durability to 1 so the item survives.
     *
     * @return true if the tool just broke this call
     */
    /**
     * Call this at the TOP of every damageItem() override.
     * Returns true if damage was intercepted (caller should return 0).
     *
     * Usage:
     *   if (BrokenToolHandler.interceptDamage(stack, amount, entity)) return 0;
     */
    public static <T extends net.minecraft.world.entity.LivingEntity> boolean interceptDamage(
            ItemStack stack, int amount, @Nullable T entity) {

        // Already broken — swallow all damage
        if (isBroken(stack)) return true;

        // Would reach max durability — mark broken instead
        if (entity instanceof Player player) {
            if (stack.getDamageValue() + amount >= stack.getMaxDamage()) {
                checkAndBreak(stack, player);
                return true;
            }
        }

        return false;
    }

    public static boolean checkAndBreak(ItemStack stack, Player player) {
        if (isBroken(stack)) return false; // already broken

        // Clamp durability to 1 so item doesn't vanish
        if (stack.getDamageValue() >= stack.getMaxDamage() - 1) {
            stack.setDamageValue(stack.getMaxDamage() - 1);
            markBroken(stack, player);
            return true;
        }
        return false;
    }

    /**
     * Marks the tool as broken and plays the item break sound.
     */
    public static void markBroken(ItemStack stack, Player player) {
        stack.set(ModComponents.BROKEN_STATE.get(), BrokenState.BROKEN);

        // Play vanilla item break sound at player position
        player.level().playSound(
                null, // null = broadcast to nearby players
                player.getX(), player.getY(), player.getZ(),
                SoundEvents.ITEM_BREAK,
                SoundSource.PLAYERS,
                0.8f,
                0.8f + player.level().getRandom().nextFloat() * 0.4f
        );

        System.out.println("[ArsenalTool] " + stack.getHoverName().getString() + " is BROKEN!");
    }

    /**
     * Clears the broken state — called from repair kit logic.
     */
    public static void markRepaired(ItemStack stack) {
        stack.remove(ModComponents.BROKEN_STATE.get());
    }
}