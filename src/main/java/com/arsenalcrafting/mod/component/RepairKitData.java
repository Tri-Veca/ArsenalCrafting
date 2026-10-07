package com.arsenalcrafting.mod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Persistent state of a Repair Kit — saved on the item as a data component.
 *
 * material     = alloy name as a plain String ("" = nothing stored)
 * storedValue  = repair currency, 0–128 (ingots give 2 each)
 * tool / ingot = snapshots of what's in the kit's two GUI slots
 *
 * Read/written through RepairKitItem's static helpers.
 */
public record RepairKitData(
        String material,           // alloy name, plain String ("" = empty)
        int storedValue,           // stored repair value, 0..128
        Optional<ItemStack> tool,  // tool snapshot — mirrors GUI slot 0
        Optional<ItemStack> ingot  // ingot snapshot — mirrors GUI slot 1
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

    /** Copy with a new material name. */
    public RepairKitData withMaterial(String newMaterial) {
        return new RepairKitData(newMaterial, storedValue, tool, ingot);
    }

    /** Copy with a new value. Clearing to 0 also clears material — fully empty kit. */
    public RepairKitData withStoredValue(int newValue) {
        String newMaterial = (newValue <= 0) ? "" : material;
        return new RepairKitData(newMaterial, newValue, tool, ingot);
    }
}