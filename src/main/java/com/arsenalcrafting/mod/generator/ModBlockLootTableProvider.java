package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

import java.util.Set;

public class ModBlockLootTableProvider extends BlockLootSubProvider {
    protected ModBlockLootTableProvider(HolderLookup.Provider registries) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), registries);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.LAVA_TANK.get());
        dropSelf(ModBlocks.TOOLKIT.get());
        dropSelf(ModBlocks.IRON_CASING.get());
        dropSelf(ModBlocks.VOID_CASING.get());
        dropSelf(ModBlocks.ALLOY_SMELTER.get());

        // Fossil generators
        dropSelf(ModBlocks.COPPER_FOSSIL_GENERATOR.get());
        dropSelf(ModBlocks.IRON_FOSSIL_GENERATOR.get());
        dropSelf(ModBlocks.VOID_FOSSIL_GENERATOR.get());

        // Solar panels
        dropSelf(ModBlocks.COPPER_SOLAR_PANEL.get());
        dropSelf(ModBlocks.IRON_SOLAR_PANEL.get());
        dropSelf(ModBlocks.VOID_SOLAR_PANEL.get());

        // Bio generators
        dropSelf(ModBlocks.COPPER_BIO_GENERATOR.get());
        dropSelf(ModBlocks.IRON_BIO_GENERATOR.get());
        dropSelf(ModBlocks.VOID_BIO_GENERATOR.get());

        // Power

        dropSelf(ModBlocks.POWER_CABLE.get());
        dropSelf(ModBlocks.COPPER_POWER_BANK.get());
        dropSelf(ModBlocks.IRON_POWER_BANK.get());
        dropSelf(ModBlocks.VOID_POWER_BANK.get());

        ModBlocks.MOLD_BLOCKS.values().forEach(holder -> dropSelf(holder.get()));
        ModBlocks.BRONZE_MOLD_BLOCKS.values().forEach(holder -> dropSelf(holder.get()));
        /*dropSelf(ModBlocks.PICKAXE_HEAD_MOLD.get());
        dropSelf(ModBlocks.TOOL_ROD_MOLD.get());
        dropSelf(ModBlocks.CORE_MOLD.get());

         */

        // dropSelf(ModBlocks.MAGIC_BLOCK.get());

//        add(ModBlocks.BISMUTH_ORE.get(),
//                block -> createOreDrop(ModBlocks.BISMUTH_ORE.get(), ModItems.RAW_BISMUTH.get()));
//        add(ModBlocks.BISMUTH_DEEPSLATE_ORE.get(),
//                block -> createMultipleOreDrops(ModBlocks.BISMUTH_DEEPSLATE_ORE.get(), ModItems.RAW_BISMUTH.get(), 2, 5));

    }

    protected LootTable.Builder createMultipleOreDrops(Block pBlock, Item item, float minDrops, float maxDrops) {
        HolderLookup.RegistryLookup<Enchantment> registrylookup = this.registries.lookupOrThrow(Registries.ENCHANTMENT);
        return this.createSilkTouchDispatchTable(pBlock,
                this.applyExplosionDecay(pBlock, LootItem.lootTableItem(item)
                        .apply(SetItemCountFunction.setCount(UniformGenerator.between(minDrops, maxDrops)))
                        .apply(ApplyBonusCount.addOreBonusCount(registrylookup.getOrThrow(Enchantments.FORTUNE)))));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return ModBlocks.BLOCKS.getEntries().stream().map(Holder::value)::iterator;
    }
}