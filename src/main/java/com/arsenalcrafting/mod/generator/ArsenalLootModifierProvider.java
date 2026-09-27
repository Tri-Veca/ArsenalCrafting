package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.loot.ChromiumOreDropModifier;
import com.arsenalcrafting.mod.loot.GravelLootModifier;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.data.GlobalLootModifierProvider;
import net.neoforged.neoforge.common.loot.LootTableIdCondition;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Data-generates ALL of the mod's global loot modifiers — gravel's raw tin chance and
 * every chromium-drop tier. This must be the ONLY GlobalLootModifierProvider in the mod:
 * NeoForge's shared data/neoforge/loot_modifiers/global_loot_modifiers.json is a single
 * aggregated file, and two separate writers (or a writer plus a hand-written copy) will
 * collide. If gravel previously had a hand-written global_loot_modifiers.json and/or a
 * hand-written gravel_raw_tin.json instance file, DELETE both — this provider regenerates
 * them, with the chromium entries added alongside.
 */
public class ArsenalLootModifierProvider extends GlobalLootModifierProvider {

    // Common ores: coal, copper — most frequently mined, so kept rare per-instance.
    private static final float COMMON_CHANCE = 0.03f;
    private static final List<String> COMMON_ORES = List.of(
            "coal_ore", "deepslate_coal_ore",
            "copper_ore", "deepslate_copper_ore"
    );

    // Mid-tier ores: iron, gold, redstone, lapis, nether gold/quartz.
    private static final float MID_CHANCE = 0.06f;
    private static final List<String> MID_ORES = List.of(
            "iron_ore", "deepslate_iron_ore",
            "gold_ore", "deepslate_gold_ore",
            "redstone_ore", "deepslate_redstone_ore",
            "lapis_ore", "deepslate_lapis_ore",
            "nether_gold_ore", "nether_quartz_ore"
    );

    // Rare ores: diamond, emerald, ancient debris — highest per-instance chance.
    private static final float RARE_CHANCE = 0.12f;
    private static final List<String> RARE_ORES = List.of(
            "diamond_ore", "deepslate_diamond_ore",
            "emerald_ore", "deepslate_emerald_ore",
            "ancient_debris"
    );

    public ArsenalLootModifierProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider) {
        super(output, lookupProvider, ArsenalCrafting.MOD_ID);
    }

    @Override
    protected void start() {
        // Gravel — folded in here so there's only one writer of the shared global list.
        // Empty conditions array matches GravelLootModifier's actual behavior: it checks
        // the block directly inside doApply() rather than filtering via LootItemCondition.
        add("gravel_raw_tin", new GravelLootModifier(new LootItemCondition[0]));

        addTier(COMMON_ORES, COMMON_CHANCE);
        addTier(MID_ORES, MID_CHANCE);
        addTier(RARE_ORES, RARE_CHANCE);
    }

    private void addTier(List<String> oreNames, float chance) {
        for (String oreName : oreNames) {
            ResourceLocation lootTableId = ResourceLocation.withDefaultNamespace("blocks/" + oreName);
            String modifierName = "chromium_from_" + oreName;

            add(modifierName, new ChromiumOreDropModifier(
                    new LootItemCondition[]{
                            LootTableIdCondition.builder(lootTableId).build()
                    },
                    chance
            ));
        }
    }
}