package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.ToolkitMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class ToolkitScreen extends AbstractContainerScreen<ToolkitMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "textures/gui/container/toolkit_gui.png");
    // Standard width and custom height (176x220) for the GUI content area.
    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 220;

    public ToolkitScreen(ToolkitMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        // Set the screen dimensions to match the GUI content area
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;

        // Set the standard inventory label position offset from the bottom of the content area

        this.inventoryLabelY = this.imageHeight - 148;

    }

    @Override
    protected void init() {
        super.init();

        if (this.font != null) {
            this.titleLabelX = (this.imageWidth - this.font.width(this.title)) / 2;
        } else {
            // Safe fallback position
            this.titleLabelX = 8;
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        // Standard, simple blit call. The game draws the GUI from (0,0) of the texture
        // using the defined imageWidth and imageHeight.
        guiGraphics.blit(TEXTURE,
                this.leftPos, this.topPos,
                0, 0,
                this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        this.renderBackground(guiGraphics, pMouseX, pMouseY, pPartialTick);
        super.render(guiGraphics, pMouseX, pMouseY, pPartialTick);
        this.renderTooltip(guiGraphics, pMouseX, pMouseY);
    }
}