package com.arsenalcrafting.mod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record RepairKitData(
        String material,           // What material is currently stored in the kit
        int storedValue,           // Current stored repair value (0-128)
        Optional<ItemStack> tool,  // Tool sitting in the kit slot
        Optional<ItemStack> ingot  // Ingot sitting in the kit slot
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

    public RepairKitData withMaterial(String newMaterial) {
        return new RepairKitData(newMaterial, storedValue, tool, ingot);
    }

    public RepairKitData withStoredValue(int newValue) {
        // When value hits 0, clear material too — kit is fully empty
        String newMaterial = (newValue <= 0) ? "" : material;
        return new RepairKitData(newMaterial, newValue, tool, ingot);
    }
}