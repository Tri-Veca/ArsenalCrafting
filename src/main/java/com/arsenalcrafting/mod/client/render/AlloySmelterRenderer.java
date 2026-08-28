package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.AlloySmelterBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.smeltery.SmelteryData;
import com.arsenalcrafting.mod.registry.ModFluids;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.List;

public class AlloySmelterRenderer implements BlockEntityRenderer<AlloySmelterBlockEntity> {

    public AlloySmelterRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(AlloySmelterBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        SmelteryData data = be.getSmelteryData();
        if (data == null || !data.isValid()) return;

        List<FluidStack> fluids = be.getAllOutputFluids();
        if (fluids.isEmpty()) return;

        int totalAmount = be.getOutputFluidAmount();
        int capacity = be.getOutputFluidCapacity();
        if (capacity <= 0 || totalAmount <= 0) return;

        // Use the first (top) fluid only
        FluidStack topFluid = fluids.get(0);
        if (topFluid.isEmpty()) return;

        // Get fluid texture and color
        var ext = IClientFluidTypeExtensions.of(topFluid.getFluid());
        var stillTex = ext.getStillTexture(topFluid);
        if (stillTex == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTex);

        int color = ext.getTintColor(topFluid);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.85f;

        // Interior bounds relative to controller block position
        BlockPos controllerPos = data.controllerPos();
        BlockPos interiorOrigin = data.interiorOrigin();

        // Offset from controller to interior origin (in blocks)
        float offsetX = interiorOrigin.getX() - controllerPos.getX();
        float offsetY = interiorOrigin.getY() - controllerPos.getY();
        float offsetZ = interiorOrigin.getZ() - controllerPos.getZ();

        // Interior dimensions in blocks
        float interiorWidth = data.width();
        float interiorDepth = data.depth();
        float interiorHeight = data.height();

        // Fill level — how high the fluid reaches inside the interior
        float fillFraction = (float) totalAmount / (float) capacity;
        float fluidHeight = interiorHeight * fillFraction;

        // Fluid top surface Y (relative to controller)
        float minX = offsetX;
        float maxX = offsetX + interiorWidth;
        float minZ = offsetZ;
        float maxZ = offsetZ + interiorDepth;
        float minY = offsetY;
        float maxY = offsetY + fluidHeight;

        // Clamp to interior bounds
        maxY = Math.min(maxY, offsetY + interiorHeight);

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        VertexConsumer buffer = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        // Top face only — just the surface of the fluid
        buffer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 1, 0);

        // Side faces — visible through casing gaps/glass
        // North (minZ)
        buffer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, -1);
        buffer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, -1);

        // South (maxZ)
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 0, 1);
        buffer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 0, 1);

        // West (minX)
        buffer.addVertex(pose, minX, minY, maxZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, minX, maxY, maxZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, minX, maxY, minZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(-1, 0, 0);
        buffer.addVertex(pose, minX, minY, minZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(-1, 0, 0);

        // East (maxX)
        buffer.addVertex(pose, maxX, minY, minZ).setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, maxX, maxY, minZ).setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, maxX, maxY, maxZ).setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(1, 0, 0);
        buffer.addVertex(pose, maxX, minY, maxZ).setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(1, 0, 0);
    }

    @Override
    public boolean shouldRenderOffScreen(AlloySmelterBlockEntity be) {
        // Fluid spans multiple blocks so don't cull when controller goes off screen
        return true;
    }

    @Override
    public int getViewDistance() {
        return 64;
    }
}