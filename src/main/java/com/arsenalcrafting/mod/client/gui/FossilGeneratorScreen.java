package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.FossilGeneratorMenu;
import com.arsenalcrafting.mod.power.PowerTier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * Fossil Generator GUI screen.
 *
 * Texture layout (from pixel analysis):
 *   GUI size:         176 × 165
 *   Fuel slot:        x=80,  y=38  (16×16)
 *   Empty bar:        x=133, y=16  (width=16, height=57) — left border at x=133
 *   Filled bar src:   x=176, y=1   (width=15, height=58) — in transparent region
 *   Bar fills:        bottom to top
 *
 *   Inventory grid:   x=8,   y=84  (3 rows × 9 cols)
 *   Hotbar:           x=8,   y=142
 */
public class FossilGeneratorScreen extends AbstractContainerScreen<FossilGeneratorMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/fossil_generator_gui.png");

    // ── LAYOUT CONSTANTS ──────────────────────────────────────────────────────

    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 165;

    // Energy bar — empty outline in main GUI
    private static final int BAR_X       = 134; // inner left edge (x=133 is border)
    private static final int BAR_Y       = 17;  // inner top edge  (y=16 is border)
    private static final int BAR_WIDTH   = 16;
    private static final int BAR_HEIGHT  = 56;  // y=16 to y=72 = 56px inner

    // Filled bar source in texture transparent region (x=176, y=1, 15×58)
    private static final int BAR_SRC_U   = 176;
    private static final int BAR_SRC_V   = 1;
    private static final int BAR_SRC_W   = 16;
    private static final int BAR_SRC_H   = 56;

    // Fuel slot hover area
    private static final int FUEL_X      = 80;
    private static final int FUEL_Y      = 38;
    private static final int FUEL_SIZE   = 16;

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────

    public FossilGeneratorScreen(FossilGeneratorMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth      = GUI_WIDTH;
        this.imageHeight     = GUI_HEIGHT;
        this.titleLabelY     = 6;
        this.inventoryLabelY = 72;
    }

    // ── RENDER ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(gfx, mouseX, mouseY, partialTick);
        super.render(gfx, mouseX, mouseY, partialTick);
        this.renderTooltip(gfx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics gfx, float partial, int mouseX, int mouseY) {
        // Draw base texture
        gfx.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        float energyFrac = menu.getEnergyFraction();
        int energy       = menu.getEnergyStored();
        int max          = menu.getMaxEnergy();

        // ── ENERGY BAR — fills bottom to top ─────────────────────────────────
        if (energy > 0 && energyFrac > 0f) {
            // How many pixels to blit from the bottom of the source
            int blitH = (int) (BAR_SRC_H * energyFrac);
            if (blitH > 0) {
                // Offset into source: start from bottom of bar
                int srcVOffset = BAR_SRC_H - blitH;
                // Destination: bottom of bar area, offset up by blitH
                int destY = topPos + BAR_Y + BAR_HEIGHT - blitH;

                gfx.blit(TEXTURE,
                        leftPos + BAR_X,   // dest x
                        destY,             // dest y (bottom-anchored)
                        BAR_SRC_U,         // src u
                        BAR_SRC_V + srcVOffset, // src v (offset from bottom)
                        BAR_WIDTH,         // width
                        blitH              // height
                );
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        gfx.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY, 0x404040, false);
        gfx.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY, 0x404040, false);
    }

    // ── TOOLTIP ───────────────────────────────────────────────────────────────

    @Override
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderTooltip(gfx, mouseX, mouseY);

        // Hover over energy bar
        if (isHovering(BAR_X, BAR_Y, BAR_WIDTH, BAR_HEIGHT, mouseX, mouseY)) {
            int energy  = menu.getEnergyStored();
            int max     = menu.getMaxEnergy();
            float pct   = max > 0 ? (energy * 100f / max) : 0f;
            PowerTier tier = menu.getBlockEntity().getTier();

            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("⚡ Energy — " + tier.name())
                    .withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal(
                            String.format("%,d / %,d FE (%.1f%%)", energy, max, pct))
                    .withStyle(pct > 60 ? ChatFormatting.GREEN
                            : pct > 25 ? ChatFormatting.YELLOW
                            : ChatFormatting.RED));
            tooltip.add(Component.literal(
                            "Output: " + tier.generatorOutput() + " FE/t")
                    .withStyle(ChatFormatting.AQUA));

            // Show burn status
            if (menu.isBurning()) {
                float burnFrac = menu.getBurnFraction();
                tooltip.add(Component.literal(
                                String.format("Burning: %.0f%% remaining", burnFrac * 100f))
                        .withStyle(ChatFormatting.GOLD));
            } else {
                tooltip.add(Component.literal("Not burning — insert fuel")
                        .withStyle(ChatFormatting.GRAY));
            }

            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }

        // Hover over fuel slot (when empty — slot tooltip handles items automatically)
        if (isHovering(FUEL_X, FUEL_Y, FUEL_SIZE, FUEL_SIZE, mouseX, mouseY)
                && !menu.getSlot(0).hasItem()) {
            List<Component> tooltip = new ArrayList<>();
            tooltip.add(Component.literal("Fuel Slot")
                    .withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.literal("Accepts: Coal, Charcoal, Coal Block, Bucket of Hydrocarbon")
                    .withStyle(ChatFormatting.GRAY));
            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }
}