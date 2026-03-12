package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.arsenalcrafting.mod.blocks.entity.ToolkitBlockEntity;
import com.arsenalcrafting.mod.power.PowerTier;
import com.arsenalcrafting.mod.power.generator.GeneratorBlockEntity;
import com.arsenalcrafting.mod.power.generator.GeneratorType;
import com.arsenalcrafting.mod.power.generator.SolarGeneratorBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder; // Changed
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;

import static com.arsenalcrafting.mod.registry.ModBlocks.BLOCKS;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ArsenalCrafting.MOD_ID);

    // Alloy Smelter (Multiblock core)


    // Toolkit (Modular crafting station)
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ToolkitBlockEntity>> TOOLKIT =
            BLOCK_ENTITIES.register("toolkit", () ->
                    BlockEntityType.Builder.of(ToolkitBlockEntity::new,
                            ModBlocks.TOOLKIT.get()).build(null));

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LavaTankBlockEntity>> LAVA_TANK =
            BLOCK_ENTITIES.register("lava_tank",
                    () -> BlockEntityType.Builder.of(
                            LavaTankBlockEntity::new,
                            ModBlocks.LAVA_TANK.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AlloySmelterBlockEntity>> ALLOY_SMELTER =
            BLOCK_ENTITIES.register("alloy_smelter", () ->
                    BlockEntityType.Builder.of(AlloySmelterBlockEntity::new,
                            ModBlocks.ALLOY_SMELTER.get()).build(null));


    // Power Generators (Solar, Bio, Fossil across tiers)
    // ✅ Generator safe scaffolding
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR =
            BLOCK_ENTITIES.register("generator", () ->
                    BlockEntityType.Builder.of(
                            GeneratorBlockEntity::new,
                            ModBlocks.COPPER_SOLAR_PANEL.get() // raw Block object
                    ).build(null)
            );




    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}