package com.arsenalcrafting.mod.client.render;

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

        int adjustedLight = scalePackedLight(brightness);

        poseStack.pushPose();

        String moldId = be.getMoldComponentId();

        // Default position (center of basin)
        float translateX = 0.5f;
        float translateY = BASIN_FLOOR_Y + 0.02f;
        float translateZ = 0.5f;
        float scale = 0.8f;

        if (moldId != null) {
            switch (moldId) {
                case "ingot":
                    translateX = 0.5f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.5f;
                    scale = 0.8f;
                    break;

                case "core":
                    translateX = 0.5f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.5f;
                    scale = 0.9f;
                    break;

                case "pickaxe_head":
                    translateX = 0.65f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.42f;
                    scale = 1.0f;
                    break;

                case "axe_head":
                    translateX = 0.65f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.35f;
                    scale = 1.0f;
                    break;

                case "shovel_head":
                    translateX = 0.75f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.3f;
                    scale = 1.0f;
                    break;

                case "katana_blade_head":
                    translateX = 0.7f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.35f;
                    scale = 1f;
                    break;

                case "broadsword_blade_head":
                    translateX = 0.7f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.35f;
                    scale = 1f;
                    break;

                case "dagger_blade_head":
                    translateX = 0.65f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.35f;
                    scale = 1f;
                    break;

                case "tool_rod":
                    translateX = 0.45f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.6f;
                    scale = 1f;
                    break;

                case "shovel_tool_rod":
                    translateX = 0.4f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.65f;
                    scale = 1f;
                    break;

                case "katana_hilt_rod":
                    translateX = 0.25f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.75f;
                    scale = 1f;
                    break;

                case "broadsword_hilt_rod":
                    translateX = 0.35f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.69f;
                    scale = 1f;
                    break;

                case "dagger_hilt_rod":
                    translateX = 0.3f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.69f;
                    scale = 1f;
                    break;

                default:
                    translateX = 0.5f;
                    translateY = BASIN_FLOOR_Y + 0.02f;
                    translateZ = 0.5f;
                    scale = 0.8f;
                    break;
            }
        }

        // Apply translations
        poseStack.translate(translateX, translateY, translateZ);
        poseStack.mulPose(Axis.XP.rotationDegrees(90f));
        poseStack.scale(scale, scale, scale);

        Minecraft.getInstance().getItemRenderer().renderStatic(
                output, ItemDisplayContext.FIXED, adjustedLight, packedOverlay,
                poseStack, bufferSource, be.getLevel(), 0);

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

        // Reduced from 0.15f to 0.05f (about 1/3 as many particles)
        if (RANDOM.nextFloat() > 0.05f) return;

        // Only spawn flame particles occasionally, not with every smoke
        boolean spawnFlame = RANDOM.nextFloat() > 0.8f; // Only 20% of smoke particles get flame

        double x = be.getBlockPos().getX() + 0.25 + RANDOM.nextDouble() * 0.5;
        double y = be.getBlockPos().getY() + BASIN_FLOOR_Y + 0.05;
        double z = be.getBlockPos().getZ() + 0.25 + RANDOM.nextDouble() * 0.5;

        // Smoke particles (always)
        be.getLevel().addParticle(ParticleTypes.SMOKE, x, y, z, 0, 0.01, 0);

        // Occasional flame particle (reduced)
        if (spawnFlame) {
            be.getLevel().addParticle(ParticleTypes.FLAME, x, y, z, 0, 0.005, 0);
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
            // Return vanilla ingot for vanilla materials, modded ingot for modded materials
            switch (mat) {
                case COPPER: return new ItemStack(Items.COPPER_INGOT);
                case IRON: return new ItemStack(Items.IRON_INGOT);
                case GOLD: return new ItemStack(Items.GOLD_INGOT);
                case DIAMOND: return new ItemStack(Items.DIAMOND);
                case NETHERITE: return new ItemStack(Items.NETHERITE_INGOT);
                case COAL: return new ItemStack(Items.COAL);
                default: return ItemStackHelper.getAlloyStack(mat);
            }
        }

        ToolComponentType type = ToolComponentType.fromId(moldId);
        if (type == null) return ItemStack.EMPTY;

        var componentMap = ModItems.TOOL_COMPONENTS.get(mat);
        if (componentMap == null) return ItemStack.EMPTY;

        var holder = componentMap.get(type);
        return holder != null ? new ItemStack(holder.get()) : ItemStack.EMPTY;
    }
}