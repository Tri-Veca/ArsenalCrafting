package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.mojang.blaze3d.systems.RenderSystem;
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

public class AlloySmelterScreen extends AbstractContainerScreen<AlloySmelterMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/alloy_smelter_gui.png");

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

        // Draw main background
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        var fluidStack = menu.blockEntity.FLUID_TANK.getFluid();

        if (!fluidStack.isEmpty()) {
            var fluid = fluidStack.getFluid();

            var fluidExtensions = IClientFluidTypeExtensions.of(fluid);

            TextureAtlasSprite sprite = Minecraft.getInstance()
                    .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .apply(fluidExtensions.getStillTexture());

            int fluidHeight = menu.getScaledFluid();

            guiGraphics.blit(
                    x + 151,                         // align to the top-left X of the container
                    y + 17 + (60 - fluidHeight),     // Y starts at top, goes down as fluid fills
                    0,                                // texture Y offset (start of the fluid sprite)
                    16,                              // width of the fluid (matches container)
                    fluidHeight - 4,                     // height based on fluid amount
                    sprite                            // the fluid texture sprite
            );

        }

        // Draw progress arrow
        if (menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 110, y + 34, 176, 0, menu.getScaledProgress(), 17);
        }

        // Draw fuel indicator
        guiGraphics.blit(TEXTURE, x + 89, y + 36 + 14 - menu.getScaledFuelProgress(),
                176, 31 - menu.getScaledFuelProgress(), 14, menu.getScaledFuelProgress());




    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}