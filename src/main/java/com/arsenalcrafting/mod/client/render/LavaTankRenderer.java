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
import org.joml.Matrix4f;

public class LavaTankRenderer implements BlockEntityRenderer<LavaTankBlockEntity> {

    private static final ResourceLocation LAVA_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "block/lava_still");

    public LavaTankRenderer(BlockEntityRendererProvider.Context ctx) {}

    @Override
    public void render(LavaTankBlockEntity tank,
                       float partialTicks,
                       PoseStack poseStack,
                       MultiBufferSource buffer,
                       int packedLight,
                       int packedOverlay) {

        int amount = tank.getFluidAmount();
        int capacity = tank.getCapacity();

        if (amount <= 0 || capacity <= 0) {
            return;
        }

        float fill = Mth.clamp(amount / (float) capacity, 0.01f, 0.999f);

        // inner bounds inside the glass frame
        float inset   = 0.06f;          // very small
        float min     = inset;           // x/z min
        float max     = 1f - inset;      // x/z max
        float yBottom = inset;           // bottom just above 0
        float yTop    = yBottom + (1f - 2f * inset) * fill;

        poseStack.pushPose();

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(LAVA_TEXTURE);

        VertexConsumer vc = buffer.getBuffer(RenderType.translucent());

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        Matrix4f pose = poseStack.last().pose();

        // --------- NORTH face (z = min) ----------
        quadDoubleSided(vc, pose,
                min, yBottom, min,
                max, yBottom, min,
                max, yTop,    min,
                min, yTop,    min,
                u0, v1, u1, v0, packedLight);

        // --------- SOUTH face (z = max) ----------
        quadDoubleSided(vc, pose,
                max, yBottom, max,
                min, yBottom, max,
                min, yTop,    max,
                max, yTop,    max,
                u0, v1, u1, v0, packedLight);

        // --------- WEST face (x = min) ----------
        quadDoubleSided(vc, pose,
                min, yBottom, max,
                min, yBottom, min,
                min, yTop,    min,
                min, yTop,    max,
                u0, v1, u1, v0, packedLight);

        // --------- EAST face (x = max) ----------
        quadDoubleSided(vc, pose,
                max, yBottom, min,
                max, yBottom, max,
                max, yTop,    max,
                max, yTop,    min,
                u0, v1, u1, v0, packedLight);

        // --------- TOP face ----------
        quadDoubleSided(vc, pose,
                min, yTop, min,
                max, yTop, min,
                max, yTop, max,
                min, yTop, max,
                u0, v1, u1, v0, packedLight);

        // (optional) BOTTOM face if you ever want it visible from below:
        // quadDoubleSided(...)

        poseStack.popPose();
    }

    /**
     * Draw a quad *both* directions so it’s visible from any side (avoids backface culling issues).
     */
    private void quadDoubleSided(VertexConsumer vc,
                                 Matrix4f pose,
                                 float x0, float y0, float z0,
                                 float x1, float y1, float z1,
                                 float x2, float y2, float z2,
                                 float x3, float y3, float z3,
                                 float u0, float v0, float u1, float v1,
                                 int packedLight) {
        // front (0 -> 1 -> 2 -> 3)
        addVertex(vc, pose, x0, y0, z0, u0, v1, packedLight);
        addVertex(vc, pose, x1, y1, z1, u1, v1, packedLight);
        addVertex(vc, pose, x2, y2, z2, u1, v0, packedLight);
        addVertex(vc, pose, x3, y3, z3, u0, v0, packedLight);

        // back (3 -> 2 -> 1 -> 0)
        addVertex(vc, pose, x3, y3, z3, u0, v0, packedLight);
        addVertex(vc, pose, x2, y2, z2, u1, v0, packedLight);
        addVertex(vc, pose, x1, y1, z1, u1, v1, packedLight);
        addVertex(vc, pose, x0, y0, z0, u0, v1, packedLight);
    }

    private void addVertex(VertexConsumer vc,
                           Matrix4f pose,
                           float x, float y, float z,
                           float u, float v,
                           int packedLight) {

        int sky   = (packedLight >> 16) & 0xFFFF;
        int block =  packedLight        & 0xFFFF;

        vc.addVertex(pose, x, y, z)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setUv1(0, 0)            // overlay
                .setUv2(block, sky)      // light
                .setNormal(0, 1, 0);
    }
}
