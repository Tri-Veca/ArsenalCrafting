package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.CastingStationBlockEntity;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.minecraft.world.level.block.Block;

public class CastingStationRenderer implements BlockEntityRenderer<CastingStationBlockEntity> {

    // Basin interior bounds
    private static final float BASIN_MIN_X  = 3f / 16f;
    private static final float BASIN_MAX_X  = 13f / 16f;
    private static final float BASIN_MIN_Z  = 3f / 16f;
    private static final float BASIN_MAX_Z  = 13f / 16f;
    private static final float BASIN_FLOOR_Y = 15f / 16f;

    // Fluid bounds (tighter — mold area only)
    private static final float FLUID_MIN_X  = 3f / 16f;
    private static final float FLUID_MAX_X  = 13f / 16f;
    private static final float FLUID_MIN_Z  = 3f / 16f;
    private static final float FLUID_MAX_Z  = 13f / 16f;

    private static final RandomSource RANDOM = RandomSource.create();

    public CastingStationRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CastingStationBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // ── MOLD — always visible when present and not yet collected ──────────
        if (be.hasMold() && be.getMoldComponentId() != null)  {
            renderMold(be, poseStack, bufferSource, packedLight, packedOverlay);
        }

        // ── BUCKET — visible when placed, no fluid yet ────────────────────────
        if (be.hasBucket() && be.getFluidAmount() == 0 && !be.isReady()) {
            renderBucket(poseStack, bufferSource, packedLight, packedOverlay);
        }

        // ── FLUID QUAD — only while pouring (fluid present but NOT yet setting)
        if (be.getFluidAmount() > 0 && !be.isSetting() && !be.isReady()
                && be.getFluidMaterialName() != null) {
            renderFluid(be, poseStack, bufferSource, packedLight);
        }

        // ── DARKENED ITEM — while setting, brightens as progress increases ────
        if (be.isSetting() && be.getFluidMaterialName() != null) {
            float progress = be.getSettingTime() > 0
                    ? (float) be.getSettingProgress() / be.getSettingTime()
                    : 0f;
            // Brightness goes from 0.1 (very dark) to 0.85 (almost full)
            float brightness = 0.1f + (progress * 0.75f);
            renderFlatItem(be, poseStack, bufferSource, packedLight, packedOverlay, brightness);

            // Spawn particles on client
            spawnSettingParticles(be, partialTick);
        }

        // ── FULL BRIGHTNESS ITEM — when ready to collect ──────────────────────
        if (be.isReady() && be.getFluidMaterialName() != null) {
            renderFlatItem(be, poseStack, bufferSource, packedLight, packedOverlay, 1.0f);
        }
    }

    // ── MOLD ──────────────────────────────────────────────────────────────────

    private void renderMold(CastingStationBlockEntity be, PoseStack poseStack,
                            MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String moldId = be.getMoldComponentId();
        ToolComponentType type = ToolComponentType.fromId(moldId);

        DeferredHolder<Block, ?> moldHolder = null;
        if (moldId != null && moldId.equals("ingot")) {
            moldHolder = be.isBronzeMold() ? ModBlocks.BRONZE_INGOT_MOLD : ModBlocks.INGOT_MOLD;
        } else if (type != null) {
            moldHolder = be.isBronzeMold()
                    ? ModBlocks.BRONZE_MOLD_BLOCKS.get(type)
                    : ModBlocks.MOLD_BLOCKS.get(type);
        }
        if (moldHolder == null) return;

        poseStack.pushPose();
        poseStack.translate(0.5f, BASIN_FLOOR_Y + 0.01f, 0.5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(0.85f, 0.85f, 0.85f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(moldHolder.get().asItem()),
                ItemDisplayContext.FIXED,
                packedLight, packedOverlay,
                poseStack, bufferSource, be.getLevel(), 0);

        poseStack.popPose();
    }

    // ── BUCKET ────────────────────────────────────────────────────────────────

    private void renderBucket(PoseStack poseStack, MultiBufferSource bufferSource,
                              int packedLight, int packedOverlay) {
        poseStack.pushPose();
        poseStack.translate(0.5f, BASIN_FLOOR_Y + 0.01f, 0.5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(0.7f, 0.7f, 0.7f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(Items.BUCKET),
                ItemDisplayContext.FIXED,
                packedLight, packedOverlay,
                poseStack, bufferSource, null, 0);

        poseStack.popPose();
    }

    // ── FLUID QUAD (while pouring only) ──────────────────────────────────────

    private void renderFluid(CastingStationBlockEntity be, PoseStack poseStack,
                             MultiBufferSource bufferSource, int packedLight) {
        AlloyMaterial mat = AlloyMaterial.fromName(be.getFluidMaterialName());
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

        float fluidY = BASIN_FLOOR_Y + 0.01f;

        var buffer = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();

        buffer.addVertex(pose, FLUID_MIN_X, fluidY, FLUID_MIN_Z)
                .setColor(r, g, b, 0.9f).setUv(u0, v0).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, FLUID_MIN_X, fluidY, FLUID_MAX_Z)
                .setColor(r, g, b, 0.9f).setUv(u0, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, FLUID_MAX_X, fluidY, FLUID_MAX_Z)
                .setColor(r, g, b, 0.9f).setUv(u1, v1).setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, FLUID_MAX_X, fluidY, FLUID_MIN_Z)
                .setColor(r, g, b, 0.9f).setUv(u1, v0).setLight(packedLight).setNormal(0, 1, 0);
    }

    // ── FLAT ITEM (setting + ready) ───────────────────────────────────────────

    private void renderFlatItem(CastingStationBlockEntity be, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight,
                                int packedOverlay, float brightness) {
        AlloyMaterial mat = AlloyMaterial.fromName(be.getFluidMaterialName());
        if (mat == null) return;

        ItemStack output = getOutputItem(be, mat);
        if (output.isEmpty()) return;

        // Convert brightness to packed light — fake it by scaling light value
        // Full light = 0xF000F0, dark = scale both sky and block light down
        int adjustedLight = scalePackedLight(brightness);

        poseStack.pushPose();

        // Sit flat in the mold at basin floor level
        poseStack.translate(0.65f, BASIN_FLOOR_Y + 0.03f, 0.4f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(1f, 1f, 1f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                output,
                ItemDisplayContext.FIXED,
                adjustedLight,
                packedOverlay,
                poseStack,
                bufferSource,
                be.getLevel(),
                0
        );

        poseStack.popPose();
    }

    /**
     * Scale packed light value by brightness factor.
     * Packed light format: sky light in bits 20-16, block light in bits 4-0 (shifted by 4).
     */
    private int scalePackedLight(float brightness) {
        int lightLevel = Math.max(0, Math.min(15, (int)(brightness * 15)));
        return LightTexture.pack(lightLevel, lightLevel);
    }

    // ── PARTICLES (during setting only) ──────────────────────────────────────

    private void spawnSettingParticles(CastingStationBlockEntity be, float partialTick) {
        if (be.getLevel() == null || !be.getLevel().isClientSide) return;

        // Only spawn occasionally — every ~10 ticks worth
        if (RANDOM.nextFloat() > 0.15f) return;

        double x = be.getBlockPos().getX() + 0.25 + RANDOM.nextDouble() * 0.5;
        double y = be.getBlockPos().getY() + BASIN_FLOOR_Y + 0.05;
        double z = be.getBlockPos().getZ() + 0.25 + RANDOM.nextDouble() * 0.5;

        // Smoke particles
        be.getLevel().addParticle(
                ParticleTypes.SMOKE,
                x, y, z,
                0, 0.01, 0
        );

        // Occasional flame particle
        if (RANDOM.nextFloat() > 0.6f) {
            be.getLevel().addParticle(
                    ParticleTypes.FLAME,
                    x, y, z,
                    0, 0.005, 0
            );
        }
    }

    // ── OUTPUT ITEM HELPER ────────────────────────────────────────────────────

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