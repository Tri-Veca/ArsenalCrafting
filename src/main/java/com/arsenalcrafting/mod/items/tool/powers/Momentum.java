package com.arsenalcrafting.mod.items.tool.powers;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.CritProcessor;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class Momentum {
    private Momentum() {}

    /**
     * Thread-local flag — true when the block being processed is the origin block
     * the player directly mined, false for vein mine extras.
     * Set by ArsenalPickaxeItem.mineBlock() before calling processBlockBreak.
     */
    private static final ThreadLocal<Boolean> IS_ORIGIN_BLOCK = ThreadLocal.withInitial(() -> true);

    public static void setOriginBlock(boolean isOrigin) {
        IS_ORIGIN_BLOCK.set(isOrigin);
    }

    public static boolean isOriginBlock() {
        return IS_ORIGIN_BLOCK.get();
    }

    private static final int TIER_1 = 10;
    private static final int TIER_2 = 30;
    private static final int TIER_3 = 60;

    // Scaling Leeway (Ticks: 20 ticks = 1 second)
    private static final int LEEWAY_TIER_0 = 25; // 1.25s (Building up to Tier 1)
    private static final int LEEWAY_TIER_1 = 25; // 1.25s  (Building up to Tier 2)
    private static final int LEEWAY_TIER_2 = 25; // 1.25s  (Building up to Tier 3+)

    // Weapon-specific constants
    public static final int WEAPON_MAX_STACKS = 5;
    public static final int WEAPON_WINDOW_TICKS = 60; // 3 seconds
    public static final float WEAPON_CRIT_PER_STACK = 0.014f; // +1.4%


    /**
     * Called on each weapon hit (server side).
     * Stacks increment up to WEAPON_MAX_STACKS.
     * If previous hit already reached max (stacks == 5), reset to 0 before incrementing,
     * so the hit that granted the 5th stack benefited from +7% crit and then reset.
     */

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


    public static void onWeaponHit(ItemStack stack, LivingEntity player) {
        if (player.level().isClientSide) return;

        long now = player.level().getGameTime();
        DynamicComponents.MomentumData data = stack.getOrDefault(DynamicComponents.WEAPON_MOMENTUM(), DynamicComponents.MomentumData.EMPTY);

        int current = data.combo();
        long last = data.lastTime();

        // Decay if outside window
        if (now - last > WEAPON_WINDOW_TICKS) {
            current = 0;
        }

        // Reset after a max-stack hit
        if (current == WEAPON_MAX_STACKS) {
            current = 0;
        }

        int newStacks = Math.min(current + 1, WEAPON_MAX_STACKS);
        stack.set(DynamicComponents.WEAPON_MOMENTUM(), new DynamicComponents.MomentumData(newStacks, now));

    }


    /**
     * Returns current crit bonus (0.0 to 0.07) based on weapon stacks.
     * Only applies if Tin core is active (component set by onWeaponHit).
     */
    public static float getWeaponCritBonus(ItemStack stack) {

        ToolAssemblyData data = stack.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (data == null || data.isEmpty()) return 0f;

        AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
        if (coreMat == null) return 0f;

        CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
        if (def == null || !def.id().equals("momentum")) return 0f;

        DynamicComponents.MomentumData weaponData = stack.getOrDefault(DynamicComponents.WEAPON_MOMENTUM(), DynamicComponents.MomentumData.EMPTY);
        if (weaponData.combo() <= 0) return 0f;
        float momentumCritBonus = weaponData.combo() * WEAPON_CRIT_PER_STACK;

        CoreDebug.proc("Momentum", "Current Crit Chance Bonus: " + String.format("%.1f%%", momentumCritBonus * 100));
        return weaponData.combo() * WEAPON_CRIT_PER_STACK;

    }

    /**
     * Decay weapon stacks if window expired. Called from MomentumCore.onToolUpdate.
     */
    public static void decayWeaponStacksIfNeeded(ItemStack stack, Level level) {
        if (level.isClientSide) return;

        DynamicComponents.MomentumData data = stack.getOrDefault(DynamicComponents.WEAPON_MOMENTUM(), DynamicComponents.MomentumData.EMPTY);
        if (data.combo() > 0) {
            long now = level.getGameTime();
            if (now - data.lastTime() > WEAPON_WINDOW_TICKS) {
                stack.set(DynamicComponents.WEAPON_MOMENTUM(), DynamicComponents.MomentumData.EMPTY);
            }
        }
    }


}