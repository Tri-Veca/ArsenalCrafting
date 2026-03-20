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
import java.util.List;

public class AlloySmelterScreen extends AbstractContainerScreen<AlloySmelterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/alloy_smelter_gui.png");

    private static final int FLUID_TANK_X = 151;
    private static final int FLUID_TANK_Y = 17;
    private static final int FLUID_TANK_WIDTH = 16;
    private static final int FLUID_TANK_HEIGHT = 60;

    public AlloySmelterScreen(AlloySmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Alloy Smelter"));
        this.imageHeight = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        renderFluidTank(guiGraphics, x, y);

        if (menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 110, y + 34, 176, 0, menu.getScaledProgress(), 17);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);

        // Save and set proper rendering state
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        super.render(guiGraphics, mouseX, mouseY, delta);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        int lastTexture = RenderSystem.getShaderTexture(0);
        //renderFluidTank(guiGraphics, x, y);
        RenderSystem.setShaderTexture(0, lastTexture);
        RenderSystem.enableDepthTest();

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderFluidTankTooltip(guiGraphics, mouseX, mouseY);
    }

    private void renderFluidTank(GuiGraphics guiGraphics, int x, int y) {
        FluidStack fluidStack = menu.getFluid();
        int amount = menu.getFluidAmount();
        int capacity = menu.getFluidCapacity();

        if (!fluidStack.isEmpty() && amount > 0 && capacity > 0) {
            var fluid = fluidStack.getFluid();
            var fluidExtensions = IClientFluidTypeExtensions.of(fluid);

            ResourceLocation stillTexture = fluidExtensions.getStillTexture(fluidStack);
            if (stillTexture == null) {
                stillTexture = fluidExtensions.getStillTexture();
            }

            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(stillTexture);

            float percentage = (float) amount / capacity;
            int fluidHeight = (int) (FLUID_TANK_HEIGHT * percentage);
            int fluidY = y + FLUID_TANK_Y + (FLUID_TANK_HEIGHT - fluidHeight);

            int color = fluidExtensions.getTintColor(fluidStack);
            float r = ((color >> 16) & 0xFF) / 255f;
            float g = ((color >> 8) & 0xFF) / 255f;
            float b = (color & 0xFF) / 255f;
            float a = ((color >> 24) & 0xFF) / 255f;

            float lastR = RenderSystem.getShaderColor()[0];
            float lastG = RenderSystem.getShaderColor()[1];
            float lastB = RenderSystem.getShaderColor()[2];
            float lastA = RenderSystem.getShaderColor()[3];

            RenderSystem.disableDepthTest();
            RenderSystem.setShaderColor(r, g, b, a);

            guiGraphics.blit(
                    x + FLUID_TANK_X,
                    fluidY ,
                    0,
                    FLUID_TANK_WIDTH,
                    fluidHeight - 4,
                    sprite
            );

            RenderSystem.setShaderColor(lastR, lastG, lastB, lastA);
            RenderSystem.enableDepthTest();
            RenderSystem.setShaderTexture(0, TEXTURE);
        }
    }

    private void renderFluidTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        if (mouseX >= x + FLUID_TANK_X && mouseX <= x + FLUID_TANK_X + FLUID_TANK_WIDTH &&
                mouseY >= y + FLUID_TANK_Y && mouseY <= y + FLUID_TANK_Y + FLUID_TANK_HEIGHT) {

            List<Component> tooltip = new ArrayList<>();

            FluidStack fluidStack = menu.getFluid();
            int amount = menu.getFluidAmount();
            int capacity = menu.getFluidCapacity();
            int tankCount = menu.getConnectedTanksCount();

            NumberFormat formatter = NumberFormat.getIntegerInstance();
            String amountStr = formatter.format(amount);
            String capacityStr = formatter.format(capacity);

            if (fluidStack.isEmpty() || amount == 0) {
                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.empty")
                        .withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.capacity",
                        capacityStr).withStyle(ChatFormatting.DARK_GRAY));
            } else {
                Component fluidName = fluidStack.getHoverName();
                tooltip.add(fluidName.copy().withStyle(ChatFormatting.GOLD));

                float percentage = (amount * 100f) / capacity;
                String percentStr = String.format("%.1f", percentage);

                tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.amount",
                                amountStr, capacityStr, percentStr)
                        .withStyle(ChatFormatting.GREEN));
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

}