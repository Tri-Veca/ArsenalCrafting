package com.arsenalcrafting.mod.generator;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.data.BlockTagsProvider;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

public class ArsenalBlockTagProvider extends BlockTagsProvider {
    public ArsenalBlockTagProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, ArsenalCrafting.MOD_ID, existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // AUTOMATION: Add every mold to the pickaxe-mineable tag
        var mineableTag = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        var toolTag = tag(BlockTags.NEEDS_STONE_TOOL);

        ModBlocks.MOLD_BLOCKS.values().forEach(holder -> {
            mineableTag.add(holder.get());
            toolTag.add(holder.get());
        });

        ModBlocks.BRONZE_MOLD_BLOCKS.values().forEach(holder -> {
            mineableTag.add(holder.get());
            toolTag.add(holder.get());
        });

        mineableTag.add(ModBlocks.RUTILE_BUDDING.get());
        mineableTag.add(ModBlocks.SMALL_RUTILE_BUD.get());
        mineableTag.add(ModBlocks.MEDIUM_RUTILE_BUD.get());
        mineableTag.add(ModBlocks.LARGE_RUTILE_BUD.get());
        mineableTag.add(ModBlocks.RUTILE_CLUSTER.get());
    }
}