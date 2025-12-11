package com.arsenalcrafting.mod.client.gui;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.client.menu.ToolMenu;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.tool.ToolAugmentSlots;
import com.arsenalcrafting.mod.network.ToggleAugmentPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

public class ToolScreen extends AbstractContainerScreen<ToolMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "textures/gui/container/tool_gui.png");

    private static final int GUI_WIDTH = 176;
    private static final int GUI_HEIGHT = 166;

    // Slot positions (using your coordinates)
    private static final int SLOT_SIZE = 18;
    private static final int SLOT_Y = 36; // Vertical position for all slots
    private static final int[] SLOT_X = {46, 69, 91, 114}; // Your horizontal positions
    private static final int SLOT_LABEL_Y = SLOT_Y + SLOT_SIZE + 5; // For labels below slots

    private final List<AugmentSlotButton> slotButtons = new ArrayList<>();

    public ToolScreen(ToolMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
        this.inventoryLabelY = 74;
    }

    @Override
    protected void init() {
        super.init();

        // Clear previous buttons
        slotButtons.clear();

        // Create buttons for each augment slot
        ToolAugmentSlots slots = menu.getAugmentSlots();

        for (int i = 0; i < 4; i++) {
            String augmentId = slots.getAugmentInSlot(i);

            // Only create button if slot has an augment
            if (augmentId != null && !augmentId.isEmpty()) {
                int slotIndex = i;
                boolean enabled = menu.isAugmentEnabled(augmentId);

                AugmentSlotButton button = new AugmentSlotButton(
                        leftPos + SLOT_X[i],
                        topPos + SLOT_Y,
                        slotIndex,
                        augmentId,
                        enabled
                );

                slotButtons.add(button);
                addRenderableWidget(button);
            }
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        // Draw background
        guiGraphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);

        // Draw slot backgrounds for all 4 slots
        for (int i = 0; i < 4; i++) {
            guiGraphics.blit(TEXTURE,
                    leftPos + SLOT_X[i] - 1, topPos + SLOT_Y - 1,
                    176, 0, SLOT_SIZE + 2, SLOT_SIZE + 2);
        }

        // Draw augment icons
        ToolAugmentSlots slots = menu.getAugmentSlots();
        for (int i = 0; i < 4; i++) {
            String augmentId = slots.getAugmentInSlot(i);
            if (augmentId != null && !augmentId.isEmpty()) {
                // Draw augment icon
                ResourceLocation icon = ResourceLocation.fromNamespaceAndPath(
                        ArsenalCrafting.MOD_ID,
                        "textures/item/augments/" + augmentId + ".png"
                );

                try {
                    RenderSystem.setShaderTexture(0, icon);
                    guiGraphics.blit(icon,
                            leftPos + SLOT_X[i] + 1, topPos + SLOT_Y + 1,
                            0, 0, 16, 16, 16, 16);
                } catch (Exception e) {
                    // Fallback if texture not found - draw gray square
                    guiGraphics.fill(leftPos + SLOT_X[i] + 1, topPos + SLOT_Y + 1,
                            leftPos + SLOT_X[i] + 17, topPos + SLOT_Y + 17,
                            0xFF666666);
                }
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        // Draw title
        guiGraphics.drawString(this.font, this.title,
                this.titleLabelX, this.titleLabelY, 0x404040, false);

        // Draw instructions
        Component instructions = Component.translatable("gui.arsenalcrafting.tool.instructions")
                .withStyle(ChatFormatting.ITALIC, ChatFormatting.GRAY);
        guiGraphics.drawString(font, instructions,
                (imageWidth - font.width(instructions)) / 2, 20, 0x666666, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderTooltip(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        super.renderTooltip(guiGraphics, mouseX, mouseY);
    }

    // Custom button class for augment slots - ONLY created for slots with augments
    private class AugmentSlotButton extends Button {
        private final int slotIndex;
        private final String augmentId;
        private boolean enabled;

        public AugmentSlotButton(int x, int y, int slotIndex, String augmentId, boolean enabled) {
            super(
                    x, y,
                    SLOT_SIZE, SLOT_SIZE,
                    Component.empty(), // No text
                    button -> {
                        // Send packet to server to toggle augment
                        ToggleAugmentPacket packet = new ToggleAugmentPacket(augmentId);
                        var connection = Minecraft.getInstance().getConnection();
                        if (connection != null) {
                            connection.send(packet);
                        }

                        // Toggle locally for immediate feedback
                        ((AugmentSlotButton) button).enabled = !((AugmentSlotButton) button).enabled;

                        // Update tooltip with new state
                        ((AugmentSlotButton) button).updateTooltip();
                    },
                    DEFAULT_NARRATION
            );

            this.slotIndex = slotIndex;
            this.augmentId = augmentId; // This is guaranteed to be non-null and non-empty
            this.enabled = enabled;

            // Set initial tooltip
            Component tooltipText = buildAugmentTooltip(augmentId, enabled);
            this.setTooltip(Tooltip.create(tooltipText));
        }

        private void updateTooltip() {
            Component tooltipText = buildAugmentTooltip(augmentId, enabled);
            this.setTooltip(Tooltip.create(tooltipText));
        }

        private Component buildAugmentTooltip(String augmentId, boolean enabled) {
            MutableComponent tooltip = Component.empty();

            // Augment name with status color
            tooltip.append(Component.translatable(AugmentManager.getTranslationKey(augmentId))
                    .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED));

            // Status indicator
            tooltip.append(Component.literal(" ("));
            if (enabled) {
                tooltip.append(Component.translatable("gui.arsenalcrafting.status.enabled")
                        .withStyle(ChatFormatting.GREEN));
            } else {
                tooltip.append(Component.translatable("gui.arsenalcrafting.status.disabled")
                        .withStyle(ChatFormatting.RED));
            }
            tooltip.append(Component.literal(")"));

            // Add description if available
            AugmentManager.AugmentDefinition def = AugmentManager.get(augmentId);
            if (def != null) {
                String description = def.description();
                if (description != null && !description.isEmpty()) {
                    tooltip.append(Component.literal("\n"));
                    tooltip.append(Component.translatable(description)
                            .withStyle(ChatFormatting.GRAY));
                }
            }

            // Click instruction
            tooltip.append(Component.literal("\n"));
            tooltip.append(Component.translatable("gui.arsenalcrafting.click_to_toggle")
                    .withStyle(ChatFormatting.ITALIC, ChatFormatting.YELLOW));

            return tooltip;
        }

        @Override
        public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            // Draw colored border based on state
            int borderColor = enabled ? 0xFF00FF00 : 0xFFFF0000; // Green for enabled, Red for disabled

            // Draw border (1 pixel thick)
            guiGraphics.fill(this.getX(), this.getY(),
                    this.getX() + this.width, this.getY() + 1, borderColor); // Top
            guiGraphics.fill(this.getX(), this.getY() + this.height - 1,
                    this.getX() + this.width, this.getY() + this.height, borderColor); // Bottom
            guiGraphics.fill(this.getX(), this.getY(),
                    this.getX() + 1, this.getY() + this.height, borderColor); // Left
            guiGraphics.fill(this.getX() + this.width - 1, this.getY(),
                    this.getX() + this.width, this.getY() + this.height, borderColor); // Right

            // Draw hover effect
            if (this.isHoveredOrFocused()) {
                int hoverColor = 0x40FFFFFF; // Semi-transparent white
                guiGraphics.fill(this.getX(), this.getY(),
                        this.getX() + this.width, this.getY() + this.height, hoverColor);
            }
        }

        @Override
        public boolean isMouseOver(double mouseX, double mouseY) {
            // All buttons are interactive (created only for slots with augments)
            return super.isMouseOver(mouseX, mouseY);
        }
    }
}