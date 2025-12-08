package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.LavaTankBlock;
import com.arsenalcrafting.mod.blocks.ToolkitBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ArsenalCrafting.MOD_ID);


    public static final DeferredHolder<Block, Block> LAVA_TANK = registerBlock(
            "lava_tank",
            () -> new LavaTankBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .strength(3.0F, 6.0F)
                            .noOcclusion() // render as non-full cube if you want transparent sides
            )
    );

    // --- Functional Blocks ---
    public static final DeferredHolder<Block, Block> ALLOY_SMELTER_CORE = registerBlock("alloy_smelter_core",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).sound(SoundType.ANVIL).noOcclusion()));

    public static final DeferredHolder<Block, ToolkitBlock> TOOLKIT = registerBlock("toolkit",
            () -> new ToolkitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE).noOcclusion()));

    // --- Multiblock/Casing Blocks ---
    public static final DeferredHolder<Block, Block> VOID_CASING = registerBlock("void_casing",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).sound(SoundType.GLASS).strength(2.0f)));
    public static final DeferredHolder<Block, Block> IRON_CASING = registerBlock("iron_casing",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f)));

    private static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Supplier<T> block) {
        // Now using DeferredHolder<Block, T>
        DeferredHolder<Block, T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> DeferredHolder<Item, ? extends Item> registerBlockItem(String name, DeferredHolder<Block, T> block) {
        // The return type is DeferredHolder<Item, Item> to register the BlockItem
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}