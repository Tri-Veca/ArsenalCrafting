package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.RepairKitMenu;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.RepairKitItem;
import com.arsenalcrafting.mod.items.tool.ArsenalTool;
import com.arsenalcrafting.mod.items.tool.powers.CoreHandlers;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.items.tool.powers.RepairReduction;
import com.arsenalcrafting.mod.registry.ModComponents;
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

    // Bar dimensions
    private static final int BAR_X = 63;
    private static final int BAR_Y = 39;
    private static final int BAR_WIDTH = 51;
    private static final int BAR_HEIGHT = 6;

    // Filled bar texture coordinates
    private static final int FILLED_BAR_U = 176;
    private static final int FILLED_BAR_V = 0;

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

            // Get material color
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
                        FILLED_BAR_U, FILLED_BAR_V,
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

        if (isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            int displayValue = menu.getCalculatedValue();
            ItemStack tool = menu.getSlot(0).getItem();
            ItemStack ingot = menu.getSlot(1).getItem();
            String material = menu.getCalculatedMaterial();
            boolean locked = RepairKitItem.isMaterialLocked(menu.kitStack);

            List<Component> tooltip = new ArrayList<>();

            // Check for material mismatch
            if (!tool.isEmpty() && !ingot.isEmpty() &&
                    tool.getItem() instanceof ArsenalTool arsenalTool) {

                String toolMaterial = arsenalTool.getMaterialName(tool);
                AlloyMaterial ingotMat = AlloyMaterial.fromItem(ingot.getItem());
                String ingotMaterial = ingotMat != null ? ingotMat.getName() : "";

                if (!toolMaterial.isEmpty() && !ingotMaterial.isEmpty() &&
                        !toolMaterial.equals(ingotMaterial)) {

                    tooltip.add(Component.literal("⚠ Material Mismatch!")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    tooltip.add(Component.literal("Tool: " + toolMaterial)
                            .withStyle(ChatFormatting.RED));
                    tooltip.add(Component.literal("Ingot: " + ingotMaterial)
                            .withStyle(ChatFormatting.RED));
                    tooltip.add(Component.literal("Cannot repair with different material")
                            .withStyle(ChatFormatting.DARK_RED));

                    gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
                    return;
                }
            }

            // Show value
            tooltip.add(Component.literal("Stored Value: " + displayValue + "/" + MAX_VALUE)
                    .withStyle(ChatFormatting.YELLOW));

            if (!material.isEmpty()) {
                tooltip.add(Component.literal("Material: " + material)
                        .withStyle(locked ? ChatFormatting.RED : ChatFormatting.GREEN));

                if (locked) {
                    tooltip.add(Component.literal("Material is locked - empty kit to change")
                            .withStyle(ChatFormatting.DARK_RED));
                }
            } else {
                tooltip.add(Component.literal("No Material Set")
                        .withStyle(ChatFormatting.GRAY));
            }

            if (tool.getItem() instanceof ArsenalTool arsenalTool) {
                float efficiency = arsenalTool.getRepairEfficiency(tool);

                // FIX THIS CHECK - ADD IT HERE
                boolean hasRepairReduction = false;
                ToolAssemblyData data = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
                if (data != null && !data.isEmpty()) {
                    AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
                    if (coreMat != null) {
                        CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat);
                        hasRepairReduction = coreDef != null && coreDef.id().equals("repair_reduction");
                    }
                }

                float repairPercentPerValue = 15.0f / efficiency; // Base repair


                if (hasRepairReduction) {
                    // Use the RepairReduction class
                    repairPercentPerValue = RepairReduction.calculateRepairPercent(efficiency);
                    tooltip.add(Component.literal("Repair Reduction Core " + CoreHandlers.RepairReductionCore.getBonusText())
                            .withStyle(ChatFormatting.GREEN));
                } else {
                    repairPercentPerValue = 15.0f / efficiency;
                }

                tooltip.add(Component.literal("1 Value = " + String.format("%.1f", repairPercentPerValue) +
                                "% of max durability")
                        .withStyle(ChatFormatting.AQUA));

            } else {
                tooltip.add(Component.literal("Insert a tool to see repair cost")
                        .withStyle(ChatFormatting.DARK_GRAY));
            }

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