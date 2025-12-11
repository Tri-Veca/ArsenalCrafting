package com.arsenalcrafting.mod.component;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ToolAssemblyData(String headMaterial, String rodMaterial, String coreMaterial) {

    public static final Codec<ToolAssemblyData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("head_material").forGetter(ToolAssemblyData::safeHeadMaterial),
            Codec.STRING.fieldOf("rod_material").forGetter(ToolAssemblyData::safeRodMaterial),
            Codec.STRING.fieldOf("core_material").forGetter(ToolAssemblyData::safeCoreMaterial)
    ).apply(instance, ToolAssemblyData::new));

    public static final ToolAssemblyData EMPTY = new ToolAssemblyData("", "", "");

    // Compact constructor that forces non-null strings
    public ToolAssemblyData {
        headMaterial = headMaterial != null ? headMaterial : "";
        rodMaterial = rodMaterial != null ? rodMaterial : "";
        coreMaterial = coreMaterial != null ? coreMaterial : "";
    }

    // SAFE getter methods that never return null
    public String safeHeadMaterial() {
        return headMaterial != null ? headMaterial : "";
    }

    public String safeRodMaterial() {
        return rodMaterial != null ? rodMaterial : "";
    }

    public String safeCoreMaterial() {
        return coreMaterial != null ? coreMaterial : "";
    }

    // Original getters (keep for compatibility)
    @Override
    public String headMaterial() {
        return safeHeadMaterial();
    }

    @Override
    public String rodMaterial() {
        return safeRodMaterial();
    }

    @Override
    public String coreMaterial() {
        return safeCoreMaterial();
    }

    // BULLETPROOF isEmpty() - handles ANY null scenario
    public boolean isEmpty() {
        // Check each field with null safety
        boolean headEmpty = (headMaterial == null) || headMaterial.isEmpty();
        boolean rodEmpty = (rodMaterial == null) || rodMaterial.isEmpty();
        boolean coreEmpty = (coreMaterial == null) || coreMaterial.isEmpty();

        return headEmpty && rodEmpty && coreEmpty;
    }

    // Debug method
    @Override
    public String toString() {
        return String.format("ToolAssemblyData{head='%s', rod='%s', core='%s'}",
                safeHeadMaterial(), safeRodMaterial(), safeCoreMaterial());
    }
}