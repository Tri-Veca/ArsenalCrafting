package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;

public interface ArsenalTool {

    ArsenalToolType getToolType();
    AlloyMaterial getBaseMaterial();

    // ---- AUGMENT SLOTS ----
    default ToolAugmentSlots getAugmentSlots(ItemStack stack) {
        ToolAugmentSlots slots = stack.get(ModComponents.TOOL_AUGMENT_SLOTS.get());
        if (slots == null) {
            slots = ToolAugmentSlots.migrateFromOld(this, stack);
            if (slots == null) slots = new ToolAugmentSlots();
            stack.set(ModComponents.TOOL_AUGMENT_SLOTS.get(), slots);
        }
        return slots;
    }

    default void setAugmentSlots(ItemStack stack, ToolAugmentSlots slots) {
        if (slots == null) slots = new ToolAugmentSlots();
        stack.set(ModComponents.TOOL_AUGMENT_SLOTS.get(), slots);
    }

    default void setAugmentInSlot(ItemStack stack, int slot, @Nullable String augmentId) {
        ToolAugmentSlots current = getAugmentSlots(stack);
        ToolAugmentSlots updated = current.withSlot(slot, augmentId == null ? "" : augmentId);
        setAugmentSlots(stack, updated);
    }

    default void clearAugmentSlot(ItemStack stack, int slot) {
        ToolAugmentSlots current = getAugmentSlots(stack);
        setAugmentSlots(stack, current.clearSlot(slot));
    }

    default boolean isAugmentInSlot(ItemStack stack, String augmentId, int slot) {
        if (augmentId == null) return false;
        String slotAugment = getAugmentSlots(stack).getAugmentInSlot(slot);
        return augmentId.equals(slotAugment);
    }

    default int getUsedAugmentSlots(ItemStack stack) {
        return getAugmentSlots(stack).getUsedSlotCount();
    }

    default int getTotalAugmentSlots() {
        return 4;
    }

    // ---- AUGMENT STATE ----
    default boolean isAugmentEnabled(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        return AugmentManager.isInstalled(augmentId, stack);
    }

    default void setAugmentEnabled(ItemStack stack, String augmentId, boolean enabled) {
        if (augmentId == null || augmentId.isEmpty()) return;
        if (enabled) {
            ToolAugmentSlots slots = getAugmentSlots(stack);
            int slot = slots.findSlotForAugment(augmentId);
            if (slot >= 0) {
                AugmentManager.install(augmentId, stack, AugmentManager.getTier(augmentId, stack));
            }
        } else {
            AugmentManager.uninstall(augmentId, stack);
        }
    }

    default void toggleAugment(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return;
        boolean currentlyEnabled = isAugmentEnabled(stack, augmentId);
        setAugmentEnabled(stack, augmentId, !currentlyEnabled);
    }

    // ---- GUI ----
    default InteractionResultHolder<ItemStack> onToolUse(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && hand == InteractionHand.MAIN_HAND) {
            if (!level.isClientSide) {
                openToolGui(player, stack);
            }
            return InteractionResultHolder.success(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    default void openToolGui(Player player, ItemStack stack) {
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new com.arsenalcrafting.mod.client.menu.ToolMenu(id, inventory, stack),
                Component.translatable("gui.arsenalcrafting.tool")
        ));
    }

    /**
     * Primary material used for repair cost.
     * Head material takes priority, fallback to base material.
     */
    default AlloyMaterial getRepairMaterial(ItemStack stack) {
        ToolAssemblyData data = stack.get(ModComponents.TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial head = AlloyMaterial.fromName(data.headMaterial());
            if (head != null) return head;
        }
        return getBaseMaterial();
    }

    default String getMaterialName(ItemStack stack) {
        AlloyMaterial mat = getRepairMaterial(stack);
        return mat != null ? mat.getName() : "";
    }

    /**
     * Value cost per 1 durability point.
     */
    default int getRepairValuePerDurability(ItemStack stack) {
        AlloyMaterial mat = getRepairMaterial(stack);
        if (mat == null) return 1;

        float mod = mat.getModifier();
        return mod <= 1.0f ? 1 : Math.round(mod);
    }

    // ---- TOOLTIP ----
    @OnlyIn(Dist.CLIENT)
    default void appendArsenalTooltip(ItemStack stack, Item.TooltipContext context,
                                      List<Component> tooltip, TooltipFlag flag) {
        ToolAugmentSlots slots = getAugmentSlots(stack);
        int usedSlots = slots.getUsedSlotCount();

        if (usedSlots > 0) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("tooltip.arsenalcrafting.installed_augments")
                    .withStyle(net.minecraft.ChatFormatting.GOLD));

            for (int i = 0; i < 4; i++) {
                String augmentId = slots.getAugmentInSlot(i);
                if (augmentId == null || augmentId.isEmpty()) continue;

                boolean enabled = isAugmentEnabled(stack, augmentId);
                net.minecraft.ChatFormatting color = enabled ? net.minecraft.ChatFormatting.GREEN : net.minecraft.ChatFormatting.RED;

                // Determine the correct translation key. If the augment has tiers, the key includes the tier.
                String translationKey;
                var def = AugmentManager.get(augmentId); // assumes AugmentManager.get(id) returns the augment definition used in LangGenerator
                if (def != null && def.hasTiers()) {
                    int tier = AugmentManager.getTier(augmentId, stack); // assumes this returns int (1..maxTier)
                    if (tier <= 0) tier = 1; // defensive fallback
                    translationKey = "augment.arsenalcrafting." + def.id() + "_augment.tier" + tier;
                } else {
                    // Fallback to whatever translation key AugmentManager already provides for non-tiered augments
                    translationKey = AugmentManager.getTranslationKey(augmentId);
                }

                Component augmentName = Component.translatable(translationKey)
                        .withStyle(color);

                if (enabled) {
                    augmentName = augmentName.copy()
                            .append(Component.literal(" ✓").withStyle(net.minecraft.ChatFormatting.GREEN));
                } else {
                    augmentName = augmentName.copy()
                            .append(Component.literal(" ✗").withStyle(net.minecraft.ChatFormatting.RED));
                }

                tooltip.add(Component.literal("  Slot " + (i + 1) + ": ").withStyle(net.minecraft.ChatFormatting.GRAY)
                        .append(augmentName));
            }
        }

        tooltip.add(Component.literal(""));
        tooltip.add(Component.translatable("item.arsenalcrafting.augment_slots",
                        usedSlots + "/" + getTotalAugmentSlots())
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
    }


    // ---- VALIDATION / INSTALLATION ----
    default boolean isValidAugmentForTool(String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        var handler = AugmentManager.getHandler(augmentId);
        if (handler != null) {
            return handler.supportsToolType(getToolType());
        }
        return false;
    }

    default boolean canInstallAugment(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        if (!isValidAugmentForTool(augmentId)) return false;
        if (getUsedAugmentSlots(stack) >= getTotalAugmentSlots()) return false;
        return !getAugmentSlots(stack).hasAugment(augmentId);
    }

    default int getFirstFreeSlot(ItemStack stack) {
        ToolAugmentSlots slots = getAugmentSlots(stack);
        for (int i = 0; i < 4; i++) {
            if (slots.getAugmentInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    default boolean installAugment(ItemStack stack, String augmentId, int tier) {
        if (!canInstallAugment(stack, augmentId)) return false;
        int freeSlot = getFirstFreeSlot(stack);
        if (freeSlot >= 0) {
            setAugmentInSlot(stack, freeSlot, augmentId);
            AugmentManager.install(augmentId, stack, tier);
            return true;
        }
        return false;
    }

    default boolean uninstallAugment(ItemStack stack, String augmentId) {
        if (augmentId == null || augmentId.isEmpty()) return false;
        ToolAugmentSlots slots = getAugmentSlots(stack);
        int slot = slots.findSlotForAugment(augmentId);
        if (slot >= 0) {
            clearAugmentSlot(stack, slot);
            AugmentManager.uninstall(augmentId, stack);
            return true;
        }
        return false;
    }
}
