package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.CastingStationBlockEntity;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.Block;

public class CastingStationRenderer implements BlockEntityRenderer<CastingStationBlockEntity> {

    // Basin interior bounds in block space (0-1)
    private static final float BASIN_MIN_X = 0f / 16f;
    private static final float BASIN_MAX_X = 16f / 16f;
    private static final float BASIN_MIN_Z = 0f / 16f;
    private static final float BASIN_MAX_Z = 16f / 16f;
    private static final float BASIN_FLOOR_Y = 15f / 16f;
    private static final float BASIN_TOP_Y   = 15.5f / 16f;

    public CastingStationRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CastingStationBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // ── RENDER MOLD (flat on basin floor like a carpet) ───────────────────
        if (be.hasMold() && be.getMoldComponentId() != null && !be.isReady()){
            renderMold(be, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // ── RENDER BUCKET (flat on basin floor) ───────────────────────────────
        if (be.hasBucket() && be.getFluidAmount() == 0 && !be.isReady()) {
            renderBucket(poseStack, bufferSource, packedLight, packedOverlay);
        }

        // ── RENDER FLUID (with pour animation) ─────────────────────────────────
        if (be.getFluidAmount() > 0 && be.getFluidMaterialName() != null) {
            renderFluid(be, partialTick, poseStack, bufferSource, packedLight);
        }

        // ── RENDER CAST ITEM (when ready) ─────────────────────────────────────
        if (be.isReady() && be.getFluidMaterialName() != null) {
            renderCastItem(be, partialTick, poseStack, bufferSource, packedLight, packedOverlay);
        }
    }

    // ── MOLD RENDERING (flat on basin floor) ──────────────────────────────────
    private void renderMold(CastingStationBlockEntity be, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String moldId = be.getMoldComponentId();
        ToolComponentType type = ToolComponentType.fromId(moldId);

        // Get the mold block to render
        DeferredHolder<Block, ?> moldHolder = null;

        if (moldId.equals("ingot")) {
            // For ingot molds
            moldHolder = be.isBronzeMold()
                    ? ModBlocks.BRONZE_INGOT_MOLD
                    : ModBlocks.INGOT_MOLD;
        } else if (type != null) {
            moldHolder = be.isBronzeMold()
                    ? ModBlocks.BRONZE_MOLD_BLOCKS.get(type)
                    : ModBlocks.MOLD_BLOCKS.get(type);
        }

        if (moldHolder == null) return;

        poseStack.pushPose();

        // Position flat on the basin floor - INCREASE Y POSITION
        // Original was 0.01f above floor, let's raise it higher
        poseStack.translate(0.5f, BASIN_FLOOR_Y + 0.05f, 0.5f);

        // Rotate to lay flat (like a carpet on the floor)
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(0.85f, 0.85f, 0.85f);

        ItemStack moldStack = new ItemStack(moldHolder.get().asItem());
        if (!moldStack.isEmpty()) {
            Minecraft.getInstance().getItemRenderer().renderStatic(
                    moldStack,
                    ItemDisplayContext.FIXED,
                    packedLight,
                    packedOverlay,
                    poseStack,
                    bufferSource,
                    be.getLevel(),
                    0
            );
        }

        poseStack.popPose();
    }

    // ── BUCKET RENDERING (flat on basin floor) ────────────────────────────────
    private void renderBucket(PoseStack poseStack, MultiBufferSource bufferSource,
                              int packedLight, int packedOverlay) {
        poseStack.pushPose();

        // Increase Y position for bucket too
        poseStack.translate(0.5f, BASIN_FLOOR_Y + 0.05f, 0.5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(0.7f, 0.7f, 0.7f);

        ItemStack bucketStack = new ItemStack(Items.BUCKET);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                bucketStack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                null,
                0
        );

        poseStack.popPose();
    }

    // ── FLUID RENDERING (with pour animation) ─────────────────────────────────
    private void renderFluid(CastingStationBlockEntity be, float partialTick,
                             PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        AlloyMaterial mat = AlloyMaterial.fromName(be.getFluidMaterialName());
        if (mat == null) return;

        var modFluids = com.arsenalcrafting.mod.registry.ModFluids.getSource(mat);
        if (modFluids == null) return;

        FluidStack fluidStack = new FluidStack(modFluids, 1000);
        var ext = IClientFluidTypeExtensions.of(modFluids);
        var stillTex = ext.getStillTexture(fluidStack);
        if (stillTex == null) return;

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTex);

        int color = ext.getTintColor(fluidStack);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = 0.9f;

        // Calculate fluid level based on setting progress
        float progress = be.isSetting() && be.getSettingTime() > 0
                ? (float) be.getSettingProgress() / be.getSettingTime()
                : 1.0f;

        // Fluid rises from floor to just above mold as it fills
        float fluidY = BASIN_FLOOR_Y + 0.01f + (progress * 0.03f);

        var buffer = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        // Render fluid surface
        buffer.addVertex(pose, BASIN_MIN_X, fluidY, BASIN_MIN_Z)
                .setColor(r, g, b, a).setUv(u0, v0).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, BASIN_MIN_X, fluidY, BASIN_MAX_Z)
                .setColor(r, g, b, a).setUv(u0, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, BASIN_MAX_X, fluidY, BASIN_MAX_Z)
                .setColor(r, g, b, a).setUv(u1, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, BASIN_MAX_X, fluidY, BASIN_MIN_Z)
                .setColor(r, g, b, a).setUv(u1, v0).setLight(packedLight).setNormal(0, 1, 0);

        // Render pouring stream while setting
    }

    // ── CAST ITEM RENDERING (when ready) ──────────────────────────────────────
    private void renderCastItem(CastingStationBlockEntity be, float partialTick,
                                PoseStack poseStack, MultiBufferSource bufferSource,
                                int packedLight, int packedOverlay) {
        AlloyMaterial mat = AlloyMaterial.fromName(be.getFluidMaterialName());
        if (mat == null) return;

        ItemStack output = getOutputItem(be, mat);
        if (output.isEmpty()) return;

        poseStack.pushPose();

        // Raise the finished item higher so it sits on top of the fluid/mold
        poseStack.translate(0.5f, BASIN_FLOOR_Y + 0.12f, 0.5f);


        poseStack.scale(0.9f, 0.9f, 0.9f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                output,
                ItemDisplayContext.GROUND,
                packedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }

    private ItemStack getOutputItem(CastingStationBlockEntity be, AlloyMaterial mat) {
        if (be.getMode() == CastingStationBlockEntity.CastMode.BUCKET) {
            var bucketHolder = ModItems.ALLOY_BUCKETS.get(mat);
            return bucketHolder != null ? new ItemStack(bucketHolder.get()) : ItemStack.EMPTY;
        }

        String moldId = be.getMoldComponentId();
        if (moldId == null) return ItemStack.EMPTY;

        if (moldId.equals("ingot")) {
            var ingotHolder = ModItems.ALLOY_ITEMS.get(mat);
            return ingotHolder != null ? new ItemStack(ingotHolder.get()) : ItemStack.EMPTY;
        }

        ToolComponentType type = ToolComponentType.fromId(moldId);
        if (type == null) return ItemStack.EMPTY;

        var componentMap = ModItems.TOOL_COMPONENTS.get(mat);
        if (componentMap == null) return ItemStack.EMPTY;

        var holder = componentMap.get(type);
        return holder != null ? new ItemStack(holder.get()) : ItemStack.EMPTY;
    }
}