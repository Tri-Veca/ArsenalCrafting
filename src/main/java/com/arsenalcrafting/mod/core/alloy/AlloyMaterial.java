package com.arsenalcrafting.mod.core.alloy;

import net.minecraft.ChatFormatting;

public enum AlloyMaterial {
    //premature version of the alloy material. name, eff, damage, augmentslot counter all work.
    // may add, crit chance and crit damage.
    BRONZE("Bronze", 500, 5.0f, 0.75f, 1.0f, 1.0f, 1.0f, 4, ChatFormatting.GOLD),

    // Future Alloys, still subject to change
    STEEL("steel", 1500, 7.0f, 1.5f, 0.8f, 2.0f, 1.2f, 4, ChatFormatting.GRAY),
    TITANIUM("titanium", 5000, 9.0f, 3.0f, 0.6f, 5.0f, 1.5f, 4, ChatFormatting.AQUA);

    private final String name;
    private final int durability;
    private final float efficiency;
    private final float modifier; // repairability, not set on how values will work
    private final float attackSpeed;
    private final float damage;
    private final float arc; // Arc of attack (for weapons)
    private final int toolAugmentSlots;
    private final ChatFormatting color;

    AlloyMaterial(String name, int durability, float efficiency, float modifier, float attackSpeed, float damage, float arc, int toolAugmentSlots, ChatFormatting color) {
        this.name = name;
        this.durability = durability;
        this.efficiency = efficiency;
        this.modifier = modifier;
        this.attackSpeed = attackSpeed;
        this.damage = damage;
        this.arc = arc;
        this.toolAugmentSlots = toolAugmentSlots;
        this.color = color;
    }

    public String getName() { return name; }
    public int getDurability() { return durability; }
    public float getEfficiency() { return efficiency; }
    public float getModifier() { return modifier; }
    public float getAttackSpeed() { return attackSpeed; }
    public float getDamage() { return damage; }
    public float getArc() { return arc; }
    public int getToolAugmentSlots() { return toolAugmentSlots; }
    public ChatFormatting getColor() { return color; }
}