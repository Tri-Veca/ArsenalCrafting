package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.ValveFaucetBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class ValveFaucetRenderer implements BlockEntityRenderer<ValveFaucetBlockEntity> {

    // Pipe tip position in the model — stream starts here
    // Based on the valve_faucet model: pipe tip is at Z=11, centered at X=8, Y=8
    private static final float STREAM_X     = 6.5f  / 16f;
    private static final float STREAM_Z     = 8f / 16f;
    private static final float STREAM_Y_TOP = 10f  / 16f; // bottom of pipe tip
    private static final float STREAM_WIDTH = 3f  / 16f; // thin stream

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
        float g = ((color >>  8) & 0xFF) / 255f;
        float b = ( color        & 0xFF) / 255f;
        float a = 0.9f;

        renderStream(poseStack, bufferSource, r, g, b, a, sprite, packedLight);

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float half = STREAM_WIDTH / 2f;

        // Stream goes from pipe tip all the way down to the block below (Y = -1.0 relative)
        float streamTop    = STREAM_Y_TOP;
        float streamBottom = -1.0f; // reaches into the block below

        var buffer = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        // North face of stream
        buffer.addVertex(pose, STREAM_X - half, streamTop,    STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, STREAM_X - half, streamBottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, STREAM_X + half, streamBottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, STREAM_X + half, streamTop,    STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, -1);

        // South face
        buffer.addVertex(pose, STREAM_X + half, streamTop,    STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, STREAM_X + half, streamBottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, STREAM_X - half, streamBottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, STREAM_X - half, streamTop,    STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, 1);

        // West face
        buffer.addVertex(pose, STREAM_X, streamTop,    STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, STREAM_X, streamBottom, STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, STREAM_X, streamBottom, STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, STREAM_X, streamTop,    STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(-1, 0, 0);

        // East face
        buffer.addVertex(pose, STREAM_X, streamTop,    STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, STREAM_X, streamBottom, STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, STREAM_X, streamBottom, STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, STREAM_X, streamTop,    STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(1, 0, 0);
    }



    private void renderStream(PoseStack poseStack, MultiBufferSource bufferSource,
                              float r, float g, float b, float a, TextureAtlasSprite sprite,
                              int packedLight) {
        var buffer = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        float half = STREAM_WIDTH / 2f;
        float top = STREAM_Y_TOP;
        float bottom = -1.0f;

        // North face
        buffer.addVertex(pose, STREAM_X - half, top, STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, STREAM_X - half, bottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, STREAM_X + half, bottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, STREAM_X + half, top, STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, -1);

        // South face
        buffer.addVertex(pose, STREAM_X + half, top, STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, STREAM_X + half, bottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, STREAM_X - half, bottom, STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, STREAM_X - half, top, STREAM_Z)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, 1);

        // West face
        buffer.addVertex(pose, STREAM_X, top, STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, STREAM_X, bottom, STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, STREAM_X, bottom, STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, STREAM_X, top, STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(-1, 0, 0);

        // East face
        buffer.addVertex(pose, STREAM_X, top, STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, STREAM_X, bottom, STREAM_Z + half)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, STREAM_X, bottom, STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, STREAM_X, top, STREAM_Z - half)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(1, 0, 0);
    }
}