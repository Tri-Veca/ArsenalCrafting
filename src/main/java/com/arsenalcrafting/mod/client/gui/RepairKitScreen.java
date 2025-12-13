package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.RepairKitMenu;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
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

    // Bar dimensions
    private static final int BAR_X = 7;
    private static final int BAR_Y = 40;
    private static final int BAR_WIDTH = 51;
    private static final int BAR_HEIGHT = 6;

    public RepairKitScreen(RepairKitMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 220;
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY) {
        // Draw background texture
        gfx.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Get stored value
        int stored = RepairKitItem.getStoredValue(menu.kitStack);

        // Get tool to calculate cost per durability
        ItemStack tool = menu.getSlot(0).getItem();
        int costPerDurability = 0;

        if (tool.getItem() instanceof ArsenalTool arsenalTool) {
            costPerDurability = arsenalTool.getRepairValuePerDurability(tool);
        }

        // Calculate progress
        float progress = 0.0f;
        if (costPerDurability > 0) {
            progress = Math.min(1.0f, (float) stored / (float) costPerDurability);
        }

        // Calculate fill width
        int fillWidth = (int) (progress * BAR_WIDTH);

        // Get material color
        String matName = RepairKitItem.getMaterial(menu.kitStack);
        AlloyMaterial mat = AlloyMaterial.fromName(matName);
        int color = mat != null ? mat.getItemColor() : 0xFF888888; // Default gray

        // Draw filled portion of the bar (if any)
        if (fillWidth > 0) {
            int barLeft = leftPos + BAR_X;
            int barTop = topPos + BAR_Y;

            // Fill with material color
            gfx.fill(
                    barLeft,
                    barTop,
                    barLeft + fillWidth,
                    barTop + BAR_HEIGHT,
                    color | 0xFF000000
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Hide vanilla labels
    }

    @Override
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderTooltip(gfx, mouseX, mouseY);

        // Check if mouse is over the progress bar
        if (isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            int stored = RepairKitItem.getStoredValue(menu.kitStack);
            ItemStack tool = menu.getSlot(0).getItem();
            int costPerDurability = 0;

            if (tool.getItem() instanceof ArsenalTool arsenalTool) {
                costPerDurability = arsenalTool.getRepairValuePerDurability(tool);
            }

            String material = RepairKitItem.getMaterial(menu.kitStack);
            if (material.isEmpty()) {
                material = "None";
            }

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Material: " + material));
            tooltip.add(Component.literal("Stored Value: " + stored));

            if (costPerDurability > 0) {
                tooltip.add(Component.literal("Cost per Durability: " + costPerDurability));
                tooltip.add(Component.literal("Progress: " + stored + "/" + costPerDurability));
            } else {
                tooltip.add(Component.literal("Insert a tool to see repair cost"));
            }

            gfx.renderTooltip(this.font, (Component) tooltip, mouseX, mouseY);
        }
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}
