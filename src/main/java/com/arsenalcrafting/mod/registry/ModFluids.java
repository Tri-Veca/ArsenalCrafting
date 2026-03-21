package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

import static net.minecraft.core.registries.BuiltInRegistries.FLUID;

public class ModFluids {

    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(FLUID, ArsenalCrafting.MOD_ID);

    public static final Map<AlloyMaterial, DeferredHolder<Fluid, FlowingFluid>> SOURCE  = new EnumMap<>(AlloyMaterial.class);
    public static final Map<AlloyMaterial, DeferredHolder<Fluid, FlowingFluid>> FLOWING = new EnumMap<>(AlloyMaterial.class);

    // Fluid properties — built after source/flowing holders exist
    static final Map<AlloyMaterial, BaseFlowingFluid.Properties> FLUID_PROPS = new EnumMap<>(AlloyMaterial.class);




    static {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String name = material.getName().toLowerCase();

            // Register source and flowing — Properties are lazily resolved via supplier
            DeferredHolder<Fluid, FlowingFluid> sourceHolder =
                    FLUIDS.register(name + "_source",
                            () -> new BaseFlowingFluid.Source(FLUID_PROPS.get(material)));

            DeferredHolder<Fluid, FlowingFluid> flowingHolder =
                    FLUIDS.register(name + "_flowing",
                            () -> new BaseFlowingFluid.Flowing(FLUID_PROPS.get(material)));

            SOURCE.put(material, sourceHolder);
            FLOWING.put(material, flowingHolder);

            // Properties — fluid block and bucket registered in ModBlocks/ModItems
            // and referenced back here via suppliers to avoid circular init
            BaseFlowingFluid.Properties props = new BaseFlowingFluid.Properties(
                    () -> ModFluidTypes.get(material),
                    sourceHolder,
                    flowingHolder
            )
                    .slopeFindDistance(2)
                    .levelDecreasePerBlock(2)
                    .block(() -> (LiquidBlock) ModBlocks.ALLOY_FLUID_BLOCKS.get(material).get())
                    .bucket(() -> ModItems.ALLOY_BUCKETS.get(material).get());

            FLUID_PROPS.put(material, props);
        }
    }

    public static FlowingFluid getSource(AlloyMaterial material) {
        DeferredHolder<Fluid, FlowingFluid> h = SOURCE.get(material);
        return h != null ? h.get() : null;
    }

    public static FlowingFluid getFlowing(AlloyMaterial material) {
        DeferredHolder<Fluid, FlowingFluid> h = FLOWING.get(material);
        return h != null ? h.get() : null;
    }

    public static void register(IEventBus eventBus) {
        FLUIDS.register(eventBus);
    }
}