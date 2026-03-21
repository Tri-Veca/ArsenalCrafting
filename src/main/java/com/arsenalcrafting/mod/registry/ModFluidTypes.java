package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.fluids.AlloyFluidType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.joml.Vector3f;

import java.util.EnumMap;
import java.util.Map;

public class ModFluidTypes {

    // Shared base textures — reuse water still/flow, tinted per material
    public static final ResourceLocation STILL_RL =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "block/molten_still");
    public static final ResourceLocation FLOWING_RL =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "block/molten_flow");
    public static final ResourceLocation OVERLAY_RL = null;


    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, ArsenalCrafting.MOD_ID);



    public static final Map<AlloyMaterial, DeferredHolder<FluidType, AlloyFluidType>> ALLOY_FLUID_TYPES =
            new EnumMap<>(AlloyMaterial.class);

    static {
        for (AlloyMaterial material : AlloyMaterial.values()) {
            String id = material.getName().toLowerCase() + "_fluid_type";

            // Derive fog color from material ARGB item color
            int argb = material.getItemColor();
            float r = ((argb >> 16) & 0xFF) / 255.0f;
            float g = ((argb >>  8) & 0xFF) / 255.0f;
            float b = ( argb        & 0xFF) / 255.0f;
            Vector3f fogColor = new Vector3f(r, g, b);

            // Capture for lambda
            final AlloyMaterial mat      = material;
            final int           tint     = argb;
            final Vector3f      fog      = fogColor;

            FluidType.Properties props = FluidType.Properties.create()
                    .density(3000)
                    .viscosity(2500)
                    .temperature(1300)
                    .lightLevel(5)
                    .canSwim(false)
                    .canDrown(true);

            DeferredHolder<FluidType, AlloyFluidType> holder = FLUID_TYPES.register(id,
                    () -> new AlloyFluidType(STILL_RL, FLOWING_RL, OVERLAY_RL, tint, fog, props));

            ALLOY_FLUID_TYPES.put(material, holder);
        }
    }



    /** Get the registered AlloyFluidType for a material. */
    public static AlloyFluidType get(AlloyMaterial material) {
        DeferredHolder<FluidType, AlloyFluidType> holder = ALLOY_FLUID_TYPES.get(material);
        return holder != null ? holder.get() : null;
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
    }
}