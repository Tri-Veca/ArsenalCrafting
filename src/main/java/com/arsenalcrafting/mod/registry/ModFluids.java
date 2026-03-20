package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.fluids.TitaniumFluidType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public class ModFluids {
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, ArsenalCrafting.MOD_ID);

    public static DeferredHolder<Fluid, FlowingFluid> TITANIUM_SOURCE =
            FLUIDS.register("titanium_source", () -> new BaseFlowingFluid.Source(ModFluids.TITANIUM_FLUID_PROPERTIES));

    public static DeferredHolder<Fluid, FlowingFluid> TITANIUM_FLOWING =
            FLUIDS.register("titanium_flowing", () -> new BaseFlowingFluid.Flowing(ModFluids.TITANIUM_FLUID_PROPERTIES));


    public static final BaseFlowingFluid.Properties TITANIUM_FLUID_PROPERTIES = new BaseFlowingFluid.Properties(
            ModFluidTypes.TITANIUM_FLUID_TYPE, TITANIUM_SOURCE, TITANIUM_FLOWING)
            .slopeFindDistance(2).levelDecreasePerBlock(2)
            .block(ModBlocks.TITANIUM_FLUID_BLOCK)
            .bucket(ModItems.TITANIUM_BUCKET);


    public static void register(IEventBus eventBus){
        FLUIDS.register(eventBus);
    }
}
