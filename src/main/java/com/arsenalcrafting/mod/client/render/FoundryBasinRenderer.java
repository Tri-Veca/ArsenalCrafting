package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.entity.FoundryBasinBlockEntity;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
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

public class FoundryBasinRenderer implements BlockEntityRenderer<FoundryBasinBlockEntity> {

    private static final float BASIN_FLOOR_Y = 2f / 16f; // basin is lower than casting station
    private static final float FLUID_MIN_X   = 1f / 16f;
    private static final float FLUID_MAX_X   = 15f / 16f;
    private static final float FLUID_MIN_Z   = 1f / 16f;
    private static final float FLUID_MAX_Z   = 15f / 16f;

    private static final RandomSource RANDOM = RandomSource.create();

    public FoundryBasinRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(FoundryBasinBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        // Fluid quad while fluid is present and setting
        if (be.getFluidAmount() > 0 && be.getFluidMaterialName() != null) {
            renderFluid(be, poseStack, bufferSource, packedLight);
        }

        // Darkening block item while setting
        if (be.isSetting() && be.getFluidMaterialName() != null) {
            float progress = be.getSettingTime() > 0
                    ? (float) be.getSettingProgress() / be.getSettingTime() : 0f;
            float brightness = 0.1f + (progress * 0.75f);
            renderBlockItem(be, poseStack, bufferSource, packedLight, packedOverlay, brightness);
            spawnSettingParticles(be, partialTick);
        }

        // Full brightness block when ready
        if (be.isReady() && be.getFluidMaterialName() != null) {
            renderBlockItem(be, poseStack, bufferSource, packedLight, packedOverlay, 1.0f);
        }
    }

    private void renderFluid(FoundryBasinBlockEntity be, PoseStack poseStack,
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

        // Full basin surface — sits just below the top of the basin walls
        float fluidY = 14f / 16f;

        // Use cutout to avoid xray through walls
        var buffer = bufferSource.getBuffer(RenderType.translucent());
        PoseStack.Pose pose = poseStack.last();

        buffer.addVertex(pose, FLUID_MIN_X, fluidY, FLUID_MIN_Z)
                .setColor(r, g, b, 1.0f).setUv(sprite.getU0(), sprite.getV0())
                .setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, FLUID_MIN_X, fluidY, FLUID_MAX_Z)
                .setColor(r, g, b, 1.0f).setUv(sprite.getU0(), sprite.getV1())
                .setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, FLUID_MAX_X, fluidY, FLUID_MAX_Z)
                .setColor(r, g, b, 1.0f).setUv(sprite.getU1(), sprite.getV1())
                .setLight(packedLight).setNormal(0, 1, 0);
        buffer.addVertex(pose, FLUID_MAX_X, fluidY, FLUID_MIN_Z)
                .setColor(r, g, b, 1.0f).setUv(sprite.getU1(), sprite.getV0())
                .setLight(packedLight).setNormal(0, 1, 0);
    }

    private void renderBlockItem(FoundryBasinBlockEntity be, PoseStack poseStack,
                                 MultiBufferSource bufferSource, int packedLight,
                                 int packedOverlay, float brightness) {
        AlloyMaterial mat = AlloyMaterial.fromName(be.getFluidMaterialName());
        if (mat == null) return;

        ItemStack block = switch (mat) {
            case COPPER    -> new ItemStack(Items.COPPER_BLOCK);
            case IRON      -> new ItemStack(Items.IRON_BLOCK);
            case GOLD      -> new ItemStack(Items.GOLD_BLOCK);
            case DIAMOND   -> new ItemStack(Items.DIAMOND_BLOCK);
            case NETHERITE -> new ItemStack(Items.NETHERITE_BLOCK);
            case COAL      -> new ItemStack(Items.COAL_BLOCK);
            default        -> ItemStack.EMPTY;
        };
        if (block.isEmpty()) return;

        int adjustedLight = LightTexture.pack(
                Math.max(0, Math.min(15, (int)(brightness * 15))),
                Math.max(0, Math.min(15, (int)(brightness * 15))));

        poseStack.pushPose();
        poseStack.translate(0.5f, BASIN_FLOOR_Y + 0.5f, 0.5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f)); // lay flat like casting station
        poseStack.scale(1.5f, 1.5f, 1.5f); // full size, basin is wide enough

        Minecraft.getInstance().getItemRenderer().renderStatic(
                block, ItemDisplayContext.FIXED,
                adjustedLight, packedOverlay,
                poseStack, bufferSource, be.getLevel(), 0);

        poseStack.popPose();
    }

    private void spawnSettingParticles(FoundryBasinBlockEntity be, float partialTick) {
        if (be.getLevel() == null || !be.getLevel().isClientSide) return;
        if (RANDOM.nextFloat() > 0.05f) return;

        double x = be.getBlockPos().getX() + 0.25 + RANDOM.nextDouble() * 0.5;
        double y = be.getBlockPos().getY() + BASIN_FLOOR_Y + 1;
        double z = be.getBlockPos().getZ() + 0.25 + RANDOM.nextDouble() * 0.5;

        be.getLevel().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
        if (RANDOM.nextFloat() > 0.8f)
            be.getLevel().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.005, 0);
    }
}