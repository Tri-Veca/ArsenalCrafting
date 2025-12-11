package com.arsenalcrafting.mod.core.alloy;

import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.Nullable;

public enum AlloyMaterial {

    COPPER(
            "Copper",
            0,
            190,
            5.0f,
            0.75f,
            1.0f,
            1.0f,
            1.0f,
            4,
            ChatFormatting.RED,
            0xFFe77c56,
            false
    ),
    IRON(
            "Iron",
            2,
            250,
            6.0f,
            0.9f,
            2.0f,
            1.0f,
            1.0f,
            4,
            ChatFormatting.WHITE,
            0xFFFFFFFF,
            false
    ),
    GOLD(
            "Gold",
            2,
            32,
            12.0f,
            1.2f,
            1.0f,
            1.0f,
            1.0f,
            4,
            ChatFormatting.YELLOW,
            0xFFfdf55f,
            false
    ),
    DIAMOND(
            "Diamond",
            3,
            1561,
            8.0f,
            1.0f,
            3.0f,
            1.0f,
            1.0f,
            4,
            ChatFormatting.AQUA,
            0xFF4aedd9,
            false
    ),

    BRONZE(
            "Bronze",
            0,
            500,
            5.0f,
            0.75f,
            1.0f,
            1.0f,
            1.0f,
            4,
            ChatFormatting.GOLD,
            0xFFE5A01D,
            true
    ),
    STEEL(
            "Steel",
            3,
            1500,
            7.0f,
            1.5f,
            0.8f,
            2.0f,
            1.2f,
            4,
            ChatFormatting.GRAY,
            0xFFC0C0C0,
            true
    ),
    TITANIUM(
            "Titanium",
            4,
            5000,
            9.0f,
            3.0f,
            0.6f,
            5.0f,
            1.5f,
            4,
            ChatFormatting.AQUA,
            0xFFA0E4FF,
            true
    );


    private final String name;
    private final int tier;
    private final int durability;
    private final float efficiency;
    private final float modifier;
    private final float attackSpeed;
    private final float damage;
    private final float arc;
    private final int toolAugmentSlots;
    private final ChatFormatting color;
    private final int itemColor;
    private final boolean allowAlloy;


    AlloyMaterial(String name,
                  int tier,
                  int durability,
                  float efficiency,
                  float modifier,
                  float attackSpeed,
                  float damage,
                  float arc,
                  int toolAugmentSlots,
                  ChatFormatting color,
                  int itemColor,
                  boolean allowAlloy) {
        this.name = name;
        this.tier = tier;
        this.durability = durability;
        this.efficiency = efficiency;
        this.modifier = modifier;
        this.attackSpeed = attackSpeed;
        this.damage = damage;
        this.arc = arc;
        this.toolAugmentSlots = toolAugmentSlots;
        this.color = color;
        this.itemColor = itemColor;
        this.allowAlloy = allowAlloy;
    }


    public String getName() { return name; }
    public int getItemColor() { return itemColor; }

    public int getDurability() { return durability; }
    public float getEfficiency() { return efficiency; }
    public float getModifier() { return modifier; }
    public float getAttackSpeed() { return attackSpeed; }
    public float getDamage() { return damage; }
    public float getArc() { return arc; }
    public int getToolAugmentSlots() { return toolAugmentSlots; }
    public ChatFormatting getColor() { return color; }
    public int getTier() { return tier; }
    public boolean allowsAlloy() { return allowAlloy; }


    // --- helpers ---

    @Nullable
    public static AlloyMaterial fromName(String name) {
        if (name == null || name.isEmpty()) {
            return null;
        }
        for (AlloyMaterial m : values()) {
            if (m.name.equalsIgnoreCase(name)) {
                return m;
            }
        }
        return null;
    }


    /** Returns the highest-tier material from the given list (ignores nulls). */
    @Nullable
    public static AlloyMaterial highestOf(@Nullable AlloyMaterial... materials) {
        AlloyMaterial highest = null;
        if (materials == null) return null;

        for (AlloyMaterial m : materials) {
            if (m == null) continue;
            if (highest == null || m.tier > highest.tier) {
                highest = m;
            }
        }
        return highest;
    }
}
