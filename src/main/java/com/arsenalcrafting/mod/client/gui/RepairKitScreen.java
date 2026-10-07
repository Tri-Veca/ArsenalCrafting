package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;

import com.arsenalcrafting.mod.client.GuiDebug;
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

/**
 * RepairKitScreen — client-side display for the kit.
 *
 * No game logic; shows whatever the menu reports.
 * Bar is tinted by the loaded alloy's color so material is visible at a glance.
 */
public class RepairKitScreen extends AbstractContainerScreen<RepairKitMenu> {

    // ── CONSTANTS ──────────────────────────────────────────────────────────

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    ArsenalCrafting.MOD_ID,
                    "textures/gui/container/repair_kit_gui.png"
            );

    /** Texture coords for the bar. MAX_VALUE mirrors the 128 cap in the menu. */
    private static final int BAR_X        = 63;
    private static final int BAR_Y        = 39;
    private static final int BAR_WIDTH    = 51;
    private static final int BAR_HEIGHT   = 6;
    private static final int FILLED_BAR_U = 176;
    private static final int FILLED_BAR_V = 0;
    private static final int MAX_VALUE    = 128;

    // ── CACHE (debug only) ─────────────────────────────────────────────────

    /** Only for debug logging (skip printing unchanged lines) — no render effect. */
    private int lastBarValue = -1;
    private String lastBarMaterial = "";
    private int lastTooltipValue = -1;
    private String lastTooltipMaterial = "";
    private String lastTooltipTool = "";

    public RepairKitScreen(RepairKitMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = 176;
        this.imageHeight = 220;
        GuiDebug.open("RepairKit", "Screen opened");
    }

    @Override
    public void onClose() {
        GuiDebug.open("RepairKit", "Screen closed");
        super.onClose();
    }

    // ── BACKGROUND ────────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY) {
        gfx.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        int displayValue       = menu.getCalculatedValue();
        String displayMaterial = menu.getCalculatedMaterial();

        if (displayValue != lastBarValue || !displayMaterial.equals(lastBarMaterial)) {
            lastBarValue = displayValue;
            lastBarMaterial = displayMaterial;
            GuiDebug.render("RepairKit", "bar value=" + displayValue + " material=" + displayMaterial);
        }

        if (displayValue <= 0) return;

        float progress  = Math.min(1.0f, (float) displayValue / MAX_VALUE);
        int   fillWidth = Math.max(1, (int) (progress * BAR_WIDTH));
        int   barLeft   = leftPos + BAR_X;
        int   barTop    = topPos  + BAR_Y;

        // Bar fill tinted by alloy color; gray fallback when no material.
        AlloyMaterial mat = AlloyMaterial.fromName(displayMaterial);
        if (mat != null) {
            int   color = mat.getItemColor();
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >>  8) & 0xFF) / 255.0f;
            float b = ( color        & 0xFF) / 255.0f;
            gfx.setColor(r, g, b, 1.0f);
        } else {
            gfx.setColor(0.75f, 0.75f, 0.75f, 1.0f);
        }

        gfx.blit(TEXTURE, barLeft, barTop, FILLED_BAR_U, FILLED_BAR_V, fillWidth, BAR_HEIGHT);
        gfx.setColor(1.0f, 1.0f, 1.0f, 1.0f);
    }

    /** No text labels drawn — the GUI is icon-only. */
    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // No labels
    }

    // ── TOOLTIP ───────────────────────────────────────────────────────────────

    /** Bar tooltip; mismatch warning drawn BEFORE super so it overrides. */
    @Override
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        // Mismatch check BEFORE super so it can't be overridden
        if (isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            ItemStack tool  = menu.getSlot(0).getItem();
            ItemStack ingot = menu.getSlot(1).getItem();

            if (!tool.isEmpty() && !ingot.isEmpty()
                    && tool.getItem() instanceof ArsenalTool arsenalTool) {

                String toolMat = arsenalTool.getMaterialName(tool);
                AlloyMaterial ingotAlloy = AlloyMaterial.fromItem(ingot.getItem());
                String ingotMat = ingotAlloy != null ? ingotAlloy.getName() : "";

                if (!toolMat.isEmpty() && !ingotMat.isEmpty() && !toolMat.equals(ingotMat)) {
                    List<Component> mismatchTooltip = new ArrayList<>();
                    mismatchTooltip.add(Component.literal("⚠ Material Mismatch!")
                            .withStyle(ChatFormatting.RED, ChatFormatting.BOLD));
                    mismatchTooltip.add(Component.literal("Tool: "  + toolMat)
                            .withStyle(ChatFormatting.RED));
                    mismatchTooltip.add(Component.literal("Ingot: " + ingotMat)
                            .withStyle(ChatFormatting.RED));
                    mismatchTooltip.add(Component.literal("Cannot repair with different material")
                            .withStyle(ChatFormatting.DARK_RED));
                    gfx.renderComponentTooltip(this.font, mismatchTooltip, mouseX, mouseY);
                    return;
                }
            }
        }

        super.renderTooltip(gfx, mouseX, mouseY);

        if (!isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) return;

        int    displayValue    = menu.getCalculatedValue();
        String displayMaterial = menu.getCalculatedMaterial();
        ItemStack tool         = menu.getSlot(0).getItem();
        String toolName        = tool.isEmpty() ? "none" : tool.getHoverName().getString();

        if (displayValue != lastTooltipValue
                || !displayMaterial.equals(lastTooltipMaterial)
                || !toolName.equals(lastTooltipTool)) {
            lastTooltipValue = displayValue;
            lastTooltipMaterial = displayMaterial;
            lastTooltipTool = toolName;
            GuiDebug.render("RepairKit", "tooltip value=" + displayValue + " material=" + displayMaterial
                    + " tool=" + toolName);
        }

        List<Component> barTooltip = new ArrayList<>();

        barTooltip.add(Component.literal("Stored Value: " + displayValue + "/" + MAX_VALUE)
                .withStyle(ChatFormatting.YELLOW));

        if (!displayMaterial.isEmpty()) {
            barTooltip.add(Component.literal("Material: " + displayMaterial)
                    .withStyle(ChatFormatting.GREEN));
        } else {
            barTooltip.add(Component.literal("No Material Set")
                    .withStyle(ChatFormatting.GRAY));
        }

        if (!tool.isEmpty() && tool.getItem() instanceof ArsenalTool arsenalTool) {
            float efficiency = arsenalTool.getRepairEfficiency(tool);

            boolean hasRepairReduction = false;
            ToolAssemblyData data = tool.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
            if (data != null && !data.isEmpty()) {
                AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
                if (coreMat != null) {
                    CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat);
                    hasRepairReduction = coreDef != null && coreDef.id().equals("repair_reduction");
                }
            }

            float repairPctPerValue = hasRepairReduction
                    ? RepairReduction.calculateRepairPercent(efficiency)
                    : 15.0f / efficiency;

            if (hasRepairReduction) {
                barTooltip.add(Component.literal("Repair Reduction Core "
                                + CoreHandlers.RepairReductionCore.getBonusText())
                        .withStyle(ChatFormatting.GREEN));
            }

            barTooltip.add(Component.literal("1 Value = "
                            + String.format("%.1f", repairPctPerValue) + "% of max durability")
                    .withStyle(ChatFormatting.AQUA));
        } else {
            barTooltip.add(Component.literal("Insert a tool to see repair cost")
                    .withStyle(ChatFormatting.DARK_GRAY));
        }

        gfx.renderComponentTooltip(this.font, barTooltip, mouseX, mouseY);
    }

    // ── RENDER ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }
}