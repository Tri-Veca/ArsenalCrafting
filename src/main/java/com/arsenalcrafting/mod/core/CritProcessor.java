package com.arsenalcrafting.mod.core;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;

/**
 * CritProcessor — Arsenal Crafting Crit System
 *
 * Handles crit chance rolling, crit damage calculation, and armor durability damage.
 *
 * Formula:
 *   critChance  = (material.getCritChance() × weaponClassMult) + flatBonuses   [cap: 60%]
 *   critDamage  = baseDamage × (material.getCritDamage() × weaponDamageMult)
 *   armorDmg    = critBonusDamage × ARMOR_DAMAGE_RATIO
 *
 * Called from ArsenalTool.rollCrit() after UnifiedProcessor.processEntityHit().
 */
public class CritProcessor {

    // ── TUNING CONSTANTS ──────────────────────────────────────────────────────

    /** Hard cap on crit chance regardless of bonuses. */
    public static final float MAX_CRIT_CHANCE = 0.60f;

    /**
     * Percentage of crit bonus damage that also hits a random armor piece's durability.
     * 0.28 = 28% of bonus damage converts to armor durability damage.
     * Adjust here to tune the ninja vs tank fantasy.
     */
    public static final float ARMOR_DAMAGE_RATIO = 0.28f;

    // ── RESULT RECORD ─────────────────────────────────────────────────────────

    /**
     * Immutable result returned from processCrit().
     * Check isCrit() before applying armorDurabilityDamage.
     */
    public record CritResult(
            boolean isCrit,
            float finalDamage,
            float armorDurabilityDamage
    ) {
        /** Convenience factory for a non-crit result. */
        public static CritResult noCrit(float damage) {
            return new CritResult(false, damage, 0f);
        }
    }

    // ── MAIN ENTRY POINT ──────────────────────────────────────────────────────

    /**
     * Primary crit processing method. Called from ArsenalTool.rollCrit().
     *
     * @param weapon                 The weapon ItemStack
     * @param baseDamage             Damage before crit
     * @param weaponCritChanceMult   From weapon class (getCritChanceMultiplier())
     * @param weaponCritDamageMult   From weapon class (getCritDamageMultiplier())
     * @param flatBonuses            Extra flat crit chance — pass 0f until Tin core / helmet skill implemented
     * @return CritResult with finalDamage and armorDurabilityDamage
     */
    public static CritResult processCrit(
            ItemStack weapon,
            float baseDamage,
            float weaponCritChanceMult,
            float weaponCritDamageMult,
            float flatBonuses) {

        AlloyMaterial headMat = getHeadMaterial(weapon);
        if (headMat == null) return CritResult.noCrit(baseDamage);

        float chance = calcCritChance(headMat, weaponCritChanceMult, flatBonuses);
        if (!rollCrit(chance)) return CritResult.noCrit(baseDamage);

        // Crit landed — calculate damage
        float critMultiplier = headMat.getCritDamage() * weaponCritDamageMult;
        float critBonus      = (baseDamage * critMultiplier) - baseDamage;
        float finalDamage    = baseDamage + critBonus;
        float armorDmg       = critBonus * ARMOR_DAMAGE_RATIO;

        return new CritResult(true, finalDamage, Math.max(0f, armorDmg));
    }

    // ── CRIT CHANCE ───────────────────────────────────────────────────────────

    /**
     * Calculates final crit chance, capped at MAX_CRIT_CHANCE.
     * Public so Tin core and helmet skills can read the value for display purposes.
     */
    public static float calcCritChance(
            AlloyMaterial mat,
            float weaponMult,
            float flatBonus) {
        float raw = (mat.getCritChance() * weaponMult) + flatBonus;
        return Math.min(raw, MAX_CRIT_CHANCE);
    }

    private static boolean rollCrit(float chance) {
        return Math.random() < chance;
    }

    // ── ARMOR DURABILITY DAMAGE ───────────────────────────────────────────────

    /**
     * Applies armor durability damage to a random occupied armor slot on the target.
     * Works on both Players (real durability) and Mobs (silently skips if no armor).
     *
     * Call this after processCrit() only if result.isCrit() is true.
     *
     * @param target                 The entity that was crit
     * @param armorDurabilityDamage  Value from CritResult.armorDurabilityDamage()
     */
    public static void applyArmorDurabilityDamage(
            LivingEntity target,
            float armorDurabilityDamage) {

        if (armorDurabilityDamage <= 0f) return;
        if (!(target.level() instanceof ServerLevel)) return;
        ServerLevel level = (ServerLevel) target.level();

        EquipmentSlot[] armorSlots = {
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };

        // Collect slots that actually have damageable armor
        List<EquipmentSlot> occupied = new ArrayList<>();
        for (EquipmentSlot slot : armorSlots) {
            ItemStack piece = target.getItemBySlot(slot);
            if (!piece.isEmpty() && piece.isDamageableItem()) {
                occupied.add(slot);
            }
        }

        if (occupied.isEmpty()) return;

        // Pick a random occupied slot
        EquipmentSlot chosen = occupied.get(level.getRandom().nextInt(occupied.size()));

        ItemStack armorPiece = target.getItemBySlot(chosen);
        int damage = Math.max(1, Math.round(armorDurabilityDamage));
        armorPiece.hurtAndBreak(damage, target, chosen);
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /**
     * Reads the head material from ToolAssemblyData.
     * Head material drives crit stats — same as how it drives mining speed and damage.
     */
    private static AlloyMaterial getHeadMaterial(ItemStack weapon) {
        ToolAssemblyData data = weapon.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (data == null || data.isEmpty()) return null;
        return AlloyMaterial.fromName(data.headMaterial());
    }
}