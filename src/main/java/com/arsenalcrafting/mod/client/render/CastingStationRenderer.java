package com.arsenalcrafting.mod.client.render;

import com.arsenalcrafting.mod.blocks.CastingStationBlock;
import com.arsenalcrafting.mod.blocks.entity.CastingStationBlockEntity;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.ItemStackHelper;
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
import net.minecraft.core.Direction;
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

    private static final float BASIN_FLOOR_Y = 15f / 16f;

    private static final float FLUID_MIN_X = 3f / 16f;
    private static final float FLUID_MAX_X = 13f / 16f;
    private static final float FLUID_MIN_Z = 3f / 16f;
    private static final float FLUID_MAX_Z = 13f / 16f;

    private static final RandomSource RANDOM = RandomSource.create();

    public CastingStationRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(CastingStationBlockEntity be, float partialTick,
                       PoseStack poseStack, MultiBufferSource bufferSource,
                       int packedLight, int packedOverlay) {

        if (be.hasMold() && be.getMoldComponentId() != null) {
            renderMold(be, poseStack, bufferSource, packedLight, packedOverlay);
        }

        if (be.hasBucket() && be.getFluidAmount() == 0 && !be.isReady()) {
            renderBucket(be, poseStack, bufferSource, packedLight, packedOverlay);
        }

        if (be.getFluidAmount() > 0 && !be.isSetting() && !be.isReady()
                && be.getFluidMaterialName() != null) {
            renderFluid(be, poseStack, bufferSource, packedLight);
        }

        if (be.isSetting() && be.getFluidMaterialName() != null) {
            float progress = be.getSettingTime() > 0
                    ? (float) be.getSettingProgress() / be.getSettingTime()
                    : 0f;
            float brightness = 0.1f + (progress * 0.75f);
            renderFlatItem(be, poseStack, bufferSource, packedLight, packedOverlay, brightness);
            spawnSettingParticles(be, partialTick);
        }

        if (be.isReady() && be.getFluidMaterialName() != null) {
            renderFlatItem(be, poseStack, bufferSource, packedLight, packedOverlay, 0.8f);
        }
    }

    private float getYRot(Direction facing) {
        return switch (facing) {
            case SOUTH -> 0f;
            case EAST -> 90f;
            case NORTH -> 180f;
            case WEST -> 270f;
            case UP, DOWN -> 0f;
        };
    }

    // ── MOLD ──────────────────────────────────────────────────────────────────

    private void renderMold(CastingStationBlockEntity be, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        String moldId = be.getMoldComponentId();
        ToolComponentType type = ToolComponentType.fromId(moldId);
        DeferredHolder<Block, ?> moldHolder = null;

        if (moldId != null && moldId.equals("ingot")) {
            moldHolder = be.isBronzeMold() ? ModBlocks.BRONZE_INGOT_MOLD : ModBlocks.INGOT_MOLD;
        } else if (type != null) {
            moldHolder = be.isBronzeMold() ? ModBlocks.BRONZE_MOLD_BLOCKS.get(type) : ModBlocks.MOLD_BLOCKS.get(type);
        }

        if (moldHolder == null) return;

        Direction facing = be.getBlockState().getValue(CastingStationBlock.FACING);

        poseStack.pushPose();
        poseStack.translate(0.5f, 0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(getYRot(facing)));
        poseStack.translate(-0.5f, 0f, -0.5f);
        poseStack.translate(0.5f, BASIN_FLOOR_Y + 0.01f, 0.5f);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(0.85f, 0.85f, 0.85f);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                new ItemStack(moldHolder.get().asItem()), ItemDisplayContext.FIXED,
                packedLight, packedOverlay, poseStack, bufferSource, be.getLevel(), 0);

        poseStack.popPose();
    }

    // ── BUCKET ────────────────────────────────────────────────────────────────

    private void renderBucket(CastingStationBlockEntity be, PoseStack poseStack,
                              MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        Direction facing = be.getBlockState().getValue(CastingStationBlock.FACING);

        poseStack.pushPose();

        poseStack.translate(0.5f, 0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(getYRot(facing)));
        poseStack.translate(-0.5f, 0f, -0.5f);

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

    // ── FLUID ─────────────────────────────────────────────────────────────────

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

    // ── FLAT ITEM ─────────────────────────────────────────────────────────────

    private void renderFlatItem(CastingStationBlockEntity be, PoseStack poseStack,
                                MultiBufferSource bufferSource, int packedLight,
                                int packedOverlay, float brightness) {
        AlloyMaterial mat = AlloyMaterial.fromName(be.getFluidMaterialName());
        if (mat == null) return;

        ItemStack output = getOutputItem(be, mat);
        if (output.isEmpty()) return;

        int adjustedLight = scalePackedLight(brightness);
        Direction facing = be.getBlockState().getValue(CastingStationBlock.FACING);

        poseStack.pushPose();

        // Rotate so item faces the faucet
        poseStack.translate(0.5f, 0f, 0.5f);
        poseStack.mulPose(Axis.YP.rotationDegrees(getYRot(facing)));
        poseStack.translate(-0.5f, 0f, -0.5f);

        String moldId = be.getMoldComponentId();

        float translateX = 0.5f;
        float translateY = BASIN_FLOOR_Y + 0.02f;
        float translateZ = 0.5f;
        float scale = 0.8f;

        if (moldId != null) {
            switch (moldId) {
                case "ingot" -> { translateX = 0.5f; translateZ = 0.5f; scale = 0.8f; }
                case "core" -> { translateX = 0.5f; translateZ = 0.5f; scale = 0.9f; }
                case "pickaxe_head" -> { translateX = 0.65f; translateZ = 0.42f; scale = 1.0f; }
                case "axe_head" -> { translateX = 0.65f; translateZ = 0.35f; scale = 1.0f; }
                case "shovel_head" -> { translateX = 0.75f; translateZ = 0.3f; scale = 1.0f; }
                case "katana_blade_head" -> { translateX = 0.7f; translateZ = 0.35f; scale = 1f; }
                case "broadsword_blade_head" -> { translateX = 0.7f; translateZ = 0.35f; scale = 1f; }
                case "dagger_blade_head" -> { translateX = 0.65f; translateZ = 0.35f; scale = 1f; }
                case "tool_rod" -> { translateX = 0.45f; translateZ = 0.6f; scale = 1f; }
                case "shovel_tool_rod" -> { translateX = 0.4f; translateZ = 0.65f; scale = 1f; }
                case "katana_hilt_rod" -> { translateX = 0.25f; translateZ = 0.75f; scale = 1f; }
                case "broadsword_hilt_rod" -> { translateX = 0.35f; translateZ = 0.69f; scale = 1f; }
                case "dagger_hilt_rod" -> { translateX = 0.3f; translateZ = 0.69f; scale = 1f; }
                default -> { translateX = 0.5f; translateZ = 0.5f; scale = 0.8f; }
            }
        }

        poseStack.translate(translateX, translateY, translateZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                output, ItemDisplayContext.FIXED, adjustedLight, packedOverlay,
                poseStack, bufferSource, be.getLevel(), 0);

        poseStack.popPose();
    }

    private int scalePackedLight(float brightness) {
        int lightLevel = Math.max(0, Math.min(15, (int)(brightness * 15)));
        return LightTexture.pack(lightLevel, lightLevel);
    }

    private void spawnSettingParticles(CastingStationBlockEntity be, float partialTick) {
        if (be.getLevel() == null || !be.getLevel().isClientSide) return;
        if (RANDOM.nextFloat() > 0.05f) return;

        boolean spawnFlame = RANDOM.nextFloat() > 0.8f;

        double x = be.getBlockPos().getX() + 0.25 + RANDOM.nextDouble() * 0.5;
        double y = be.getBlockPos().getY() + BASIN_FLOOR_Y + 0.05;
        double z = be.getBlockPos().getZ() + 0.25 + RANDOM.nextDouble() * 0.5;

        be.getLevel().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);
        if (spawnFlame) {
            be.getLevel().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.005, 0);
        }
    }

    private ItemStack getOutputItem(CastingStationBlockEntity be, AlloyMaterial mat) {
        if (be.getMode() == CastingStationBlockEntity.CastMode.BUCKET) {
            var bucketHolder = ModItems.ALLOY_BUCKETS.get(mat);
            return bucketHolder != null ? new ItemStack(bucketHolder.get()) : ItemStack.EMPTY;
        }

        String moldId = be.getMoldComponentId();
        if (moldId == null) return ItemStack.EMPTY;

        if (moldId.equals("ingot")) {
            return switch (mat) {
                case COPPER -> new ItemStack(Items.COPPER_INGOT);
                case IRON -> new ItemStack(Items.IRON_INGOT);
                case GOLD -> new ItemStack(Items.GOLD_INGOT);
                case DIAMOND -> new ItemStack(Items.DIAMOND);
                case NETHERITE -> new ItemStack(Items.NETHERITE_INGOT);
                case COAL -> new ItemStack(Items.COAL);
                default -> ItemStackHelper.getAlloyStack(mat);
            };
        }

        ToolComponentType type = ToolComponentType.fromId(moldId);
        if (type == null) return ItemStack.EMPTY;

        var componentMap = ModItems.TOOL_COMPONENTS.get(mat);
        if (componentMap == null) return ItemStack.EMPTY;

        var holder = componentMap.get(type);
        return holder != null ? new ItemStack(holder.get()) : ItemStack.EMPTY;
    }
}