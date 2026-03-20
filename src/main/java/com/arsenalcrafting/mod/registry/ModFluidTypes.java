package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.fluids.TitaniumFluidType;
import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

import java.util.function.Consumer;

public class ModFluidTypes {
    public static final ResourceLocation WATER_STILL_RL = ResourceLocation.fromNamespaceAndPath("minecraft","block/water_still");
    public static final ResourceLocation WATER_FLOWING_RL = ResourceLocation.fromNamespaceAndPath("minecraft", "block/water_flow");
    public static final ResourceLocation TITANIUM_OVERLAY_RL = ResourceLocation.fromNamespaceAndPath("minecraft", "misc/water_overlay");


    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.FLUID_TYPES, ArsenalCrafting.MOD_ID);

    public static final DeferredHolder<FluidType, TitaniumFluidType> TITANIUM_FLUID_TYPE = registerTitanium("titanium_fluid_type",
            FluidType.Properties.create()
                    .density(4500)
                    .viscosity(3000)
                    .temperature(1900)
                    .lightLevel(5)
                    .canSwim(false)
                    .canDrown(true));

    private static DeferredHolder<FluidType, TitaniumFluidType> registerTitanium(String name, FluidType.Properties properties){
        return FLUID_TYPES.register(name, () -> new TitaniumFluidType(WATER_STILL_RL, WATER_FLOWING_RL, TITANIUM_OVERLAY_RL,
                AlloyMaterial.TITANIUM.getItemColor(), AlloyMaterial.TITANIUM.getFogVector(), properties));
    }


    public static void register(IEventBus eventBus){
        FLUID_TYPES.register(eventBus);
    }
}
