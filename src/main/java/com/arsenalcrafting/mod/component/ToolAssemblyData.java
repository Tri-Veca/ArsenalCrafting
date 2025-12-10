package com.arsenalcrafting.mod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

// This record holds the structured data (head and rod material names)
// that was previously stored directly in the ItemStack NBT tag.
public record ToolAssemblyData(String headMaterial, String rodMaterial, String coreMaterial) {

    // Codec for serializing and deserializing this data component.
    public static final Codec<ToolAssemblyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("head_material").forGetter(ToolAssemblyData::headMaterial),
            Codec.STRING.fieldOf("rod_material").forGetter(ToolAssemblyData::rodMaterial),
            Codec.STRING.fieldOf("core").forGetter(ToolAssemblyData::coreMaterial)
    ).apply(instance, ToolAssemblyData::new));

    // Simple helper for default/empty data
    public static final ToolAssemblyData EMPTY = new ToolAssemblyData("", "", "");

    public boolean isEmpty() {
        return headMaterial.isEmpty() && rodMaterial.isEmpty();
    }
}