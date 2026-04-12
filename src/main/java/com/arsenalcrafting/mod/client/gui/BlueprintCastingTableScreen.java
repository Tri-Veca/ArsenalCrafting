package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.BlueprintCastingTableMenu;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.PackageType;
import com.arsenalcrafting.mod.network.PacketHandler;
import com.arsenalcrafting.mod.network.SelectPackagePacket;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModItems;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class BlueprintCastingTableScreen extends AbstractContainerScreen<BlueprintCastingTableMenu> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                    "textures/gui/container/blueprint_casting_table.png");

    // Slot positions
    private static final int BLUEPRINT_SLOT_X = 14;
    private static final int BLUEPRINT_SLOT_Y = 33;
    private static final int INGOT_SLOT_X = 38;
    private static final int INGOT_SLOT_Y = 32;
    private static final int RESULT_SLOT_X = 75;
    private static final int RESULT_SLOT_Y = 35;

    // 3x3 Grid starting position
    private static final int GRID_START_X = 115;
    private static final int GRID_START_Y = 11;
    private static final int SLOT_SIZE = 18;

    private PackageType[] packages = PackageType.values();
    private PackageType selectedPackage = null;
    private PackageType hoveredPackage = null;
    private int lastIngotCount = -1;
    private boolean lastHasBlueprint = false;
    private int animationTick = 0;

    public BlueprintCastingTableScreen(BlueprintCastingTableMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
    }



    private void drawSlot(GuiGraphics guiGraphics, int x, int y) {
        // Draw a slot background (dark gray square with border)
        guiGraphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFF000000); // Border
        guiGraphics.fill(x, y, x + 16, y + 16, 0xFF3C3C3C); // Slot background
    }


    private boolean isMouseOverSlot(int mouseX, int mouseY, int slotX, int slotY) {
        return mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                mouseY >= slotY && mouseY <= slotY + SLOT_SIZE;
    }

    private void renderGhostItem(GuiGraphics guiGraphics, ItemStack stack, int x, int y) {
        if (stack.isEmpty()) return;

        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 0.5F);
        guiGraphics.renderItem(stack, x, y);
        guiGraphics.renderItemDecorations(font, stack, x, y);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.disableBlend();
    }

    private void renderCannotAfford(GuiGraphics guiGraphics, int x, int y) {
        // Draw a red X over the slot
        guiGraphics.fill(x, y, x + 16, y + 1, 0xFFFF0000);
        guiGraphics.fill(x, y + 15, x + 16, y + 16, 0xFFFF0000);
        guiGraphics.fill(x, y, x + 1, y + 16, 0xFFFF0000);
        guiGraphics.fill(x + 15, y, x + 16, y + 16, 0xFFFF0000);

        // Draw diagonal lines
        guiGraphics.fill(x, y, x + 16, y + 16, 0x88FF0000);
    }

    private void renderPackageIcon(GuiGraphics guiGraphics, ItemStack icon, int x, int y, boolean grayedOut) {
        if (grayedOut) {
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(0.5F, 0.5F, 0.5F, 0.6F);
            guiGraphics.renderItem(icon, x, y);
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            RenderSystem.disableBlend();
        } else {
            guiGraphics.renderItem(icon, x, y);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);

        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;

        // Draw background texture
        guiGraphics.blit(TEXTURE, x, y, 0, 0, imageWidth, imageHeight);



        // Draw the 3x3 package grid slots
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                if (idx < packages.length) {
                    int slotX = x + GRID_START_X + col * SLOT_SIZE;
                    int slotY = y + GRID_START_Y + row * SLOT_SIZE;

                }
            }
        }
    }


    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics, mouseX, mouseY, delta);
        super.render(guiGraphics, mouseX, mouseY, delta);

        boolean hasBlueprint = menu.hasBlueprint();
        int ingotCount = menu.getIngotCount();

        // Reset hovered package
        hoveredPackage = null;

        // Render package icons in the 3x3 grid
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                if (idx < packages.length) {
                    PackageType pkg = packages[idx];
                    int slotX = leftPos + GRID_START_X + col * SLOT_SIZE;
                    int slotY = topPos + GRID_START_Y + row * SLOT_SIZE;

                    boolean canAfford = hasBlueprint && ingotCount >= pkg.getIngotCost();

                    // Render package icon
                    ItemStack icon = getPackageIcon(pkg);
                    renderPackageIcon(guiGraphics, icon, slotX, slotY, !canAfford);

                    // Check hover for tooltip ONLY
                    if (isMouseOverSlot(mouseX, mouseY, slotX, slotY)) {
                        hoveredPackage = pkg;
                        renderCostTooltip(guiGraphics, pkg, mouseX, mouseY);
                    }
                }
            }
        }

        // Get the currently selected package (the one the mouse is over)
        PackageType hoveredPackage = getHoveredPackage(mouseX, mouseY);
        if (selectedPackage != null && menu.getSelectedPackage() == selectedPackage) {
            // Only show ghost if no result exists yet
            if (menu.getResultItem().isEmpty()) {
                ItemStack result = getPackageIcon(selectedPackage);
                int resultX = leftPos + RESULT_SLOT_X;
                int resultY = topPos + RESULT_SLOT_Y;
                renderGhostItem(guiGraphics, result, resultX, resultY);
            }
        } else {
            // Clear selection if server doesn't have it
            selectedPackage = null;
        }

        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    private PackageType getHoveredPackage(int mouseX, int mouseY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                if (idx < packages.length) {
                    int slotX = leftPos + GRID_START_X + col * SLOT_SIZE;
                    int slotY = topPos + GRID_START_Y + row * SLOT_SIZE;

                    if (isMouseOverSlot(mouseX, mouseY, slotX, slotY)) {
                        return packages[idx];
                    }
                }
            }
        }
        return null;
    }

    private void renderCostTooltip(GuiGraphics guiGraphics, PackageType pkg, int mouseX, int mouseY) {
        List<Component> tooltip = new ArrayList<>();

        tooltip.add(Component.literal(pkg.getDisplayName()).withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("Cost: " + pkg.getIngotCost() + " Gold/Bronze Ingots + 1 Blueprint")
                .withStyle(ChatFormatting.GRAY));

        long gameTime = System.currentTimeMillis();
        boolean showGold = (gameTime / 1000) % 2 == 0;

        tooltip.add(Component.literal(showGold ? "Gold Ingot" : "Bronze Ingot")
                .withStyle(ChatFormatting.YELLOW));

        tooltip.add(Component.literal("Contains:").withStyle(ChatFormatting.GRAY));

        for (var mold : pkg.getMolds()) {
            tooltip.add(Component.literal("  • " + mold.getDisplayName())
                    .withStyle(ChatFormatting.YELLOW));
        }
        for (var extra : pkg.getExtraMolds()) {
            tooltip.add(Component.literal("  • " + extra.getDisplayName())
                    .withStyle(ChatFormatting.YELLOW));
        }

        guiGraphics.renderComponentTooltip(font, tooltip, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                if (idx < packages.length) {
                    int slotX = leftPos + GRID_START_X + col * SLOT_SIZE;
                    int slotY = topPos + GRID_START_Y + row * SLOT_SIZE;

                    if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                            mouseY >= slotY && mouseY <= slotY + SLOT_SIZE) {
                        selectedPackage = packages[idx];
                        PacketHandler.sendToServer(new SelectPackagePacket(packages[idx].getId()));
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    private PackageType getHoveredPackage(int mouseX, int mouseY, int guiX, int guiY) {
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                int idx = row * 3 + col;
                if (idx < packages.length) {
                    int slotX = guiX + GRID_START_X + col * SLOT_SIZE;
                    int slotY = guiY + GRID_START_Y + row * SLOT_SIZE;
                    if (mouseX >= slotX && mouseX <= slotX + SLOT_SIZE &&
                            mouseY >= slotY && mouseY <= slotY + SLOT_SIZE) {
                        return packages[idx];
                    }
                }
            }
        }
        return null;
    }

    private ItemStack getPackageIcon(PackageType pkg) {
        var holder = ModItems.CASTING_PACKAGES.get(pkg);
        return holder != null ? new ItemStack(holder.get()) : ItemStack.EMPTY;
    }


}