package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.AlloySmelterMenu;
import com.arsenalcrafting.mod.core.ItemStackHelper;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.network.*;
import com.arsenalcrafting.mod.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AlloySmelterScreen extends AbstractContainerScreen<AlloySmelterMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/alloy_smelter_gui_2.png");

    // ── TANKS ─────────────────────────────────────────────────────────────────
    private static final int FLUID_TANK_X      = 151;
    private static final int FLUID_TANK_Y      = 17;
    private static final int FLUID_TANK_WIDTH  = 16;
    private static final int FLUID_TANK_HEIGHT = 60;

    private static final int OUTPUT_TANK_X      = 87;
    private static final int OUTPUT_TANK_Y      = 21;
    private static final int OUTPUT_TANK_WIDTH  = 56;
    private static final int OUTPUT_TANK_HEIGHT = 56;

    // ── POPOUT BUTTONS ────────────────────────────────────────────────────────
    private static final int POPOUT_BUTTON_X    = 176;
    private static final int POPOUT_BUTTON_SIZE = 16;
    private static final int CONTROLS_BUTTON_Y  = 6;
    private static final int FLUID_OUT_BUTTON_Y = CONTROLS_BUTTON_Y + POPOUT_BUTTON_SIZE + 4;

    // ── POPOUT PANEL ──────────────────────────────────────────────────────────
    private static final int POPOUT_WIDTH   = 100;
    private static final int POPOUT_PADDING = 14;
    private static final int BTN_SIZE       = 16;

    // ── SMELTER CONTROLS MATERIALS ────────────────────────────────────────────
    private static final AlloyMaterial[] MELT_MATERIALS = {
            AlloyMaterial.COPPER, AlloyMaterial.IRON, AlloyMaterial.GOLD,
            AlloyMaterial.DIAMOND, AlloyMaterial.NETHERITE, AlloyMaterial.COAL,
            AlloyMaterial.TIN, AlloyMaterial.TITANIUM
    };
    private static final AlloyMaterial[] ALLOY_MATERIALS = {
            AlloyMaterial.BRONZE, AlloyMaterial.STEEL
    };

    // ── SCROLL ────────────────────────────────────────────────────────────────
    private static final int SCROLL_X           = 67;
    private static final int SCROLL_Y           = 19;
    private static final int SCROLL_TRACK_U     = 188;
    private static final int SCROLL_TRACK_V     = 151;
    private static final int SCROLL_TRACK_W     = 12;
    private static final int SCROLL_TRACK_H     = 54;
    private static final int SCROLL_THUMB_U     = 176;
    private static final int SCROLL_THUMB_V     = 150;
    private static final int SCROLL_THUMB_W     = 10;
    private static final int SCROLL_THUMB_H     = 15;
    private static final int SLOTS_PER_ROW      = 3;
    private static final int VISIBLE_ROWS       = 3;  // Show 3 rows of 3 slots = 9 slots visible
    private static final int EXTRA_SLOTS_START_X = 10;
    private static final int EXTRA_SLOTS_START_Y = 19; // 19 + 3*18 = 73

    // ── BLUE LINES ────────────────────────────────────────────────────────────
    private static final int BLUE_LINES_X = 87;
    private static final int BLUE_LINES_Y = 20;
    private static final int BLUE_LINES_U = 176;
    private static final int BLUE_LINES_V = 21;
    private static final int BLUE_LINES_W = 56;
    private static final int BLUE_LINES_H = 51;

    // ── EXTRA SLOT BACKGROUNDS ────────────────────────────────────────────────
    private static final int EXTRA_SLOT_U = 176;
    private static final int EXTRA_SLOT_V = 100;  // Far from blue lines (21) and scrollbar (1)
    private static final int EXTRA_SLOT_W = 18;
    private static final int EXTRA_SLOT_H = 18;
   // Grid position

    // ── STATE ─────────────────────────────────────────────────────────────────
    private String activePopout        = null;
    private String highlightedMaterial = null;
    private boolean highlightedIsMelt  = false;
    private String castTargetFluid     = null;
    private String highlightedFluid    = null;

    private int currentScrollRow = 0;
    private boolean isScrolling = false;

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────

    public AlloySmelterScreen(AlloySmelterMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, Component.literal("Alloy Smelter"));
        this.imageHeight     = 166;
        this.inventoryLabelY = this.imageHeight - 94;
    }

    // ── JEI EXTRA AREAS ───────────────────────────────────────────────────────

    public List<Rect2i> getGuiExtraAreas() {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        List<Rect2i> areas = new ArrayList<>();
        areas.add(new Rect2i(x + POPOUT_BUTTON_X, y + CONTROLS_BUTTON_Y,
                POPOUT_BUTTON_SIZE + 4, POPOUT_BUTTON_SIZE));
        areas.add(new Rect2i(x + POPOUT_BUTTON_X, y + FLUID_OUT_BUTTON_Y,
                POPOUT_BUTTON_SIZE + 4, POPOUT_BUTTON_SIZE));
        if (activePopout != null) {
            int popoutHeight = activePopout.equals("controls") ? getControlsHeight() : getFluidOutHeight();
            areas.add(new Rect2i(x + imageWidth + 2, y, POPOUT_WIDTH, popoutHeight));
        }
        return areas;
    }

    // ── SCROLL HELPERS ────────────────────────────────────────────────────────

    private boolean shouldShowScrollbar() {
        return menu.blockEntity.getCurrentSlotCount() > 9;
    }
    private int getTotalRows() {
        // Each "row" of scroll = 1 full page of 9
        return (int) Math.ceil((double) menu.blockEntity.getCurrentSlotCount() / 9.0);
    }

    private int getMaxScrollRows() {
        return Math.max(0, getTotalRows() - 1);
    }
    /*private void updateScrollRow(int newRow) {
        int maxRows = getMaxScrollRows();
        currentScrollRow = Math.max(0, Math.min(newRow, maxRows));
        menu.blockEntity.setScrolledRow(currentScrollRow);
        System.out.println("[Scroll] updateScrollRow → " + currentScrollRow + " (max=" + maxRows + ", slotCount=" + menu.blockEntity.getCurrentSlotCount() + ")");
    }

     */
    // ── MOUSE INPUT ───────────────────────────────────────────────────────────

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (!shouldShowScrollbar()) return false;
        int delta = (int) -Math.signum(scrollY);
        int maxRows = getMaxScrollRows();
        currentScrollRow = Mth.clamp(currentScrollRow + delta, 0, maxRows);
        menu.blockEntity.setScrolledRowClientOnly(currentScrollRow);
        System.out.println("[Scroll] row=" + currentScrollRow + "/" + maxRows + " slotCount=" + menu.blockEntity.getCurrentSlotCount());
        return true;
    }

    private int debugCounter = 0;

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        System.out.println("mouseDragged: isScrolling=" + isScrolling + " shouldShowScrollbar=" + shouldShowScrollbar());

        if (isScrolling && shouldShowScrollbar()) {
            int x = (width - imageWidth) / 2;
            int y = (height - imageHeight) / 2;
            int scrollY = y + SCROLL_Y;
            updateScrollFromMouse(mouseY, scrollY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        System.out.println("mouseClicked ENTRY - button=" + button);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int scrollX = x + SCROLL_X;
        int scrollY = y + SCROLL_Y;

        System.out.println("scrollbar area: X=" + scrollX + "-" + (scrollX + SCROLL_TRACK_W) +
                " Y=" + scrollY + "-" + (scrollY + SCROLL_TRACK_H));
        System.out.println("mouse position: " + mouseX + "," + mouseY);
        System.out.println("shouldShowScrollbar=" + shouldShowScrollbar());

        if (shouldShowScrollbar() && button == 0) {
            boolean inX = mouseX >= scrollX && mouseX <= scrollX + SCROLL_TRACK_W;
            boolean inY = mouseY >= scrollY && mouseY <= scrollY + SCROLL_TRACK_H;
            System.out.println("in X range: " + inX + ", in Y range: " + inY);

            if (inX && inY) {
                System.out.println("!!! CLICK DETECTED ON SCROLLBAR !!!");
                isScrolling = true;
                updateScrollFromMouse(mouseY, scrollY);
                return true;
            }
        }

        // Controls button
        if (isOverButton((int) mouseX, (int) mouseY, x, y, CONTROLS_BUTTON_Y)) {
            activePopout = "controls".equals(activePopout) ? null : "controls";
            if (activePopout == null) highlightedMaterial = null;
            return true;
        }

        // Fluid output button
        if (isOverButton((int) mouseX, (int) mouseY, x, y, FLUID_OUT_BUTTON_Y)) {
            activePopout = "fluidout".equals(activePopout) ? null : "fluidout";
            if (activePopout == null) highlightedFluid = null;
            return true;
        }

        if ("controls".equals(activePopout)) handleControlsClick((int) mouseX, (int) mouseY, button, x, y);
        else if ("fluidout".equals(activePopout)) handleFluidOutClick((int) mouseX, (int) mouseY, button, x, y);

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        isScrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
    private void updateScrollFromMouse(double mouseY, int scrollY) {
        float percent = (float)(mouseY - scrollY) / (SCROLL_TRACK_H - SCROLL_THUMB_H);
        percent = Mth.clamp(percent, 0f, 1f);
        int maxRows = getMaxScrollRows();
        int newRow = Math.round(percent * maxRows);
        currentScrollRow = newRow;
        menu.blockEntity.setScrolledRowClientOnly(currentScrollRow);
        System.out.println("[Scroll] drag row=" + currentScrollRow + "/" + maxRows);
    }
    private boolean handleControlsClick(int mouseX, int mouseY, int button, int x, int y) {
        int popoutX = x + imageWidth + 2;
        int popoutY = y;

        // Alloy mix buttons
        for (int i = 0; i < ALLOY_MATERIALS.length; i++) {
            AlloyMaterial mat = ALLOY_MATERIALS[i];
            int[] pos = getAlloyButtonPos(popoutX, popoutY, i);
            if (isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE)) {
                if (button == 1) {
                    highlightedMaterial = mat.getName().equals(highlightedMaterial) ? null : mat.getName();
                    highlightedIsMelt   = false;
                } else {
                    if (mat.getName().equals(highlightedMaterial)) highlightedMaterial = null;
                    PacketHandler.sendToServer(
                            new ToggleMixingPacket(menu.blockEntity.getBlockPos(), mat.getName()));
                }
                return true;
            }
        }

        // Force Mix button
        int forceMixY = popoutY + POPOUT_PADDING + 10 + BTN_SIZE + 8;
        if (highlightedMaterial != null && !highlightedIsMelt &&
                isOver(mouseX, mouseY, popoutX + POPOUT_PADDING, forceMixY,
                        POPOUT_WIDTH - POPOUT_PADDING * 2, 12)) {
            PacketHandler.sendToServer(new ForceMixPacket(menu.blockEntity.getBlockPos(), highlightedMaterial));
            highlightedMaterial = null;
            return true;
        }

        // Melt buttons
        for (int i = 0; i < MELT_MATERIALS.length; i++) {
            AlloyMaterial mat = MELT_MATERIALS[i];
            int[] pos = getMeltButtonPos(popoutX, popoutY, i);
            if (isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE)) {
                if (button == 1) {
                    highlightedMaterial = mat.getName().equals(highlightedMaterial) ? null : mat.getName();
                    highlightedIsMelt   = true;
                } else {
                    if (mat.getName().equals(highlightedMaterial)) highlightedMaterial = null;
                    PacketHandler.sendToServer(
                            new ToggleMeltingPacket(menu.blockEntity.getBlockPos(), mat.getName()));
                }
                return true;
            }
        }

        // Force Recipe button
        int forceRecipeY = popoutY + getControlsHeight() - 20;
        if (highlightedMaterial != null && highlightedIsMelt &&
                isOver(mouseX, mouseY, popoutX + POPOUT_PADDING, forceRecipeY,
                        POPOUT_WIDTH - POPOUT_PADDING * 2, 12)) {
            PacketHandler.sendToServer(new ForceRecipePacket(menu.blockEntity.getBlockPos(), highlightedMaterial));
            highlightedMaterial = null;
            return true;
        }

        return false;
    }

    private boolean handleFluidOutClick(int mouseX, int mouseY, int button, int x, int y) {
        int popoutX = x + imageWidth + 2;
        int popoutY = y;

        List<FluidStack> fluids = menu.getAllOutputFluids();

        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            AlloyMaterial mat = AlloyMaterial.fromFluid(fluid.getFluid());
            if (mat == null) continue;

            int[] pos = getFluidButtonPos(popoutX, popoutY, i);
            if (isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE)) {
                if (button == 1) {
                    highlightedFluid = mat.getName().equals(highlightedFluid) ? null : mat.getName();
                } else {
                    String newTarget = mat.getName().equals(castTargetFluid) ? "" : mat.getName();
                    castTargetFluid  = newTarget.isEmpty() ? null : newTarget;
                    PacketHandler.sendToServer(new SetCastTargetPacket(menu.blockEntity.getBlockPos(), newTarget));
                    if (mat.getName().equals(highlightedFluid)) highlightedFluid = null;
                }
                return true;
            }
        }

        // Dump button
        int dumpY = popoutY + getFluidOutHeight() - 20;
        if (highlightedFluid != null &&
                isOver(mouseX, mouseY, popoutX + POPOUT_PADDING, dumpY,
                        POPOUT_WIDTH - POPOUT_PADDING * 2, 12)) {
            if (highlightedFluid.equals(castTargetFluid)) castTargetFluid = null;
            PacketHandler.sendToServer(new DumpFluidPacket(menu.blockEntity.getBlockPos(), highlightedFluid));
            highlightedFluid = null;
            return true;
        }

        return false;
    }

    // ── POSITION HELPERS ──────────────────────────────────────────────────────

    private int[] getAlloyButtonPos(int popoutX, int popoutY, int index) {
        return new int[]{
                popoutX + POPOUT_PADDING + index * (BTN_SIZE + 4),
                popoutY + POPOUT_PADDING + 10
        };
    }

    private int[] getMeltButtonPos(int popoutX, int popoutY, int index) {
        int col        = index % 3;
        int row        = index / 3;
        int gridStartY = popoutY + POPOUT_PADDING + 10 + BTN_SIZE + 8 + 14 + 8 + 10;
        return new int[]{
                popoutX + POPOUT_PADDING + col * (BTN_SIZE + 4),
                gridStartY + row * (BTN_SIZE + 4)
        };
    }

    private int[] getFluidButtonPos(int popoutX, int popoutY, int index) {
        return new int[]{
                popoutX + POPOUT_PADDING,
                popoutY + POPOUT_PADDING + 10 + index * (BTN_SIZE + 4)
        };
    }

    private int getControlsHeight() {
        int meltRows = (int) Math.ceil(MELT_MATERIALS.length / 3.0);
        return POPOUT_PADDING
                + 10 + BTN_SIZE + 8
                + 14
                + 8 + 10
                + meltRows * (BTN_SIZE + 4)
                + 20
                + POPOUT_PADDING;
    }

    private int getFluidOutHeight() {
        int rows = Math.max(1, menu.getAllOutputFluids().size());
        return POPOUT_PADDING
                + 10
                + rows * (BTN_SIZE + 4)
                + 8
                + 20
                + POPOUT_PADDING;
    }

    private boolean isOver(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean isOverButton(int mx, int my, int x, int y, int buttonY) {
        return isOver(mx, my, x + POPOUT_BUTTON_X, y + buttonY, POPOUT_BUTTON_SIZE, POPOUT_BUTTON_SIZE);
    }

    // ── RENDER BG ─────────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Base GUI
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);

        // Fluid renders first (behind blue lines)
        renderFluidTank(guiGraphics, x, y);
        renderOutputTank(guiGraphics, x, y);

        // Blue lines overlay on output tank
        guiGraphics.blit(TEXTURE, x + BLUE_LINES_X, y + BLUE_LINES_Y,
                BLUE_LINES_U, BLUE_LINES_V, BLUE_LINES_W, BLUE_LINES_H);

        // Progress arrow
        if (menu.isCrafting()) {
            guiGraphics.blit(TEXTURE, x + 110, y + 34, 176, 0, menu.getScaledProgress(), 17);
        }

        // Dynamic extra slot backgrounds (only show visible slots)
        renderExtraSlots(guiGraphics, x, y);

        // Scrollbar
        if (shouldShowScrollbar()) {
            renderScrollbar(guiGraphics, x, y);
        }
    }

    // ── RENDER ────────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        this.renderBackground(guiGraphics, mouseX, mouseY, delta);
        RenderSystem.enableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        super.render(guiGraphics, mouseX, mouseY, delta);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        int lastTexture = RenderSystem.getShaderTexture(0);

        renderSideButtons(guiGraphics, x, y, mouseX, mouseY);

        if ("controls".equals(activePopout)) renderControlsPopout(guiGraphics, x, y, mouseX, mouseY);
        if ("fluidout".equals(activePopout)) renderFluidOutPopout(guiGraphics, x, y, mouseX, mouseY);

        RenderSystem.setShaderTexture(0, lastTexture);
        RenderSystem.enableDepthTest();

        this.renderTooltip(guiGraphics, mouseX, mouseY);
        renderFluidTankTooltip(guiGraphics, mouseX, mouseY);
        renderOutputTankTooltip(guiGraphics, mouseX, mouseY);

        if ("controls".equals(activePopout)) renderControlsTooltips(guiGraphics, mouseX, mouseY, x, y);
        if ("fluidout".equals(activePopout)) renderFluidOutTooltips(guiGraphics, mouseX, mouseY, x, y);
    }

    // ── EXTRA SLOTS ───────────────────────────────────────────────────────────
    // Only renders the 3x3 grid of slots based on current scroll position

    private void renderExtraSlots(GuiGraphics guiGraphics, int x, int y) {
        if (currentScrollRow == 0) return; // page 0 already drawn by base texture

        // Draw slot backgrounds for the current page (slots share the base 3x3 positions)
        for (int i = 0; i < 9; i++) {
            int col = i % 3;
            int row = i / 3;
            guiGraphics.blit(TEXTURE,
                    x + 10 + col * 18,
                    y + 19 + row * 18,
                    EXTRA_SLOT_U, EXTRA_SLOT_V, EXTRA_SLOT_W, EXTRA_SLOT_H);
        }
    }

    // ── SCROLLBAR RENDER ──────────────────────────────────────────────────────
    private float lastPercent = -1f;
    private void renderScrollbar(GuiGraphics guiGraphics, int x, int y) {
        guiGraphics.blit(TEXTURE, x + SCROLL_X, y + SCROLL_Y,
                SCROLL_TRACK_U, SCROLL_TRACK_V, SCROLL_TRACK_W, SCROLL_TRACK_H);

        int maxRows = getMaxScrollRows();
        if (maxRows <= 0) return;

        float percent = (float) currentScrollRow / maxRows;
        int thumbY = (int)(percent * (SCROLL_TRACK_H - SCROLL_THUMB_H));

        guiGraphics.blit(TEXTURE, x + SCROLL_X, y + SCROLL_Y + thumbY,
                SCROLL_THUMB_U, SCROLL_THUMB_V, SCROLL_THUMB_W, SCROLL_THUMB_H);
    }

    // ── SIDE BUTTONS ──────────────────────────────────────────────────────────

    private void renderSideButtons(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        renderSideButton(guiGraphics, x, y, mouseX, mouseY, CONTROLS_BUTTON_Y,
                ItemStackHelper.getAlloyStack(AlloyMaterial.BRONZE),
                "controls".equals(activePopout));

        ItemStack titaniumBucket = new ItemStack(ModItems.ALLOY_BUCKETS.get(AlloyMaterial.TITANIUM).get());
        renderSideButton(guiGraphics, x, y, mouseX, mouseY, FLUID_OUT_BUTTON_Y,
                titaniumBucket,
                "fluidout".equals(activePopout));
    }

    private void renderSideButton(GuiGraphics guiGraphics, int x, int y,
                                  int mouseX, int mouseY, int buttonY,
                                  ItemStack icon, boolean active) {
        int bx = x + POPOUT_BUTTON_X;
        int by = y + buttonY;
        boolean hovered = isOverButton(mouseX, mouseY, x, y, buttonY);
        int bgColor = (active || hovered) ? 0xFFAAAAAA : 0xFF888888;
        guiGraphics.fill(bx - 1, by - 1, bx + POPOUT_BUTTON_SIZE + 1, by + POPOUT_BUTTON_SIZE + 1, bgColor);
        guiGraphics.renderItem(icon, bx, by);
    }

    // ── CONTROLS POPOUT ───────────────────────────────────────────────────────

    private void renderControlsPopout(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int popoutX      = x + imageWidth + 2;
        int popoutY      = y;
        int popoutHeight = getControlsHeight();

        renderPopoutBackground(guiGraphics, popoutX, popoutY, popoutHeight);

        guiGraphics.drawString(font, "Alloys", popoutX + POPOUT_PADDING, popoutY + POPOUT_PADDING, 0xFFAAAAAA, false);
        for (int i = 0; i < ALLOY_MATERIALS.length; i++) {
            AlloyMaterial mat = ALLOY_MATERIALS[i];
            int[] pos = getAlloyButtonPos(popoutX, popoutY, i);
            boolean isBlocked     = menu.blockEntity.mixingBlocked.contains(mat.getName());
            boolean isHighlighted = mat.getName().equals(highlightedMaterial);
            boolean isHovered     = isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE);
            renderIngotButton(guiGraphics, pos[0], pos[1], getIngotForMaterial(mat), isBlocked, isHighlighted, isHovered);
        }

        int forceMixY = popoutY + POPOUT_PADDING + 10 + BTN_SIZE + 8;
        renderForceButton(guiGraphics, popoutX, forceMixY, "Force Mix",
                highlightedMaterial != null && !highlightedIsMelt);

        int dividerY = forceMixY + 16;
        guiGraphics.fill(popoutX + POPOUT_PADDING, dividerY,
                popoutX + POPOUT_WIDTH - POPOUT_PADDING, dividerY + 1, 0xFF444444);
        guiGraphics.drawString(font, "Melting", popoutX + POPOUT_PADDING, dividerY + 3, 0xFFAAAAAA, false);

        for (int i = 0; i < MELT_MATERIALS.length; i++) {
            AlloyMaterial mat = MELT_MATERIALS[i];
            int[] pos = getMeltButtonPos(popoutX, popoutY, i);
            boolean isBlocked     = menu.blockEntity.meltingBlocked.contains(mat.getName());
            boolean isHighlighted = mat.getName().equals(highlightedMaterial);
            boolean isHovered     = isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE);
            renderIngotButton(guiGraphics, pos[0], pos[1], getIngotForMaterial(mat), isBlocked, isHighlighted, isHovered);
        }

        int forceRecipeY = popoutY + getControlsHeight() - 20;
        renderForceButton(guiGraphics, popoutX, forceRecipeY, "Force Recipe",
                highlightedMaterial != null && highlightedIsMelt);
    }

    // ── FLUID OUTPUT POPOUT ───────────────────────────────────────────────────

    private void renderFluidOutPopout(GuiGraphics guiGraphics, int x, int y, int mouseX, int mouseY) {
        int popoutX      = x + imageWidth + 2;
        int popoutY      = y;
        int popoutHeight = getFluidOutHeight();

        renderPopoutBackground(guiGraphics, popoutX, popoutY, popoutHeight);

        guiGraphics.drawString(font, "Fluid Output", popoutX + POPOUT_PADDING, popoutY + POPOUT_PADDING, 0xFFAAAAAA, false);

        List<FluidStack> fluids = menu.getAllOutputFluids();

        if (fluids.isEmpty()) {
            guiGraphics.drawString(font, "Tank empty",
                    popoutX + POPOUT_PADDING, popoutY + POPOUT_PADDING + 14, 0xFF666666, false);
        } else {
            for (int i = 0; i < fluids.size(); i++) {
                FluidStack fluid = fluids.get(i);
                AlloyMaterial mat = AlloyMaterial.fromFluid(fluid.getFluid());
                if (mat == null) continue;

                int[] pos         = getFluidButtonPos(popoutX, popoutY, i);
                boolean isCastTarget  = mat.getName().equals(castTargetFluid);
                boolean isHighlighted = mat.getName().equals(highlightedFluid);
                boolean isHovered     = isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE);

                int borderColor;
                if (isHighlighted)     borderColor = 0xFFFFDD00;
                else if (isCastTarget) borderColor = 0xFF22CC22;
                else                   borderColor = 0xFFCC2222;

                guiGraphics.fill(pos[0] - 1, pos[1] - 1,
                        pos[0] + BTN_SIZE + 1, pos[1] + BTN_SIZE + 1, borderColor);
                guiGraphics.fill(pos[0], pos[1], pos[0] + BTN_SIZE, pos[1] + BTN_SIZE,
                        isHovered ? 0xFF444444 : 0xFF333333);

                var fluidExt = IClientFluidTypeExtensions.of(fluid.getFluid());
                ResourceLocation stillTex = fluidExt.getStillTexture();
                if (stillTex != null) {
                    TextureAtlasSprite sprite = Minecraft.getInstance()
                            .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(stillTex);
                    int color = fluidExt.getTintColor();
                    RenderSystem.setShaderColor(
                            ((color >> 16) & 0xFF) / 255f,
                            ((color >>  8) & 0xFF) / 255f,
                            ( color        & 0xFF) / 255f,
                            1.0f);
                    guiGraphics.enableScissor(pos[0] + 1, pos[1] + 1,
                            pos[0] + BTN_SIZE - 1, pos[1] + BTN_SIZE - 1);
                    guiGraphics.blit(pos[0] + 1, pos[1] + 1, 0, BTN_SIZE - 2, BTN_SIZE - 2, sprite);
                    guiGraphics.disableScissor();
                    RenderSystem.setShaderColor(1, 1, 1, 1);
                }

                int textColor = isHighlighted ? 0xFFFFDD00 : (isCastTarget ? 0xFF44CC44 : 0xFFAAAAAA);
                guiGraphics.drawString(font, mat.getName(),
                        pos[0] + BTN_SIZE + 4, pos[1] + 4, textColor, false);
            }
        }

        int dumpY = popoutY + getFluidOutHeight() - 20;
        renderForceButton(guiGraphics, popoutX, dumpY, "Dump Fluid", highlightedFluid != null);
    }

    // ── SHARED PANEL RENDERING ────────────────────────────────────────────────

    private void renderPopoutBackground(GuiGraphics guiGraphics, int popoutX, int popoutY, int height) {
        guiGraphics.fill(popoutX, popoutY, popoutX + POPOUT_WIDTH, popoutY + height, 0xFF1A1A1A);
        guiGraphics.fill(popoutX, popoutY, popoutX + POPOUT_WIDTH, popoutY + 1, 0xFF555555);
        guiGraphics.fill(popoutX, popoutY + height - 1, popoutX + POPOUT_WIDTH, popoutY + height, 0xFF555555);
        guiGraphics.fill(popoutX, popoutY, popoutX + 1, popoutY + height, 0xFF555555);
        guiGraphics.fill(popoutX + POPOUT_WIDTH - 1, popoutY, popoutX + POPOUT_WIDTH, popoutY + height, 0xFF555555);
    }

    private void renderIngotButton(GuiGraphics guiGraphics, int bx, int by,
                                   ItemStack icon, boolean isBlocked,
                                   boolean isHighlighted, boolean isHovered) {
        int borderColor = isHighlighted ? 0xFFFFDD00 : (isBlocked ? 0xFFCC2222 : 0xFF22CC22);
        guiGraphics.fill(bx - 1, by - 1, bx + BTN_SIZE + 1, by + BTN_SIZE + 1, borderColor);
        guiGraphics.fill(bx, by, bx + BTN_SIZE, by + BTN_SIZE, isHovered ? 0xFF444444 : 0xFF333333);
        guiGraphics.renderItem(icon, bx + 1, by + 1);
    }

    private void renderForceButton(GuiGraphics guiGraphics, int popoutX, int y,
                                   String label, boolean active) {
        guiGraphics.fill(popoutX + POPOUT_PADDING, y,
                popoutX + POPOUT_WIDTH - POPOUT_PADDING, y + 12,
                active ? 0xFF886600 : 0xFF2A2A2A);
        guiGraphics.drawCenteredString(font, label,
                popoutX + POPOUT_WIDTH / 2, y + 2,
                active ? 0xFFFFDD00 : 0xFF555555);
    }

    // ── CONTROLS TOOLTIPS ─────────────────────────────────────────────────────

    private void renderControlsTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        int popoutX = x + imageWidth + 2;
        int popoutY = y;

        for (int i = 0; i < ALLOY_MATERIALS.length; i++) {
            AlloyMaterial mat = ALLOY_MATERIALS[i];
            int[] pos = getAlloyButtonPos(popoutX, popoutY, i);
            if (isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE)) {
                guiGraphics.renderComponentTooltip(font,
                        buildControlsTooltip(mat.getName(),
                                menu.blockEntity.mixingBlocked.contains(mat.getName()),
                                mat.getName().equals(highlightedMaterial), true),
                        mouseX, mouseY);
                return;
            }
        }

        for (int i = 0; i < MELT_MATERIALS.length; i++) {
            AlloyMaterial mat = MELT_MATERIALS[i];
            int[] pos = getMeltButtonPos(popoutX, popoutY, i);
            if (isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE)) {
                guiGraphics.renderComponentTooltip(font,
                        buildControlsTooltip(mat.getName(),
                                menu.blockEntity.meltingBlocked.contains(mat.getName()),
                                mat.getName().equals(highlightedMaterial), false),
                        mouseX, mouseY);
                return;
            }
        }

        int forceMixY = popoutY + POPOUT_PADDING + 10 + BTN_SIZE + 8;
        if (isOver(mouseX, mouseY, popoutX + POPOUT_PADDING, forceMixY,
                POPOUT_WIDTH - POPOUT_PADDING * 2, 12)) {
            List<Component> tip = new ArrayList<>();
            if (highlightedMaterial != null && !highlightedIsMelt) {
                tip.add(Component.literal("Force Mix: " + highlightedMaterial).withStyle(ChatFormatting.YELLOW));
                tip.add(Component.literal("Mixes once immediately").withStyle(ChatFormatting.GRAY));
                tip.add(Component.literal("Respects tank capacity").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tip.add(Component.literal("Force Mix").withStyle(ChatFormatting.GRAY));
                tip.add(Component.literal("Right-click an alloy to mark it").withStyle(ChatFormatting.DARK_GRAY));
            }
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
            return;
        }

        int forceRecipeY = popoutY + getControlsHeight() - 20;
        if (isOver(mouseX, mouseY, popoutX + POPOUT_PADDING, forceRecipeY,
                POPOUT_WIDTH - POPOUT_PADDING * 2, 12)) {
            List<Component> tip = new ArrayList<>();
            if (highlightedMaterial != null && highlightedIsMelt) {
                tip.add(Component.literal("Force Recipe: " + highlightedMaterial).withStyle(ChatFormatting.YELLOW));
                tip.add(Component.literal("Melts one item immediately").withStyle(ChatFormatting.GRAY));
                tip.add(Component.literal("Requires lava and tank space").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tip.add(Component.literal("Force Recipe").withStyle(ChatFormatting.GRAY));
                tip.add(Component.literal("Right-click a melt material to mark it").withStyle(ChatFormatting.DARK_GRAY));
            }
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    // ── FLUID OUTPUT TOOLTIPS ─────────────────────────────────────────────────

    private void renderFluidOutTooltips(GuiGraphics guiGraphics, int mouseX, int mouseY, int x, int y) {
        int popoutX = x + imageWidth + 2;
        int popoutY = y;

        List<FluidStack> fluids = menu.getAllOutputFluids();

        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            AlloyMaterial mat = AlloyMaterial.fromFluid(fluid.getFluid());
            if (mat == null) continue;

            int[] pos = getFluidButtonPos(popoutX, popoutY, i);
            if (isOver(mouseX, mouseY, pos[0], pos[1], BTN_SIZE, BTN_SIZE)) {
                boolean isCastTarget  = mat.getName().equals(castTargetFluid);
                boolean isHighlighted = mat.getName().equals(highlightedFluid);

                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(mat.getName()).withStyle(ChatFormatting.WHITE));
                tip.add(Component.literal(fluid.getAmount() + " mb").withStyle(ChatFormatting.GRAY));

                if (isHighlighted) {
                    tip.add(Component.literal("★ Marked for Dump").withStyle(ChatFormatting.YELLOW));
                    tip.add(Component.literal("Click Dump Fluid to erase").withStyle(ChatFormatting.GRAY));
                    tip.add(Component.literal("Right-click to clear").withStyle(ChatFormatting.DARK_GRAY));
                } else if (isCastTarget) {
                    tip.add(Component.literal("✔ Ready for Casting").withStyle(ChatFormatting.GREEN));
                    tip.add(Component.literal("Left-click to deselect").withStyle(ChatFormatting.GRAY));
                    tip.add(Component.literal("Right-click to mark for Dump").withStyle(ChatFormatting.DARK_GRAY));
                } else {
                    tip.add(Component.literal("Left-click to mark for casting").withStyle(ChatFormatting.GRAY));
                    tip.add(Component.literal("Right-click to mark for Dump").withStyle(ChatFormatting.DARK_GRAY));
                }

                guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
                return;
            }
        }

        int dumpY = popoutY + getFluidOutHeight() - 20;
        if (isOver(mouseX, mouseY, popoutX + POPOUT_PADDING, dumpY,
                POPOUT_WIDTH - POPOUT_PADDING * 2, 12)) {
            List<Component> tip = new ArrayList<>();
            if (highlightedFluid != null) {
                tip.add(Component.literal("Dump: " + highlightedFluid).withStyle(ChatFormatting.YELLOW));
                tip.add(Component.literal("Erases all of this fluid").withStyle(ChatFormatting.RED));
                tip.add(Component.literal("This cannot be undone").withStyle(ChatFormatting.DARK_GRAY));
            } else {
                tip.add(Component.literal("Dump Fluid").withStyle(ChatFormatting.GRAY));
                tip.add(Component.literal("Right-click a fluid to mark it").withStyle(ChatFormatting.DARK_GRAY));
            }
            guiGraphics.renderComponentTooltip(font, tip, mouseX, mouseY);
        }
    }

    // ── TOOLTIP BUILDER ───────────────────────────────────────────────────────

    private List<Component> buildControlsTooltip(String name, boolean isBlocked,
                                                 boolean isHighlighted, boolean isMix) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal(name).withStyle(ChatFormatting.WHITE));
        if (isHighlighted) {
            tip.add(Component.literal("★ Marked for Force " + (isMix ? "Mix" : "Recipe"))
                    .withStyle(ChatFormatting.YELLOW));
            tip.add(Component.literal("Right-click to clear").withStyle(ChatFormatting.DARK_GRAY));
        } else if (isBlocked) {
            tip.add(Component.literal("✗ " + (isMix ? "Will NOT mix" : "Melting disabled"))
                    .withStyle(ChatFormatting.RED));
            tip.add(Component.literal("Left-click to enable").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Right-click to mark for Force " + (isMix ? "Mix" : "Recipe"))
                    .withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tip.add(Component.literal("✔ " + (isMix ? "Will mix with compatible fluids" : "Melting enabled"))
                    .withStyle(ChatFormatting.GREEN));
            tip.add(Component.literal("Left-click to disable").withStyle(ChatFormatting.GRAY));
            tip.add(Component.literal("Right-click to mark for Force " + (isMix ? "Mix" : "Recipe"))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
        return tip;
    }

    // ── INGOT ITEM HELPER ─────────────────────────────────────────────────────

    private ItemStack getIngotForMaterial(AlloyMaterial mat) {
        return switch (mat) {
            case COPPER    -> new ItemStack(Items.COPPER_INGOT);
            case IRON      -> new ItemStack(Items.IRON_INGOT);
            case GOLD      -> new ItemStack(Items.GOLD_INGOT);
            case DIAMOND   -> new ItemStack(Items.DIAMOND);
            case NETHERITE -> new ItemStack(Items.NETHERITE_INGOT);
            case COAL      -> new ItemStack(Items.COAL);
            default        -> ItemStackHelper.getAlloyStack(mat);
        };
    }

    // ── FLUID RENDERS ─────────────────────────────────────────────────────────

    private void renderFluidTank(GuiGraphics guiGraphics, int x, int y) {
        FluidStack fluidStack = menu.getFluid();
        int amount   = menu.getFluidAmount();
        int capacity = menu.getFluidCapacity();
        if (fluidStack.isEmpty() || amount <= 0 || capacity <= 0) return;

        var ext = IClientFluidTypeExtensions.of(fluidStack.getFluid());
        ResourceLocation tex = ext.getStillTexture(fluidStack);
        if (tex == null) tex = ext.getStillTexture();

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(tex);

        int fluidHeight = (int) (FLUID_TANK_HEIGHT * ((float) amount / capacity));
        int fluidY      = y + FLUID_TANK_Y + (FLUID_TANK_HEIGHT - fluidHeight);
        int color       = ext.getTintColor(fluidStack);
        float[] last    = RenderSystem.getShaderColor();

        RenderSystem.disableDepthTest();
        RenderSystem.setShaderColor(
                ((color >> 16) & 0xFF) / 255f,
                ((color >>  8) & 0xFF) / 255f,
                ( color        & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f);

        guiGraphics.blit(x + FLUID_TANK_X, fluidY, 0, FLUID_TANK_WIDTH, fluidHeight - 4, sprite);
        RenderSystem.setShaderColor(last[0], last[1], last[2], last[3]);
        RenderSystem.enableDepthTest();
        RenderSystem.setShaderTexture(0, TEXTURE);
    }

    private void renderOutputTank(GuiGraphics guiGraphics, int x, int y) {
        List<FluidStack> fluids = menu.getAllOutputFluids();
        Collections.reverse(fluids);
        if (fluids.isEmpty()) return;

        int capacity    = menu.getOutputFluidCapacity();
        int totalAmount = menu.getOutputFluidAmount();
        if (capacity <= 0 || totalAmount <= 0) return;

        int filledHeight = Math.round(OUTPUT_TANK_HEIGHT * ((float) totalAmount / capacity));
        if (filledHeight <= 0) return;

        int currentY   = y + OUTPUT_TANK_Y + OUTPUT_TANK_HEIGHT - 4;
        int usedHeight = 0;

        for (int i = 0; i < fluids.size(); i++) {
            FluidStack fluid = fluids.get(i);
            int fh = Math.round(filledHeight * ((float) fluid.getAmount() / totalAmount));
            if (i == fluids.size() - 1) fh = filledHeight - usedHeight;
            if (fh <= 0) fh = 1;
            renderFluid(guiGraphics, x + OUTPUT_TANK_X, currentY - fh, OUTPUT_TANK_WIDTH, fh, fluid);
            currentY   -= fh;
            usedHeight += fh;
        }
    }

    private void renderFluid(GuiGraphics guiGraphics, int x, int y, int width, int height, FluidStack fluid) {
        var ext    = IClientFluidTypeExtensions.of(fluid.getFluid());
        var sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(ext.getStillTexture(fluid));
        int color  = ext.getTintColor(fluid);
        RenderSystem.setShaderColor(
                ((color >> 16) & 0xFF) / 255f,
                ((color >>  8) & 0xFF) / 255f,
                ( color        & 0xFF) / 255f,
                ((color >> 24) & 0xFF) / 255f);
        guiGraphics.blit(x, y, 0, width, height, sprite);
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    // ── FLUID TOOLTIPS ────────────────────────────────────────────────────────

    private void renderFluidTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        if (!isOver(mouseX, mouseY, x + FLUID_TANK_X, y + FLUID_TANK_Y,
                FLUID_TANK_WIDTH, FLUID_TANK_HEIGHT)) return;

        List<Component> tooltip = new ArrayList<>();
        FluidStack fs  = menu.getFluid();
        int amount     = menu.getFluidAmount();
        int capacity   = menu.getFluidCapacity();
        int tankCount  = menu.getConnectedTanksCount();
        NumberFormat fmt = NumberFormat.getIntegerInstance();

        if (fs.isEmpty() || amount == 0) {
            tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.empty")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.capacity",
                    fmt.format(capacity)).withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(fs.getHoverName().copy().withStyle(ChatFormatting.GOLD));
            tooltip.add(Component.translatable("tooltip.arsenalcrafting.fluid_tank.amount",
                    fmt.format(amount), fmt.format(capacity),
                    String.format("%.1f", (amount * 100f) / capacity)).withStyle(ChatFormatting.GREEN));
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

    private void renderOutputTankTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        if (!isHovering(OUTPUT_TANK_X, OUTPUT_TANK_Y, OUTPUT_TANK_WIDTH, OUTPUT_TANK_HEIGHT,
                mouseX, mouseY)) return;

        List<Component> tooltip = new ArrayList<>();
        List<FluidStack> fluids = menu.getAllOutputFluids();
        tooltip.add(Component.literal("Molten Output").withStyle(ChatFormatting.GOLD));

        if (fluids.isEmpty()) {
            tooltip.add(Component.literal("Empty").withStyle(ChatFormatting.GRAY));
        } else {
            int total    = menu.getOutputFluidAmount();
            int capacity = menu.getOutputFluidCapacity();
            for (FluidStack fluid : fluids) {
                tooltip.add(Component.literal(String.format("  %s: %d mb (%.1f%%)",
                                fluid.getFluid().getFluidType().getDescription().getString(),
                                fluid.getAmount(), (fluid.getAmount() * 100f) / capacity))
                        .withStyle(ChatFormatting.YELLOW));
            }
            tooltip.add(Component.empty());
            tooltip.add(Component.literal(String.format("Total: %d / %d mb (%.1f%%)",
                    total, capacity, (total * 100f / capacity))).withStyle(ChatFormatting.GREEN));
            /*System.out.println("[Tooltip] capacity=" + menu.getOutputFluidCapacity()
                    + " bePos=" + menu.blockEntity.getBlockPos()
                    + " beCapacity=" + menu.blockEntity.OUTPUT_TANK.getCapacity());

             */
        }
        guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }
}