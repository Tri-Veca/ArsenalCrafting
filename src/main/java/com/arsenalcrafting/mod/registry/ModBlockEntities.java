package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.arsenalcrafting.mod.blocks.entity.ToolkitBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder; // Changed
import net.neoforged.neoforge.registries.DeferredRegister;

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

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}