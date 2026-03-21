package com.arsenalcrafting.mod.power;

import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModItems;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

/**
 * Defines what items count as fuel for each generator type.
 * Tag-based so datapacks/other mods can extend without code changes.
 *
 * Fossil: coal-like items (coal, charcoal, coal blocks)
 * Bio:    plant/seed items (seeds, saplings, leaves, c:plants)
 */
public class FuelRegistry {

    // ── FOSSIL FUEL ───────────────────────────────────────────────────────────
    // Returns burn ticks for fossil fuel items. 0 = not valid fuel.

    public static int getFossilBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        // Coal block = 16000t, coal/charcoal = 1600t
        if (stack.is(Items.COAL_BLOCK))     return 1440;
        if (stack.is(Items.COAL))           return 160;
        if (stack.is(ModItems.ALLOY_BUCKETS.get(AlloyMaterial.COAL))) return 640;
        if (stack.is(Items.CHARCOAL))       return 160;
        if (stack.is(net.minecraft.world.item.Items.BLAZE_ROD)) return 1800;

        // Tag-based: c:coals, c:coal, minecraft:coals
        if (stack.is(net.minecraft.tags.ItemTags.COALS)) return 160;

        return 0;
    }

    public static boolean isFossilFuel(ItemStack stack) {
        return getFossilBurnTime(stack) > 0;
    }

    // ── BIO FUEL ──────────────────────────────────────────────────────────────
    // Returns burn ticks for bio fuel items. 0 = not valid fuel.

    public static int getBioBurnTime(ItemStack stack) {
        if (stack.isEmpty()) return 0;

        if (stack.is(ItemTags.SAPLINGS))    return 800;
        if (stack.is(ItemTags.LEAVES))      return 400;
        if (stack.is(ItemTags.FLOWERS))     return 600;

        // Seeds
        if (stack.is(Items.WHEAT_SEEDS))    return 200;
        if (stack.is(Items.MELON_SEEDS))    return 200;
        if (stack.is(Items.PUMPKIN_SEEDS))  return 200;
        if (stack.is(Items.BEETROOT_SEEDS)) return 200;
        if (stack.is(Items.WHEAT))          return 300;
        if (stack.is(Items.SUGAR_CANE))     return 300;
        if (stack.is(Items.CACTUS))         return 300;
        if (stack.is(Items.KELP))           return 200;
        if (stack.is(Items.DRIED_KELP))     return 400;
        if (stack.is(Items.DRIED_KELP_BLOCK)) return 4000;

        // NeoForge tags
        if (stack.is(Tags.Items.SEEDS))     return 200;
        if (stack.is(Tags.Items.CROPS))     return 300;

        return 0;
    } //TODO: Make this also balanced like i did with the fossil fuels

    public static boolean isBioFuel(ItemStack stack) {
        return getBioBurnTime(stack) > 0;
    }
}