package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.RepairKitMenu;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class RepairKitScreen extends AbstractContainerScreen<RepairKitMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ArsenalCrafting.MOD_ID,
                    "textures/gui/container/repair_kit_gui.png"
            );

    // Bar dimensions - Screen positions (where to draw on screen)
    private static final int BAR_X = 64;      // Screen X position
    private static final int BAR_Y = 39;      // Screen Y position (moved up 1 pixel)
    private static final int BAR_WIDTH = 51;  // Width of the bar
    private static final int BAR_HEIGHT = 6;  // Height of the bar

    // Filled bar texture coordinates - ADJUST THESE based on your texture!
    // These are coordinates within your texture file where the filled bar graphic is
    private static final int FILLED_BAR_U = 176;  // Texture X coordinate of filled bar
    private static final int FILLED_BAR_V = 0;    // Texture Y coordinate of filled bar

    // Maximum stored value
    private static final int MAX_VALUE = 128;

    public RepairKitScreen(RepairKitMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 220;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY) {
        // Draw background texture
        gfx.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Get display value and material from menu
        int displayValue = menu.getCalculatedValue();
        String displayMaterial = menu.getCalculatedMaterial();

        // Draw filled portion of the bar
        if (displayValue > 0) {
            // Calculate progress
            float progress = Math.min(1.0f, (float) displayValue / (float) MAX_VALUE);
            int fillWidth = (int) (progress * BAR_WIDTH);

            // Ensure at least 1 pixel if we have any value
            if (fillWidth == 0 && displayValue > 0) {
                fillWidth = 1;
            }

            // Get material color for tinting
            AlloyMaterial mat = AlloyMaterial.fromName(displayMaterial);

            // Calculate screen positions
            int barLeft = leftPos + BAR_X;
            int barTop = topPos + BAR_Y;

            if (mat != null) {
                // Get material color
                int color = mat.getItemColor();

                // Extract RGB components for tinting
                float r = ((color >> 16) & 0xFF) / 255.0f;
                float g = ((color >> 8) & 0xFF) / 255.0f;
                float b = (color & 0xFF) / 255.0f;

                // Set tint color
                gfx.setColor(r, g, b, 1.0f);

                // Draw the filled bar texture (tinted with material color)
                gfx.blit(TEXTURE,
                        barLeft, barTop,
                        FILLED_BAR_U, FILLED_BAR_V,  // Draw from filled bar texture area
                        fillWidth, BAR_HEIGHT
                );

                // Reset color
                gfx.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            } else {
                // Default gray if no material
                gfx.setColor(0.75f, 0.75f, 0.75f, 1.0f); // Light gray

                // Draw the filled bar texture
                gfx.blit(TEXTURE,
                        barLeft, barTop,
                        FILLED_BAR_U, FILLED_BAR_V,
                        fillWidth, BAR_HEIGHT
                );

                // Reset color
                gfx.setColor(1.0f, 1.0f, 1.0f, 1.0f);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // No labels needed
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderTooltip(gfx, mouseX, mouseY);

        // Check if mouse is over the progress bar
        if (isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            int displayValue = menu.getCalculatedValue();
            ItemStack tool = menu.getSlot(0).getItem();
            String material = menu.getCalculatedMaterial();
            boolean locked = RepairKitItem.isMaterialLocked(menu.kitStack);

            List<Component> tooltip = new ArrayList<>();

            // Check if we're showing preview or actual value
            ItemStack ingot = menu.getSlot(1).getItem();
            if (!ingot.isEmpty()) {
                tooltip.add(Component.literal("Preview: " + displayValue + "/" + MAX_VALUE)
                        .withStyle(ChatFormatting.YELLOW));
            } else {
                tooltip.add(Component.literal("Stored Value: " + displayValue + "/" + MAX_VALUE)
                        .withStyle(ChatFormatting.YELLOW));
            }

            if (!material.isEmpty()) {
                tooltip.add(Component.literal("Material: " + material)
                        .withStyle(locked ? ChatFormatting.RED : ChatFormatting.GREEN));

                if (locked) {
                    tooltip.add(Component.literal("Material is locked")
                            .withStyle(ChatFormatting.DARK_RED));
                }
            } else {
                tooltip.add(Component.literal("No Material Set")
                        .withStyle(ChatFormatting.GRAY));
            }

            if (tool.getItem() instanceof ArsenalTool arsenalTool) {
                int cost = arsenalTool.getRepairValuePerDurability(tool);
                if (cost > 0) {
                    tooltip.add(Component.literal("Cost: " + cost + " value per durability")
                            .withStyle(ChatFormatting.GOLD));

                    int possibleRepairs = displayValue / cost;
                    if (possibleRepairs > 0) {
                        tooltip.add(Component.literal("Can repair: " + possibleRepairs + " durability")
                                .withStyle(ChatFormatting.AQUA));
                    }
                }
            }

            tooltip.add(Component.literal("1 Ingot = 2 Value")
                    .withStyle(ChatFormatting.LIGHT_PURPLE));

            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}