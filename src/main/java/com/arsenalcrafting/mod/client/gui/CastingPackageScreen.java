package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.CastingPackageMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

public class CastingPackageScreen extends AbstractContainerScreen<CastingPackageMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/package_gui.png");

    private static final int IMAGE_WIDTH = 176;
    private static final int IMAGE_HEIGHT = 166;

    // Match these to your menu's values
    private static final int SLOT_START_X = 8;
    private static final int SLOT_START_Y = 30;
    private static final int SLOT_SIZE = 18;
    private static final int SLOTS_PER_ROW = 9;

    public CastingPackageScreen(CastingPackageMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Draw the main GUI background
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Draw slot backgrounds from the GUI texture (x=176, y=1)
        int requiredMolds = menu.getTotalRequired();
        for (int i = 0; i < requiredMolds; i++) {
            int row = i / SLOTS_PER_ROW;
            int col = i % SLOTS_PER_ROW;

            int x = leftPos + SLOT_START_X + (col * SLOT_SIZE);
            int y = topPos + SLOT_START_Y + (row * SLOT_SIZE);

            // Blit the slot from texture at x=176, y=1 (18x18)
            guiGraphics.blit(TEXTURE, x, y, 176, 1, SLOT_SIZE, SLOT_SIZE);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        String title = menu.getPackageType().getDisplayName();
        guiGraphics.drawString(font, title,
                (imageWidth - font.width(title)) / 2,
                6,
                0x404040, false);
    }
}