package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.*;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.power.PowerTier;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
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
    public static final Map<AlloyMaterial, DeferredHolder<Block, LiquidBlock>> ALLOY_FLUID_BLOCKS =
            new HashMap<>();





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

    // ── CASINGS ───────────────────────────────────────────────────────────────

    public static final DeferredHolder<Block, Block> VOID_CASING = registerBlock("void_casing",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.END_STONE)
                    .sound(SoundType.GLASS).strength(2.0f)));

    public static final DeferredHolder<Block, Block> IRON_CASING = registerBlock("iron_casing",
            () -> new Block(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(5.0f)));

    // ── CABLE ─────────────────────────────────────────────────────────────────
    // No BlockEntity — pure graph node. One universal type.

    public static final DeferredHolder<Block, CableBlock> POWER_CABLE =
            registerBlock("power_cable",
                    () -> new CableBlock(BlockBehaviour.Properties.of()
                            .strength(1.5f)
                            .requiresCorrectToolForDrops()
                            .sound(SoundType.METAL)
                            .noOcclusion()));

    // ── POWER BANKS ───────────────────────────────────────────────────────────
    // Tiered storage. Capacity and transfer rate from PowerTier.bankCapacity().

    public static final DeferredHolder<Block, PowerBankBlock> COPPER_POWER_BANK =
            registerBlock("copper_power_bank",
                    () -> new PowerBankBlock(generatorProps().mapColor(MapColor.COLOR_ORANGE), PowerTier.COPPER));

    public static final DeferredHolder<Block, PowerBankBlock> IRON_POWER_BANK =
            registerBlock("iron_power_bank",
                    () -> new PowerBankBlock(generatorProps().mapColor(MapColor.METAL), PowerTier.IRON));

    public static final DeferredHolder<Block, PowerBankBlock> VOID_POWER_BANK =
            registerBlock("void_power_bank",
                    () -> new PowerBankBlock(generatorProps().mapColor(MapColor.COLOR_BLACK), PowerTier.TITANIUM));


    // Burns coal/charcoal/coal blocks. Tier scales output rate.

    private static BlockBehaviour.Properties generatorProps() {
        return BlockBehaviour.Properties.of()
                .strength(3.5f)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    public static final DeferredHolder<Block, FossilGeneratorBlock> COPPER_FOSSIL_GENERATOR =
            registerBlock("copper_fossil_generator",
                    () -> new FossilGeneratorBlock(generatorProps().mapColor(MapColor.COLOR_ORANGE), PowerTier.COPPER));

    public static final DeferredHolder<Block, FossilGeneratorBlock> IRON_FOSSIL_GENERATOR =
            registerBlock("iron_fossil_generator",
                    () -> new FossilGeneratorBlock(generatorProps().mapColor(MapColor.METAL), PowerTier.IRON));

    public static final DeferredHolder<Block, FossilGeneratorBlock> VOID_FOSSIL_GENERATOR =
            registerBlock("void_fossil_generator",
                    () -> new FossilGeneratorBlock(generatorProps().mapColor(MapColor.COLOR_BLACK), PowerTier.TITANIUM));

    // ── GENERATORS: SOLAR PANEL ───────────────────────────────────────────────
    // Generates FE during daylight. No fuel. Stops in rain/thunder.

    public static final DeferredHolder<Block, SolarGeneratorBlock> COPPER_SOLAR_PANEL =
            registerBlock("copper_solar_panel",
                    () -> new SolarGeneratorBlock(generatorProps().mapColor(MapColor.COLOR_ORANGE), PowerTier.COPPER));

    public static final DeferredHolder<Block, SolarGeneratorBlock> IRON_SOLAR_PANEL =
            registerBlock("iron_solar_panel",
                    () -> new SolarGeneratorBlock(generatorProps().mapColor(MapColor.METAL), PowerTier.IRON));

    public static final DeferredHolder<Block, SolarGeneratorBlock> VOID_SOLAR_PANEL =
            registerBlock("void_solar_panel",
                    () -> new SolarGeneratorBlock(generatorProps().mapColor(MapColor.COLOR_BLACK), PowerTier.TITANIUM));

    // ── GENERATORS: BIO MASHER ────────────────────────────────────────────────
    // Burns seeds, saplings, crops, and other organic items.

    public static final DeferredHolder<Block, BioGeneratorBlock> COPPER_BIO_GENERATOR =
            registerBlock("copper_bio_generator",
                    () -> new BioGeneratorBlock(generatorProps().mapColor(MapColor.COLOR_ORANGE), PowerTier.COPPER));

    public static final DeferredHolder<Block, BioGeneratorBlock> IRON_BIO_GENERATOR =
            registerBlock("iron_bio_generator",
                    () -> new BioGeneratorBlock(generatorProps().mapColor(MapColor.METAL), PowerTier.IRON));

    public static final DeferredHolder<Block, BioGeneratorBlock> VOID_BIO_GENERATOR =
            registerBlock("void_bio_generator",
                    () -> new BioGeneratorBlock(generatorProps().mapColor(MapColor.COLOR_BLACK), PowerTier.TITANIUM));



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



    public static void registerAlloyFluidBlocks() {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String id = material.getName().toLowerCase() + "_fluid_block";
            final AlloyMaterial mat = material;

            DeferredHolder<Block, LiquidBlock> holder = BLOCKS.register(id,
                    () -> new LiquidBlock(
                            ModFluids.getSource(mat),
                            BlockBehaviour.Properties.of()
                                    .noCollission()
                                    .strength(100f)
                                    .noLootTable()
                                    .liquid()
                                    .replaceable()
                                    .pushReaction(PushReaction.DESTROY)  // Add this
                                    .mapColor(MapColor.COLOR_GRAY)       // Add this
                                    .lightLevel(state -> 5)              // Add this - makes it glow
                    )
            );
            ALLOY_FLUID_BLOCKS.put(material, holder);
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
        registerAlloyFluidBlocks();
    }
}