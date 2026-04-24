package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.ValveFaucetBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class ValveFaucetRenderer implements BlockEntityRenderer<ValveFaucetBlockEntity> {

    // Left/Right position (increase = move right)
    private static final float STREAM_X = 6.2f / 16f;
    // Forward/Back position (increase = move away from faucet)
    private static final float STREAM_Z = 8f / 16f;
    // Top height (increase = stream starts higher)
    private static final float STREAM_Y_TOP = 9.0f / 16f;
    // Stream thickness (increase = wider stream)
    private static final float STREAM_WIDTH = 2.0f / 16f;
    private static final float HALF = STREAM_WIDTH / 2f;
    // Bottom (decrease = stream goes lower, more negative = deeper into block below)
    private static final float STREAM_Y_BOTTOM = -1.0f;

    public ValveFaucetRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(ValveFaucetBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (!be.isPouring() || be.getPouringMaterialName() == null) return;

        AlloyMaterial mat = AlloyMaterial.fromName(be.getPouringMaterialName());
        if (mat == null) return;

        var fluidSource = com.arsenalcrafting.mod.registry.ModFluids.getSource(mat);
        if (fluidSource == null) return;

        FluidStack fluidStack = new FluidStack(fluidSource, 1000);
        var ext = IClientFluidTypeExtensions.of(fluidSource);
        var stillTex = ext.getStillTexture(fluidStack);
        if (stillTex == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTex);

        int color = ext.getTintColor(fluidStack);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.9f;

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float minX = STREAM_X - HALF;
        float maxX = STREAM_X + HALF;
        float minZ = STREAM_Z - HALF;
        float maxZ = STREAM_Z + HALF;
        float top = STREAM_Y_TOP;
        float bottom = STREAM_Y_BOTTOM;

        // North face (minZ) - facing -Z
        buffer.addVertex(pose, minX, bottom, minZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, minX, top, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, maxX, top, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, maxX, bottom, minZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, -1);

        // South face (maxZ) - facing +Z
        buffer.addVertex(pose, maxX, bottom, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, maxX, top, maxZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, minX, top, maxZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, minX, bottom, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, 1);

        // West face (minX) - facing -X
        buffer.addVertex(pose, minX, bottom, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, minX, top, maxZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, minX, top, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, minX, bottom, minZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(-1, 0, 0);

        // East face (maxX) - facing +X
        buffer.addVertex(pose, maxX, bottom, minZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, maxX, top, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, maxX, top, maxZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, maxX, bottom, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(1, 0, 0);

        // Bottom face (bottom) - facing -Y
        buffer.addVertex(pose, minX, bottom, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, -1, 0);
        buffer.addVertex(pose, maxX, bottom, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, -1, 0);
        buffer.addVertex(pose, maxX, bottom, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, -1, 0);
        buffer.addVertex(pose, minX, bottom, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, -1, 0);

        // Top face (top) - facing +Y, reversed winding order
        buffer.addVertex(pose, minX, top, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, maxX, top, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, maxX, top, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, minX, top, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 1, 0);
    }
}