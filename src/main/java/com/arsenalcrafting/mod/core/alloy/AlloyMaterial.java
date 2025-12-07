package com.arsenalcrafting.mod.core.alloy;

import net.minecraft.ChatFormatting;

public enum AlloyMaterial {

    BRONZE(
            "Bronze",
            500,
            5.0f,
            0.75f,
            1.0f,
            1.0f,
            1.0f,
            4,
            ChatFormatting.GOLD,
            0xFFFFC46B // <- was 0xFFC46B, now ARGB: FF (alpha) + FFC46B
    ),
    STEEL(
            "Steel",
            1500,
            7.0f,
            1.5f,
            0.8f,
            2.0f,
            1.2f,
            4,
            ChatFormatting.GRAY,
            0xFFC0C0C0 // if you want, make this 0xFFC0C0C0 -> already has alpha if 8 hex digits
    ),
    TITANIUM(
            "Titanium",
            5000,
            9.0f,
            3.0f,
            0.6f,
            5.0f,
            1.5f,
            4,
            ChatFormatting.AQUA,
            0xFFA0E4FF
    );


    private final String name;
    private final int durability;
    private final float efficiency;
    private final float modifier;
    private final float attackSpeed;
    private final float damage;
    private final float arc;

    public int getDurability() {
        return durability;
    }

    public float getEfficiency() {
        return efficiency;
    }

    public float getModifier() {
        return modifier;
    }

    public float getAttackSpeed() {
        return attackSpeed;
    }

    public float getDamage() {
        return damage;
    }

    public float getArc() {
        return arc;
    }

    public int getToolAugmentSlots() {
        return toolAugmentSlots;
    }

    public ChatFormatting getColor() {
        return color;
    }

    private final int toolAugmentSlots;
    private final ChatFormatting color;
    private final int itemColor;

    AlloyMaterial(String name,
                  int durability,
                  float efficiency,
                  float modifier,
                  float attackSpeed,
                  float damage,
                  float arc,
                  int toolAugmentSlots,
                  ChatFormatting color,
                  int itemColor) {
        this.name = name;
        this.durability = durability;
        this.efficiency = efficiency;
        this.modifier = modifier;
        this.attackSpeed = attackSpeed;
        this.damage = damage;
        this.arc = arc;
        this.toolAugmentSlots = toolAugmentSlots;
        this.color = color;
        this.itemColor = itemColor;
    }

    public String getName() { return name; }
    public int getItemColor() { return itemColor; }
}
