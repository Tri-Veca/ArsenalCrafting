package com.arsenalcrafting.mod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record RepairKitData(
        String material,
        int storedValue,
        Optional<ItemStack> tool,
        Optional<ItemStack> ingot
) {

    public static final RepairKitData EMPTY =
            new RepairKitData("", 0, Optional.empty(), Optional.empty());

    public static final Codec<RepairKitData> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.optionalFieldOf("material", "")
                            .forGetter(RepairKitData::material),

                    Codec.INT.optionalFieldOf("storedValue", 0)
                            .forGetter(RepairKitData::storedValue),

                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("tool")
                            .forGetter(RepairKitData::tool),

                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("ingot")
                            .forGetter(RepairKitData::ingot)
            ).apply(inst, RepairKitData::new));
}