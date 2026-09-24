package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.ValveFaucetBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.smeltery.SmelteryDebug;
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

    // ==== Falling stream (below the faucet, pouring into a mold/basin) ====
    // Left/Right position (increase = move right)
    private static final float STREAM_X = 8f / 16f;
    // Forward/Back position (increase = move closer to the faucet)
    private static final float STREAM_Z = 9.78f / 16f;
    // Top height (increase = stream starts higher)
    private static final float STREAM_Y_TOP = 7f / 16f;
    // Stream thickness (increase = wider stream)
    private static final float STREAM_WIDTH = 2.0f / 16f;
    private static final float STREAM_HALF = STREAM_WIDTH / 2f;
    // Bottom (decrease = stream goes lower, more negative = deeper into block below)
    private static final float STREAM_Y_BOTTOM = -1.0f;

    // ==== Static channel fill (fluid sitting inside the faucet's own trough) ====
    // Derived from the faucet model's own geometry (inner walls between the two side arms).
    private static final float CHANNEL_X_MIN = 7.01f / 16f;
    private static final float CHANNEL_X_MAX = 8.99f / 16f;
    private static final float CHANNEL_Z_MIN = 0.6676f;  // was 0.637
    private static final float CHANNEL_Z_MAX = 0.9906f;  // was 0.96
    private static final float CHANNEL_Y_FLOOR = 5.94f / 16f;

    private static final float CHANNEL_Y_FILL = 7.02f / 16f;

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

        // Rotate the local coordinate space to match the faucet's actual facing —
        // all quad coordinates below are authored for the default (NORTH) orientation,
        // and this rotation carries them to whichever way the block is actually placed.
        poseStack.pushPose();
        poseStack.translate(0.5, 0.5, 0.5);
        poseStack.mulPose(com.mojang.math.Axis.YP.rotationDegrees(getFacingAngle(be)));
        poseStack.translate(-0.5, -0.5, -0.5);
        PoseStack.Pose pose = poseStack.last();

        // Falling stream below the faucet.
        float streamMinX = STREAM_X - STREAM_HALF;
        float streamMaxX = STREAM_X + STREAM_HALF;
        float streamMinZ = STREAM_Z - STREAM_HALF;
        float streamMaxZ = STREAM_Z + STREAM_HALF;
        renderFluidBox(buffer, pose, streamMinX, streamMaxX, STREAM_Y_BOTTOM, STREAM_Y_TOP,
                streamMinZ, streamMaxZ, r, g, b, a, sprite, packedLight);

        // Static fill sitting inside the faucet's own channel.
        renderFluidBox(buffer, pose, CHANNEL_X_MIN, CHANNEL_X_MAX, CHANNEL_Y_FLOOR, CHANNEL_Y_FILL,
                CHANNEL_Z_MIN, CHANNEL_Z_MAX, r, g, b, a, sprite, packedLight);

        poseStack.popPose();
    }

    /**
     * Maps the faucet's FACING to the Y-axis rotation needed to align the fluid quads
     * (authored for the default NORTH orientation) with the block's actual placement.
     * Guards against a stale blockstate — if this position no longer holds a
     * ValveFaucetBlock (e.g. it was just removed to air), fall back to 0 rather than crash.
     */
    private float getFacingAngle(ValveFaucetBlockEntity be) {
        var level = be.getLevel();
        if (level == null) return 0f;
        var state = level.getBlockState(be.getBlockPos());
        if (!(state.getBlock() instanceof com.arsenalcrafting.mod.blocks.ValveFaucetBlock)) return 0f;
        var facing = state.getValue(com.arsenalcrafting.mod.blocks.ValveFaucetBlock.FACING);
        float angle = switch (facing) {
            case NORTH -> 0f;
            case EAST  -> 90f;   // or keep -90 if you prefer; test both later
            case SOUTH -> 0f;    // ← the only value that differs from ±180
            case WEST  -> -90f;
            default    -> 0f;
        };
        SmelteryDebug.faucetPouring("Render", "pos=" + be.getBlockPos() + " facing=" + facing + " angle=" + angle);
        return angle;
    }

    /**
     * Draws a single axis-aligned fluid box (all six faces) using the given fluid sprite/tint.
     * Shared by both the falling stream and the static channel fill so the two don't duplicate
     * the same vertex-building logic.
     */
    private void renderFluidBox(VertexConsumer buffer, PoseStack.Pose pose,
                                float minX, float maxX, float bottom, float top,
                                float minZ, float maxZ,
                                float r, float g, float b, float a,
                                TextureAtlasSprite sprite, int packedLight) {

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

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

        // Bottom face - facing -Y
        buffer.addVertex(pose, minX, bottom, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, -1, 0);
        buffer.addVertex(pose, maxX, bottom, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, -1, 0);
        buffer.addVertex(pose, maxX, bottom, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, -1, 0);
        buffer.addVertex(pose, minX, bottom, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, -1, 0);

        // Top face - facing +Y, reversed winding order
        buffer.addVertex(pose, minX, top, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, maxX, top, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, maxX, top, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, minX, top, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 1, 0);
    }
}