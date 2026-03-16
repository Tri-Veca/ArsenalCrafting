package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity;
import com.arsenalcrafting.mod.client.menu.PowerBankMenu;
import com.arsenalcrafting.mod.power.PowerTier;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.network.SetFaceModePacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Power Bank GUI screen.
 *
 * Layout (from texture analysis):
 *   GUI size:        176 × 196
 *   Battery icon:    x=43–129, y=27–56 (border), fill area x=48–124, y=32–51
 *   Inventory grid:  x=7, y=83 (3×9 player inv + hotbar)
 *   Energy bar:      x=7–66, y=167–177 (bottom fill bar)
 *
 * The battery icon fill is tinted with the tier material color.
 * The bottom bar fills left-to-right based on energy %.
 * Hovering the battery or bottom bar shows the energy tooltip.
 */
public class PowerBankScreen extends AbstractContainerScreen<PowerBankMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/power_bank_gui.png");

    // ── LAYOUT CONSTANTS (pixel-perfect from texture analysis) ────────────────

    // Full GUI dimensions — GUI content ends at y=164
    private static final int GUI_WIDTH  = 176;
    private static final int GUI_HEIGHT = 165;

    // Battery icon border bounds (for hover detection)
    private static final int BATTERY_X      = 43;
    private static final int BATTERY_Y      = 27;
    private static final int BATTERY_WIDTH  = 87;
    private static final int BATTERY_HEIGHT = 30;

    // Battery fill area (inside the battery border)
    private static final int BATTERY_FILL_X      = 48;
    private static final int BATTERY_FILL_Y      = 32;
    private static final int BATTERY_FILL_WIDTH  = 76;
    private static final int BATTERY_FILL_HEIGHT = 19;

    // Empty bar position in main GUI (y=163-164, x=7 to ~169)
    private static final int BAR_X      = 7;
    private static final int BAR_Y      = 155;  // just above GUI bottom
    private static final int BAR_WIDTH  = 60;
    private static final int BAR_HEIGHT = 8;

    // Filled bar SOURCE in texture transparent region (y=180-201, x=7-79)
    private static final int BAR_SRC_X  = 7;
    private static final int BAR_SRC_Y  = 180;
    private static final int BAR_SRC_W  = 72;  // full width of filled bar in texture
    private static final int BAR_SRC_H  = 10;

    // Face buttons — cross layout left of battery (x=7, y=14)
    // Each button 14x14. Cross: UP center-top, LEFT/CENTER/RIGHT middle, DOWN center-bottom
    //         [UP ]
    //   [W  ] [   ] [E  ]
    //         [DN ]
    // Plus North/South stacked below
    private static final int BTN_SIZE = 14;
    private static final int BTN_GAP  = 1;
    private static final int BTN_ORIGIN_X = 7;   // left edge of cross
    private static final int BTN_ORIGIN_Y = 14;  // top of cross

    private final Map<Direction, Button> faceButtons = new EnumMap<>(Direction.class);

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────

    public PowerBankScreen(PowerBankMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth  = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.titleLabelY     = 6;
        this.inventoryLabelY = 72;
    }

    @Override
    protected void init() {
        super.init();
        // Build face buttons in a cross layout
        // Cross center column x = BTN_ORIGIN_X + BTN_SIZE + BTN_GAP
        int cx = leftPos + BTN_ORIGIN_X + BTN_SIZE + BTN_GAP; // center column
        int rx = cx + BTN_SIZE + BTN_GAP;                      // right column
        int lx = leftPos + BTN_ORIGIN_X;                       // left column

        int row0 = topPos + BTN_ORIGIN_Y;                      // top row (UP)
        int row1 = row0 + BTN_SIZE + BTN_GAP;                  // middle row
        int row2 = row1 + BTN_SIZE + BTN_GAP;                  // bottom row (DOWN)
        int row3 = row2 + BTN_SIZE + BTN_GAP;                  // extra row (NORTH)
        int row4 = row3 + BTN_SIZE + BTN_GAP;                  // extra row (SOUTH)

        addFaceButton(Direction.UP,    cx, row0);
        addFaceButton(Direction.WEST,  lx, row1);
        addFaceButton(Direction.EAST,  rx, row1);
        addFaceButton(Direction.DOWN,  cx, row2);
        addFaceButton(Direction.NORTH, cx, row3);
        addFaceButton(Direction.SOUTH, cx, row4);
    }

    private void addFaceButton(Direction dir, int x, int y) {
        Button btn = Button.builder(
                getFaceLabel(dir, menu.getBlockEntity().getFaceMode(dir)),
                b -> onFaceButtonClick(dir)
        ).pos(x, y).size(BTN_SIZE, BTN_SIZE).build();
        faceButtons.put(dir, btn);
        addRenderableWidget(btn);
    }

    private void onFaceButtonClick(Direction dir) {
        // Send packet to server — server cycles the mode and saves
        PacketHandler.sendToServer(
                new SetFaceModePacket(menu.getBlockEntity().getBlockPos(), dir));

        // Also cycle locally for immediate visual feedback
        menu.getBlockEntity().cycleFaceMode(dir);
        Button btn = faceButtons.get(dir);
        if (btn != null) {
            btn.setMessage(getFaceLabel(dir, menu.getBlockEntity().getFaceMode(dir)));
        }
    }

    private Component getFaceLabel(Direction dir, com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity.FaceMode mode) {
        String dirChar = switch (dir) {
            case UP    -> "U";
            case DOWN  -> "D";
            case NORTH -> "N";
            case SOUTH -> "S";
            case EAST  -> "E";
            case WEST  -> "W";
        };
        return switch (mode) {
            case IN       -> Component.literal(dirChar).withStyle(ChatFormatting.GREEN);
            case OUT      -> Component.literal(dirChar).withStyle(ChatFormatting.RED);
            case DISABLED -> Component.literal(dirChar).withStyle(ChatFormatting.DARK_GRAY);
        };
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

        float fill = menu.getFillFraction();
        int energy = menu.getEnergyStored();
        int max    = menu.getMaxEnergy();

        if (max <= 0) return;

        // Get tier color from the block entity
        int color = getTierColor();
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8)  & 0xFF) / 255.0f;
        float b = ((color)       & 0xFF) / 255.0f;

        // ── BATTERY ICON FILL (colored rect, drawn over texture) ────────────
        // Only draw if there is actually stored energy
        int fillWidth = (int) (BATTERY_FILL_WIDTH * fill);
        if (fillWidth > 0 && energy > 0) {
            int argb = (0xCC << 24)
                    | (((int)(r * 255)) << 16)
                    | (((int)(g * 255)) << 8)
                    |  ((int)(b * 255));
            gfx.fill(
                    leftPos + BATTERY_FILL_X,
                    topPos  + BATTERY_FILL_Y,
                    leftPos + BATTERY_FILL_X + fillWidth,
                    topPos  + BATTERY_FILL_Y + BATTERY_FILL_HEIGHT,
                    argb
            );
        }

        // ── BOTTOM ENERGY BAR — blit from texture's transparent region ────────
        // Source: filled red bar at y=180 in texture, scaled by fill fraction
        int barBlitW = (int) (BAR_SRC_W * fill);
        if (barBlitW > 0 && energy > 0) {
            gfx.blit(TEXTURE,
                    leftPos + BAR_X,          // dest x
                    topPos  + BAR_Y,          // dest y
                    BAR_SRC_X,                // source u (x in texture)
                    BAR_SRC_Y,                // source v (y in texture)
                    barBlitW,                 // blit width (fill fraction of full bar)
                    BAR_SRC_H                 // blit height
            );
        }
    }

    @Override
    protected void renderLabels(GuiGraphics gfx, int mouseX, int mouseY) {
        // Draw "Inventory" label above the player inventory grid
        gfx.drawString(this.font, this.playerInventoryTitle,
                this.inventoryLabelX, this.inventoryLabelY,
                0x404040, false);
    }

    // ── TOOLTIP ───────────────────────────────────────────────────────────────

    @Override
    protected void renderTooltip(GuiGraphics gfx, int mouseX, int mouseY) {
        super.renderTooltip(gfx, mouseX, mouseY);

        boolean hoverBattery = isHovering(BATTERY_X, BATTERY_Y,
                BATTERY_WIDTH, BATTERY_HEIGHT, mouseX, mouseY);
        boolean hoverBar     = isHovering(BAR_X, BAR_Y,
                BAR_SRC_W, BAR_SRC_H, mouseX, mouseY);

        // Face button tooltips
        for (Map.Entry<Direction, Button> entry : faceButtons.entrySet()) {
            Button btn = entry.getValue();
            Direction dir = entry.getKey();
            if (btn.isHovered()) {
                var mode = menu.getBlockEntity().getFaceMode(dir);
                List<Component> tt = new ArrayList<>();
                tt.add(Component.literal(dir.getName().toUpperCase() + " face")
                        .withStyle(ChatFormatting.WHITE));
                tt.add(switch (mode) {
                    case IN       -> Component.literal("▶ IN — receives energy")
                            .withStyle(ChatFormatting.GREEN);
                    case OUT      -> Component.literal("◀ OUT — sends energy")
                            .withStyle(ChatFormatting.RED);
                    case DISABLED -> Component.literal("✕ DISABLED")
                            .withStyle(ChatFormatting.DARK_GRAY);
                });
                tt.add(Component.literal("Click to cycle: IN → OUT → DISABLED")
                        .withStyle(ChatFormatting.GRAY));
                gfx.renderComponentTooltip(this.font, tt, mouseX, mouseY);
                return;
            }
        }

        if (hoverBattery || hoverBar) {
            int energy   = menu.getEnergyStored();
            int max      = menu.getMaxEnergy();
            int rate     = menu.getTransferRate();
            float pct    = max > 0 ? (energy * 100f / max) : 0f;
            PowerTier tier = menu.getBlockEntity().getTier();

            List<Component> tooltip = new ArrayList<>();

            tooltip.add(Component.literal("⚡ Power Bank — " + tier.name())
                    .withStyle(ChatFormatting.YELLOW, ChatFormatting.BOLD));

            tooltip.add(Component.literal(
                            String.format("Stored: %,d / %,d FE (%.1f%%)", energy, max, pct))
                    .withStyle(getEnergyColor(pct)));

            tooltip.add(Component.literal(
                            String.format("Transfer Rate: %,d FE/t", rate))
                    .withStyle(ChatFormatting.AQUA));

            tooltip.add(Component.literal(
                            "IN / OUT per face: " + rate + " FE/t")
                    .withStyle(ChatFormatting.GRAY));

            gfx.renderComponentTooltip(this.font, tooltip, mouseX, mouseY);
        }
    }

    // ── HELPERS ───────────────────────────────────────────────────────────────

    /** Returns the tier material color for fill tinting. */
    private int getTierColor() {
        PowerTier tier = menu.getBlockEntity().getTier();
        return switch (tier) {
            case COPPER   -> 0xE07040; // warm copper
            case IRON     -> 0xC0C0C0; // silver
            case BRONZE   -> 0xCB9580; // bronze
            case STEEL    -> 0x8090A0; // steel blue-grey
            case TITANIUM -> 0xA0E4FF; // titanium light blue
        };
    }

    /** Returns green/yellow/red based on energy percentage. */
    private ChatFormatting getEnergyColor(float pct) {
        if (pct > 60f) return ChatFormatting.GREEN;
        if (pct > 25f) return ChatFormatting.YELLOW;
        return ChatFormatting.RED;
    }
}