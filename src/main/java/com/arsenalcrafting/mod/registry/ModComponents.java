package com.arsenalcrafting.mod.registry;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.mojang.serialization.Codec;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;


public class ModComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, ArsenalCrafting.MOD_ID);

    // Component for storing the specific part materials used to build a modular tool
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<ToolAssemblyData>> TOOL_ASSEMBLY_DATA = COMPONENTS.register("tool_assembly_data",
            () -> DataComponentType.<ToolAssemblyData>builder()
                    .persistent(ToolAssemblyData.CODEC)
                    .build());

    /**
     * Data component for storing the ON/OFF state of the Auto Smelt augment.
     * It uses a simple Boolean, where the presence of the component means the augment is active.
     */
    /*
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> AUTO_SMELT_ENABLED = COMPONENTS.register("auto_smelt_enabled",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .cacheEncoding() // Default value when reading NBT where it wasn't present
                    .build());

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> ORE_MINER_ENABLED = COMPONENTS.register("ore_miner_enabled",
            () -> DataComponentType.<Boolean>builder()
                    .persistent(Codec.BOOL)
                    .networkSynchronized(ByteBufCodecs.BOOL)
                    .cacheEncoding() // Default value when reading NBT where it wasn't present
                    .build());


    // Fortune: on/off flag
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> FORTUNE_ENABLED =
            COMPONENTS.register("fortune_enabled",
                    () -> DataComponentType.<Boolean>builder()
                            .persistent(Codec.BOOL)
                            .networkSynchronized(ByteBufCodecs.BOOL)
                            .cacheEncoding()
                            .build());

    // Fortune: tier (1–3)
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FORTUNE_TIER =
            COMPONENTS.register("fortune_tier",
                    () -> DataComponentType.<Integer>builder()
                            .persistent(Codec.INT)
                            .networkSynchronized(ByteBufCodecs.VAR_INT)
                            .cacheEncoding()
                            .build());

     */

    static {
        // Register dynamic components FIRST
        DynamicComponents.registerAll(COMPONENTS);
    }


        public static void register (IEventBus eventBus){
            COMPONENTS.register(eventBus);
        }
    }
