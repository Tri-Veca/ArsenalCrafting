package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.AlloySmelterBlock;
import com.arsenalcrafting.mod.blocks.LavaTankBlock;
import com.arsenalcrafting.mod.blocks.MoldBlock;
import com.arsenalcrafting.mod.blocks.ToolkitBlock;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.power.PowerTier;
import com.arsenalcrafting.mod.power.generator.SolarGeneratorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, ArsenalCrafting.MOD_ID);
    public static final Map<ToolComponentType, DeferredHolder<Block, MoldBlock>> MOLD_BLOCKS = new HashMap<>();
    public static final Map<ToolComponentType, DeferredHolder<Block, MoldBlock>> BRONZE_MOLD_BLOCKS = new HashMap<>();


    public static final DeferredHolder<Block, Block> LAVA_TANK = registerBlock(
            "lava_tank",
            () -> new LavaTankBlock(
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_RED)
                            .strength(3.0F, 6.0F)
                            .noOcclusion() // render as non-full cube if you want transparent sides
            )
    );

    public static final DeferredHolder<Block, AlloySmelterBlock> ALLOY_SMELTER =
            registerBlock("alloy_smelter", () -> new AlloySmelterBlock(
                    BlockBehaviour.Properties.of()
                            .strength(3.5f)
                            .requiresCorrectToolForDrops()
                            .noOcclusion()
            ));


    // --- Functional Blocks ---


    public static final DeferredHolder<Block, ToolkitBlock> TOOLKIT = registerBlock("toolkit",
            () -> new ToolkitBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.SMITHING_TABLE).noOcclusion()));

    public static final DeferredHolder<Block, SolarGeneratorBlock> COPPER_SOLAR_PANEL =
            registerBlock("copper_solar_panel",
                    () -> new SolarGeneratorBlock(
                            BlockBehaviour.Properties.of()
                                    .strength(3f)
                                    .requiresCorrectToolForDrops(),
                            PowerTier.COPPER
                    ));

    // --- Multiblock/Casing Blocks ---
    public static final DeferredHolder<Block, Block> VOID_CASING = registerBlock("void_casing",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE).sound(SoundType.GLASS).strength(2.0f)));
    public static final DeferredHolder<Block, Block> IRON_CASING = registerBlock("iron_casing",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f)));



    /*public static final DeferredHolder<Block, MoldBlock> PICKAXE_HEAD_MOLD =
            registerBlock("pickaxe_head_mold", MoldBlock::new);

    public static final DeferredHolder<Block, MoldBlock> TOOL_ROD_MOLD =
            registerBlock("tool_rod_mold", MoldBlock::new);

    public static final DeferredHolder<Block, MoldBlock> CORE_MOLD =
            registerBlock("core_mold", MoldBlock::new);

     */


    public static void registerMolds() {
        System.out.println("[ModBlocks] Automating Mold Registration (Standard & Bronze)...");

        for (ToolComponentType type : ToolComponentType.values()) {
            // --- 1. Standard Molds (Standard Gold Appearance) ---
            String goldId = type.getId() + "_mold";
            DeferredHolder<Block, MoldBlock> goldHolder = BLOCKS.register(goldId, MoldBlock::new);

            ModItems.ITEMS.register(goldId, () -> new BlockItem(goldHolder.get(), new Item.Properties()));
            MOLD_BLOCKS.put(type, goldHolder);

            // --- 2. Bronze Molds (Tinted via Material Color) ---
            String bronzeId = "bronze_" + type.getId() + "_mold";
            DeferredHolder<Block, MoldBlock> bronzeHolder = BLOCKS.register(bronzeId, MoldBlock::new);

            ModItems.ITEMS.register(bronzeId, () -> new BlockItem(bronzeHolder.get(), new Item.Properties()));
            BRONZE_MOLD_BLOCKS.put(type, bronzeHolder);
        }
    }

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
        registerMolds();
    }
}