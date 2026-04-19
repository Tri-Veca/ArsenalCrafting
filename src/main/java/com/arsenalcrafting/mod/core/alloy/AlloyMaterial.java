package com.arsenalcrafting.mod.core.alloy;

import com.arsenalcrafting.mod.core.ItemStackHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public enum AlloyMaterial {
    /**
     * Lists all Materials. Crucial Class, is the backbone for the entire mod!

     */
    COPPER(
            "Copper",   //Name
            1,             //maps tier to Incorrect tool for block BlockTag
            190,        //durability, getMaxDamage
            5.0f,       //efficency, getDestroySpeed
            0.75f,        //how cheap are repairs? lower is better.
            1.6f,      //attack speed. default for swords
            0f,      // 0 extra dmg. Correct for Copper
            1.0f,      // how many can we swing?
            4,       //max aug slots
            0.05f,   //crit chance
            1.25f,   //crit damage multiplier
            3.0f,    //weapon damage (shown in attack indicator, base 1 added by vanilla)
            ChatFormatting.RED,   //tooltip color
            0xFFe77c56,          //tint color
            false,//allow alloy to be created automatically. We don't use for vanilla materials
            30,// 20 ticks = 1 second. how long to craft.
            true //Should Parts be created automatically. set to false in special cases like coal
    ),
    TIN(
            "Tin",
            1,
            220,
            5.0f,
            0.9f,
            1.6f,
            0f,
            1.0f,
            4,
            0.10f,   //crit chance
            1.35f,   //crit damage multiplier
            3.5f,    //weapon damage
            ChatFormatting.GRAY,
            0xFFeddfdb,
            true,
            20,
            true
    ),
    IRON(
            "Iron",
            2,
            250,
            6.0f,
            1.0f,
            1.6f,
            2.0f,
            1.0f,
            4,
            0.08f,   //crit chance
            1.30f,   //crit damage multiplier
            4.0f,    //weapon damage
            ChatFormatting.WHITE,
            0xFFFFFFFF,
            false,
            60,
            true
    ),
    GOLD(
            "Gold",
            2,
            32,
            12.0f,
            0.5f,
            1.6f,
            0f,
            1.0f,
            4,
            0.14f,   //crit chance — high crit, low multiplier (gambler)
            1.9f,   //crit damage multiplier
            3.5f,    //weapon damage — fast and crits often, not raw power
            ChatFormatting.YELLOW,
            0xFFfdf55f,
            false,
            10,
            true
    ),
    DIAMOND(
            "Diamond",
            3,
            1561,
            8.0f,
            1.0f,
            1.6f,
            3.0f,
            1.0f,
            4,
            0.12f,   //crit chance
            1.60f,   //crit damage multiplier — sweet spot of both
            6.0f,    //weapon damage
            ChatFormatting.AQUA,
            0xFF4aedd9,
            false,
            70,
            true
    ),
    NETHERITE(
            "Netherite",
            4,
            2031,
            9.0f,
            1.1f,
            1.6f,
            4.0f,
            1.0f,
            4,
            0.10f,   //crit chance
            1.60f,   //crit damage multiplier
            7.0f,    //weapon damage
            ChatFormatting.DARK_GRAY,
            0xFF706770,
            false,
            80,
            true
    ),

    BRONZE(
            "Bronze",
            2,
            500,
            6.0f,
            0.8f,
            1.6f,
            1.5f,
            1.0f,
            4,
            0.08f,   //crit chance
            1.25f,   //crit damage multiplier
            4.0f,
            ChatFormatting.GOLD,
            0xFFcb9580,
            true,
            40,
            true

    ),
    STEEL(
            "Steel",
            4,
            2500,
            8.5f,
            1.4f,
            1.2f,
            5.0f,
            1.2f,
            4,
            0.05f,   //crit chance
            1.50f,   //crit damage multiplier
            8.0f,    //weapon damage — high damage, slower speed
            ChatFormatting.GRAY,
            0xFFC0C0C0,
            true,
            90,
            true
    ),
    TITANIUM(
            "Titanium",
            4,
            1800,
            10.0f,
            1.1f,
            1.9f,
            4.0f,
            1.5f,
            4,
            0.09f,   //crit chance — rare but brutal
            1.70f,   //crit damage multiplier
            7.5f,    //weapon damage — fast + heavy crits, mid base
            ChatFormatting.DARK_AQUA,
            0xFFA0E4FF,
            true,
            100,
            true

    ),
    // non component materials
    COAL(
            "Coal",   //Name
            1,             //maps tier to Incorrect tool for block BlockTag
            190,        //durability, getMaxDamage
            5.0f,       //efficency, getDestroySpeed
            0.75f,        //how cheap are repairs? lower is better.
            1.6f,      //attack speed. default for swords
            0f,      // 0 extra dmg. Correct for Copper
            1.0f,      // how many can we swing?
            4,       //max aug slots
            0.05f,   //crit chance
            1.25f,   //crit damage multiplier
            3.0f,    //weapon damage (shown in attack indicator, base 1 added by vanilla)
            ChatFormatting.RED,   //tooltip color
            0xFFFFFACA,          //tint color
            false,//allow alloy to be created automatically. We don't use for vanilla materials
            30,
            false
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
    private final float critChance;
    private final float critDamage;
    private final float weaponDamage; // Separate from tool damage — used by weapons via getAssembledDamage()
    private final ChatFormatting color;
    private final int itemColor;
    private final boolean allowAlloy;
    public int meltTime;
    private final boolean shouldGenerateParts;


    AlloyMaterial(String name, int tier, int durability, float efficiency, float modifier, float attackSpeed, float damage, float arc, int toolAugmentSlots,
                  float critChance, float critDamage, float weaponDamage,
                  ChatFormatting color, int itemColor, boolean allowAlloy, int meltTime, boolean shouldGenerateParts) {
        this.name = name;
        this.tier = tier;
        this.durability = durability;
        this.efficiency = efficiency;
        this.modifier = modifier;
        this.attackSpeed = attackSpeed;
        this.damage = damage;
        this.arc = arc;
        this.toolAugmentSlots = toolAugmentSlots;
        this.critChance = critChance;
        this.critDamage = critDamage;
        this.weaponDamage = weaponDamage;
        this.color = color;
        this.itemColor = itemColor;
        this.allowAlloy = allowAlloy;
        this.meltTime= meltTime;
        this.shouldGenerateParts = shouldGenerateParts;
    }


    public String getName() { return name; }
    public int getItemColor() { return itemColor; }

    public int getDurability() { return durability; }
    public float getEfficiency() { return efficiency; }
    public float getModifier() { return modifier; }
    public float getAttackSpeed() { return attackSpeed; }
    public float getDamage() { return damage; }
    public float getWeaponDamage() { return weaponDamage; }
    public float getArc() { return arc; }
    public int getToolAugmentSlots() { return toolAugmentSlots; }
    public float getCritChance() { return critChance; }
    public float getCritDamage() { return critDamage; }
    public ChatFormatting getColor() { return color; }
    public int getTier() { return tier; }
    public boolean allowsAlloy() { return allowAlloy; }
    public boolean shouldGenerateParts() { return shouldGenerateParts; }


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

    /**
     * Converts the material's itemColor to a Vector3f for fog effects
     * @return Vector3f with RGB values normalized to 0-1 range
     */
    public Vector3f getFogVector() {
        return getFogVector(1.0f);
    }

    /**
     * Converts the material's itemColor to a Vector3f for fog effects with brightness adjustment
     * @param brightness Multiplier for the color (0.0 - 1.0)
     * @return Vector3f with RGB values normalized to 0-1 range
     */
    public Vector3f getFogVector(float brightness) {
        // Extract RGB components from itemColor (format: 0xAARRGGBB)
        float r = ((itemColor >> 16) & 0xFF) / 255.0f * brightness;
        float g = ((itemColor >> 8) & 0xFF) / 255.0f * brightness;
        float b = (itemColor & 0xFF) / 255.0f * brightness;

        // Clamp values to 0-1 range
        r = Math.max(0.0f, Math.min(1.0f, r));
        g = Math.max(0.0f, Math.min(1.0f, g));
        b = Math.max(0.0f, Math.min(1.0f, b));

        return new Vector3f(r, g, b);
    }

    @Nullable
    public static AlloyMaterial fromItem(Item item) {
        //Sets vanilla items to their respective materials for repair kit usage
        if (item == Items.COPPER_INGOT) return COPPER;
        if (item == Items.IRON_INGOT) return IRON;
        if (item == Items.GOLD_INGOT) return GOLD;
        if (item == Items.DIAMOND) return DIAMOND;
        if (item == Items.COAL) return COAL;
        if (item == Items.NETHERITE_INGOT) return NETHERITE;

        if (item == Items.RAW_COPPER) return COPPER;
        if (item == Items.RAW_IRON)   return IRON;
        if (item == Items.RAW_GOLD)   return GOLD;


        //Sets modded ingots to their respective materials for repair kit usage
        if (item == ItemStackHelper.getAlloyStack(AlloyMaterial.BRONZE).getItem()) return BRONZE;
        if (item == ItemStackHelper.getAlloyStack(AlloyMaterial.STEEL).getItem()) return STEEL;
        if (item == ItemStackHelper.getAlloyStack(AlloyMaterial.TITANIUM).getItem()) return TITANIUM;
        if (item == ItemStackHelper.getAlloyStack(AlloyMaterial.TIN).getItem()) return TIN;


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

    @Nullable
    public static AlloyMaterial fromFluid(net.minecraft.world.level.material.Fluid fluid) {
        for (AlloyMaterial mat : values()) {
            var source = com.arsenalcrafting.mod.registry.ModFluids.getSource(mat);
            if (source != null && source == fluid) return mat;
        }
        return null;
    }

    public static TagKey<Block> getToolBreakTag(AlloyMaterial material){
        //Set Incorrect for tool to Alloy Material Tier
        int tier = material.getTier();
        switch (tier) {
            case 1 -> {
                return BlockTags.INCORRECT_FOR_STONE_TOOL;
            }
            case 2 -> {
                return BlockTags.INCORRECT_FOR_IRON_TOOL;
            }
            case 3 -> {
                return BlockTags.INCORRECT_FOR_DIAMOND_TOOL;
            }
            case 4 -> {
                return BlockTags.INCORRECT_FOR_NETHERITE_TOOL;
            }
            default -> {
                return BlockTags.INCORRECT_FOR_WOODEN_TOOL;
            }
        }
    }
}