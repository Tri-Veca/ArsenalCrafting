package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.augments.UniversalAugmentItem;
import com.arsenalcrafting.mod.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class AugmentInfoCategory implements IRecipeCategory<AugmentManager.AugmentDefinition> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "augment_info");
    public static final RecipeType<AugmentManager.AugmentDefinition> RECIPE_TYPE = new RecipeType<>(UID, AugmentManager.AugmentDefinition.class);

    private final IDrawable background;
    private final IDrawable icon;

    public AugmentInfoCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(180, 120);

        // Find any augment for the icon
        ItemStack iconStack = ItemStack.EMPTY;
        for (var entry : ModItems.ITEMS.getEntries()) {
            if (entry.get() instanceof UniversalAugmentItem) {
                iconStack = new ItemStack(entry.get());
                break;
            }
        }
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, iconStack);
    }

    @Override
    public RecipeType<AugmentManager.AugmentDefinition> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.arsenalcrafting.augments");
    }

    @Override
    public IDrawable getBackground() {
        return background;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, AugmentManager.AugmentDefinition recipe, IFocusGroup focuses) {
        int x = 20;
        int y = 20;

        // Show all tiers of this augment
        if (recipe.hasTiers()) {
            for (int tier = 1; tier <= recipe.maxTier(); tier++) {
                ItemStack augmentStack = getAugmentStack(recipe.id(), tier);
                if (!augmentStack.isEmpty()) {
                    int finalTier = tier;
                    builder.addSlot(RecipeIngredientRole.RENDER_ONLY, x, y)
                            .addItemStack(augmentStack)
                            .addTooltipCallback((slotView, tooltip) -> {
                                tooltip.add(Component.literal(recipe.name() + " Tier " + finalTier)
                                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                                tooltip.add(Component.empty());
                                tooltip.add(Component.literal(recipe.description())
                                        .withStyle(ChatFormatting.GRAY));
                                tooltip.add(Component.empty());
                                tooltip.add(Component.literal("Compatible Tools:")
                                        .withStyle(ChatFormatting.YELLOW));

                                // You could add tool compatibility info here
                                // This would require a method to get compatible tools for an augment
                            });
                    x += 40;
                    if (x > 140) {
                        x = 20;
                        y += 40;
                    }
                }
            }
        } else {
            // Single-tier augment
            ItemStack augmentStack = getAugmentStack(recipe.id(), 1);
            if (!augmentStack.isEmpty()) {
                builder.addSlot(RecipeIngredientRole.RENDER_ONLY, 80, 50)
                        .addItemStack(augmentStack)
                        .addTooltipCallback((slotView, tooltip) -> {
                            tooltip.add(Component.literal(recipe.name())
                                    .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                            tooltip.add(Component.empty());
                            tooltip.add(Component.literal(recipe.description())
                                    .withStyle(ChatFormatting.GRAY));
                            tooltip.add(Component.empty());
                            tooltip.add(Component.literal("Compatible Tools:")
                                    .withStyle(ChatFormatting.YELLOW));
                            // Add tool compatibility info
                        });
            }
        }
    }

    private ItemStack getAugmentStack(String augmentId, int tier) {
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof UniversalAugmentItem augment) {
                if (augment.getAugmentId().equals(augmentId)) {
                    if (!augment.hasTiers() || augment.getTier() == tier) {
                        return new ItemStack(item);
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
}