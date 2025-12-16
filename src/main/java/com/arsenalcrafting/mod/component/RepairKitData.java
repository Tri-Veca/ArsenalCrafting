package com.arsenalcrafting.mod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public record RepairKitData(
        String material,              // Current active material
        int storedValue,              // Current stored repair value
        Optional<ItemStack> tool,     // Currently stored tool
        Optional<ItemStack> ingot,    // Currently stored ingot
        boolean materialLocked        // NEW: Whether material is locked or can be changed
) {

    public static final RepairKitData EMPTY =
            new RepairKitData("", 0, Optional.empty(), Optional.empty(), false);

    public static final Codec<RepairKitData> CODEC =
            RecordCodecBuilder.create(inst -> inst.group(
                    Codec.STRING.optionalFieldOf("material", "")
                            .forGetter(RepairKitData::material),

                    Codec.INT.optionalFieldOf("storedValue", 0)
                            .forGetter(RepairKitData::storedValue),

                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("tool")
                            .forGetter(RepairKitData::tool),

                    ItemStack.OPTIONAL_CODEC.optionalFieldOf("ingot")
                            .forGetter(RepairKitData::ingot),

                    Codec.BOOL.optionalFieldOf("materialLocked", false)
                            .forGetter(RepairKitData::materialLocked)
            ).apply(inst, RepairKitData::new));

    // Helper methods
    public RepairKitData withMaterial(String newMaterial) {
        return new RepairKitData(newMaterial, storedValue, tool, ingot, materialLocked);
    }

    public RepairKitData withStoredValue(int newValue) {
        return new RepairKitData(material, newValue, tool, ingot, materialLocked);
    }

    public RepairKitData withMaterialLocked(boolean locked) {
        return new RepairKitData(material, storedValue, tool, ingot, locked);
    }


    public boolean canAcceptMaterial(String newMaterial) {
        // Can accept if:
        // 1. No material is set yet
        // 2. Material is not locked AND new material matches current material
        // 3. Material is locked but we're trying to clear it (empty string)
        if (material.isEmpty()) return true;
        if (!materialLocked && material.equals(newMaterial)) return true;
        if (materialLocked && newMaterial.isEmpty()) return true; // Allow clearing
        return false;
    }


}