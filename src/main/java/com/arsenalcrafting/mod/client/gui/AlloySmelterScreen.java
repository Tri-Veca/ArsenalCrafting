package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlloySmelterScreen extends AbstractContainerScreen<AlloySmelterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/alloy_smelter_gui.png");

    // ── LAVA TANK (existing — unchanged) ─────────────────────────────────────
    private static final int FLUID_TANK_X      = 151;
    private static final int FLUID_TANK_Y      = 17;
    private static final int FLUID_TANK_WIDTH  = 16;
    private static final int FLUID_TANK_HEIGHT = 60;

    // ── OUTPUT TANK (new — adjust X/Y to match your GUI texture) ─────────────
    private static final int OUTPUT_TANK_X      = 87;  // adjust to fit your GUI layout
    private static final int OUTPUT_TANK_Y      = 21;
    private static final int OUTPUT_TANK_WIDTH  = 56;
    private static final int OUTPUT_TANK_HEIGHT = 56;

    public AlloySmelterScreen(AlloySmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Alloy Smelter"));
        this.imageHeight      = 166;
        this.inventoryLabelY  = this.imageHeight - 94;
    }

    // ── RENDER BG (existing + output tank render added) ───────────────────────

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Lava tank (existing)
        renderFluidTank(guiGraphics, x, y);

        // Progress arrow (existing)
        if (menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 110, y + 34, 176, 0, menu.getScaledProgress(), 17);
        }
    }

    // ── RENDER (existing + output tank render added) ──────────────────────────

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);

        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        super.render(guiGraphics, mouseX, mouseY, delta);

        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        int lastTexture = RenderSystem.getShaderTexture(0);

        // Output tank fluid render
        renderOutputTank(guiGraphics, x, y);

        RenderSystem.setShaderTexture(0, lastTexture);
        RenderSystem.enableDepthTest();

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderFluidTankTooltip(guiGraphics, mouseX, mouseY);
        renderOutputTankTooltip(guiGraphics, mouseX, mouseY);  // new
    }

    // ── LAVA TANK RENDER (existing — unchanged) ───────────────────────────────

    private void renderFluidTank(GuiGraphics guiGraphics, int x, int y) {
        FluidStack fluidStack = menu.getFluid();
        int amount   = menu.getFluidAmount();
        int capacity = menu.getFluidCapacity();

        if (!fluidStack.isEmpty() && amount > 0 && capacity > 0) {
            var fluid           = fluidStack.getFluid();
            var fluidExtensions = IClientFluidTypeExtensions.of(fluid);

            ResourceLocation stillTexture = fluidExtensions.getStillTexture(fluidStack);
            if (stillTexture == null) stillTexture = fluidExtensions.getStillTexture();

            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(stillTexture);

            float percentage  = (float) amount / capacity;
            int fluidHeight   = (int) (FLUID_TANK_HEIGHT * percentage);
            int fluidY        = y + FLUID_TANK_Y + (FLUID_TANK_HEIGHT - fluidHeight);

            int color = fluidExtensions.getTintColor(fluidStack);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >>  8) & 0xFF) / 255f;
            float b = ( color        & 0xFF) / 255f;
            float a = ((color >> 24) & 0xFF) / 255f;

            float[] last = RenderSystem.getShaderColor();
            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(r, g, b, a);

            guiGraphics.blit(x + FLUID_TANK_X, fluidY, 0,
                    FLUID_TANK_WIDTH, fluidHeight - 4, sprite);

            RenderSystem.setShaderColor(last[0], last[1], last[2], last[3]);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderTexture(0, TEXTURE);
        }
    }

    // ── OUTPUT TANK RENDER (new) ──────────────────────────────────────────────

    private void renderOutputTank(GuiGraphics guiGraphics, int x, int y) {
        List<FluidStack> fluids = menu.getAllOutputFluids();
        Collections.reverse(fluids); // reverse so largest amounts are at bottom
        if (fluids.isEmpty()) return;

        int capacity = menu.getOutputFluidCapacity();
        int totalAmount = menu.getOutputFluidAmount();
        if (capacity <= 0 || totalAmount <= 0) return;

        int filledHeight = Math.round(OUTPUT_TANK_HEIGHT * ((float) totalAmount / capacity));
        if (filledHeight <= 0) return;

        // Start from the BOTTOM of the filled area (since we stack upward)
        int currentY = y + OUTPUT_TANK_Y + OUTPUT_TANK_HEIGHT - 4; // bottom of tank
        int usedHeight = 0;

        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);

            int fluidHeight = Math.round(filledHeight * ((float) fluid.getAmount() / totalAmount));

            if (i == fluids.size() - 1) {
                fluidHeight = filledHeight - usedHeight;
            }

            if (fluidHeight <= 0) {
                fluidHeight = 1;
            }

            // Render from bottom going up
            renderFluid(
                    guiGraphics,
                    x + OUTPUT_TANK_X,
                    currentY - fluidHeight , // Y position is top of this fluid layer
                    OUTPUT_TANK_WIDTH,
                    fluidHeight,
                    fluid
            );

            currentY -= fluidHeight; // move up for next fluid
            usedHeight += fluidHeight;
        }
    }

    private void renderFluid(GuiGraphics guiGraphics, int x, int y, int width, int height, FluidStack fluid) {
        var fluidExtensions = IClientFluidTypeExtensions.of(fluid.getFluid());
        ResourceLocation still = fluidExtensions.getStillTexture(fluid);
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(still);

        int color = fluidExtensions.getTintColor(fluid);
        float r = ((color >> 16) & 0xFF) / 255f;
        float g = ((color >> 8) & 0xFF) / 255f;
        float b = (color & 0xFF) / 255f;
        float a = ((color >> 24) & 0xFF) / 255f;

        RenderSystem.setShaderColor(r, g, b, a);
        guiGraphics.blit(x, y, 0, width, height, sprite);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // ── LAVA TANK TOOLTIP (existing — unchanged) ──────────────────────────────

    private void renderFluidTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width  - imageWidth)  / 2;
        int y = (height - imageHeight) / 2;

        if (mouseX >= x + FLUID_TANK_X && mouseX <= x + FLUID_TANK_X + FLUID_TANK_WIDTH &&
                mouseY >= y + FLUID_TANK_Y && mouseY <= y + FLUID_TANK_Y + FLUID_TANK_HEIGHT) {

            List<Component> tooltip = new ArrayList<>();
            FluidStack fluidStack   = menu.getFluid();
            int amount   = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            int tankCount = menu.getConnectedTanksCount();

            NumberFormat formatter = NumberFormat.getIntegerInstance();

            if (fluidStack.isEmpty() || amount == 0) {
                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.empty")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.capacity",
                        formatter.format(capacity)).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltip.add(fluidStack.getHoverName().copy().withStyle(ChatFormatting.GOLD));
                float pct = (amount * 100f) / capacity;
                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.amount",
                        formatter.format(amount), formatter.format(capacity),
                        String.format("%.1f", pct)).withStyle(ChatFormatting.GREEN));
            }

            if (tankCount > 0) {
                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.connected",
                        tankCount).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.no_tanks")
                        .withStyle(ChatFormatting.RED));
            }

            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }

    // ── OUTPUT TANK TOOLTIP (new) ─────────────────────────────────────────────

    private void renderOutputTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (isHovering(OUTPUT_TANK_X, OUTPUT_TANK_Y, OUTPUT_TANK_WIDTH, OUTPUT_TANK_HEIGHT, mouseX, mouseY)) {
            List<Component> tooltip = new ArrayList<>();
            List<FluidStack> fluids = menu.getAllOutputFluids();

            tooltip.add(Component.literal("Molten Output").withStyle(ChatFormatting.GOLD));

            if (fluids.isEmpty()) {
                tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
            } else {
                int total = menu.getOutputFluidAmount();
                int capacity = menu.getOutputFluidCapacity();

                for (FluidStack fluid : fluids) {
                    float percent = (fluid.getAmount() * 100f) / capacity;
                    tooltip.add(Component.literal(
                                    String.format("  %s: %d mb (%.1f%%)",
                                            fluid.getFluid().getFluidType().getDescription().getString(),
                                            fluid.getAmount(), percent))
                            .withStyle(ChatFormatting.YELLOW));
                }

                tooltip.add(Component.empty());
                tooltip.add(Component.literal(String.format("Total: %d / %d mb (%.1f%%)",
                                total, capacity, (total * 100f / capacity)))
                        .withStyle(ChatFormatting.GREEN));
            }

            guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
        }
    }
}