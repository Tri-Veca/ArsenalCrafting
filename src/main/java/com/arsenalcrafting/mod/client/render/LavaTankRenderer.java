package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.LavaTankBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.joml.Matrix4f;

public class LavaTankRenderer implements BlockEntityRenderer<LavaTankBlockEntity> {

    public LavaTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LavaTankBlockEntity tank,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       int packedOverlay) {

        if (tank.isEmpty()) {
            return;
        }

        // Get fluid stack from the tank
        var fluidStack = tank.getFluid();
        if (fluidStack.isEmpty()) {
            return;
        }

        int amount = fluidStack.getAmount();
        int capacity = tank.getCapacity();

        if (amount <= 0 || capacity <= 0) {
            return;
        }

        float fill = Mth.clamp(amount / (float) capacity, 0.01f, 0.999f);

        // Get the appropriate texture based on the fluid
        var fluidExtensions = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation stillTexture = fluidExtensions.getStillTexture(fluidStack);
        if (stillTexture == null) {
            stillTexture = fluidExtensions.getStillTexture();
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(stillTexture);

        // Get color tint for the fluid (lava is orange/red)
        int color = fluidExtensions.getTintColor(fluidStack);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        // inner bounds inside the glass frame
        float inset   = 0.06f;
        float min     = inset;
        float max     = 1f - inset;
        float yBottom = inset;
        float yTop    = yBottom + (1f - 2f * inset) * fill;

        poseStack.pushPose();

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        Matrix4f pose = poseStack.last().pose();

        // Render all faces with the fluid color
        renderFluidCube(vc, pose, min, max, yBottom, yTop, u0, u1, v0, v1, r, g, b, a, packedLight);

        poseStack.popPose();
    }

    private void renderFluidCube(VertexConsumer vc, Matrix4f pose,
                                 float min, float max, float yBottom, float yTop,
                                 float u0, float u1, float v0, float v1,
                                 float r, float g, float b, float a,
                                 int packedLight) {

        // North face
        quadDoubleSided(vc, pose,
                min, yBottom, min,
                max, yBottom, min,
                max, yTop,    min,
                min, yTop,    min,
                u0, v1, u1, v0, r, g, b, a, packedLight);

        // South face
        quadDoubleSided(vc, pose,
                max, yBottom, max,
                min, yBottom, max,
                min, yTop,    max,
                max, yTop,    max,
                u0, v1, u1, v0, r, g, b, a, packedLight);

        // West face
        quadDoubleSided(vc, pose,
                min, yBottom, max,
                min, yBottom, min,
                min, yTop,    min,
                min, yTop,    max,
                u0, v1, u1, v0, r, g, b, a, packedLight);

        // East face
        quadDoubleSided(vc, pose,
                max, yBottom, min,
                max, yBottom, max,
                max, yTop,    max,
                max, yTop,    min,
                u0, v1, u1, v0, r, g, b, a, packedLight);

        // Top face
        quadDoubleSided(vc, pose,
                min, yTop, min,
                max, yTop, min,
                max, yTop, max,
                min, yTop, max,
                u0, v1, u1, v0, r, g, b, a, packedLight);
    }

    private void quadDoubleSided(VertexConsumer vc, Matrix4f pose,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float u0, float v0, float u1, float v1,
                                 float r, float g, float b, float a,
                                 int packedLight) {

        int sky = (packedLight >> 16) & 0xFFFF;
        int block = packedLight & 0xFFFF;

        // Front face
        addVertex(vc, pose, x0, y0, z0, u0, v1, r, g, b, a, block, sky);
        addVertex(vc, pose, x1, y1, z1, u1, v1, r, g, b, a, block, sky);
        addVertex(vc, pose, x2, y2, z2, u1, v0, r, g, b, a, block, sky);
        addVertex(vc, pose, x3, y3, z3, u0, v0, r, g, b, a, block, sky);

        // Back face
        addVertex(vc, pose, x3, y3, z3, u0, v0, r, g, b, a, block, sky);
        addVertex(vc, pose, x2, y2, z2, u1, v0, r, g, b, a, block, sky);
        addVertex(vc, pose, x1, y1, z1, u1, v1, r, g, b, a, block, sky);
        addVertex(vc, pose, x0, y0, z0, u0, v1, r, g, b, a, block, sky);
    }

    private void addVertex(VertexConsumer vc, Matrix4f pose,
                           float x, float y, float z,
                           float u, float v,
                           float r, float g, float b, float a,
                           int blockLight, int skyLight) {

        vc.addVertex(pose, x, y, z)
                .setColor((int)(r * 255), (int)(g * 255), (int)(b * 255), (int)(a * 255))
                .setUv(u, v)
                .setUv2(blockLight, skyLight)
                .setNormal(0, 1, 0);
    }
}