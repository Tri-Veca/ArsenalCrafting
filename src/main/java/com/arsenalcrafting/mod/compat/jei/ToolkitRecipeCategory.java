package com.arsenalcrafting.mod.compat.jei;

import com.arsenalcrafting.mod.ArsenalCrafting;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.alloy.ToolType;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.tool.ArsenalToolType;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.arsenalcrafting.mod.registry.ModComponents;
import com.arsenalcrafting.mod.registry.ModItems;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
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
import net.minecraft.world.item.crafting.Ingredient;

import java.util.*;
import java.util.stream.Collectors;

public class ToolkitRecipeCategory implements IRecipeCategory<ToolkitRecipeCategory.ToolkitRecipe> {

    public static final ResourceLocation UID = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID, "toolkit");
    public static final RecipeType<ToolkitRecipe> RECIPE_TYPE = new RecipeType<>(UID, ToolkitRecipe.class);

    private final IDrawable background;
    private final IDrawable icon;
    private final IGuiHelper guiHelper;

    // GUI slot positions
    private static final int HEAD_SLOT_X = 49;
    private static final int HEAD_SLOT_Y = 6;

    private static final int CORE_SLOT_X = 49;
    private static final int CORE_SLOT_Y = 29;

    private static final int ROD_SLOT_X = 49;
    private static final int ROD_SLOT_Y = 52;

    private static final int OUTPUT_SLOT_X = 116;
    private static final int OUTPUT_SLOT_Y = 20;

    // Augment slots
    private static final int[][] AUGMENT_SLOTS = {
            {84, 61},   // Slot 1
            {107, 61},  // Slot 2
            {129, 61},  // Slot 3
            {152, 61}   // Slot 4
    };

    // Cache of all possible ingredients by type
    private static Map<ToolComponentType, List<ItemStack>> COMPONENT_CACHE = null;
    private static Map<ToolComponentType, Ingredient> INGREDIENT_CACHE = null;

    public ToolkitRecipeCategory(IGuiHelper helper) {
        this.guiHelper = helper;
        ResourceLocation texture = ResourceLocation.fromNamespaceAndPath(ArsenalCrafting.MOD_ID,
                "textures/gui/container/toolkit_gui.png");
        this.background = helper.createDrawable(texture, 0, 0, 176, 85);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK,
                new ItemStack(ModBlocks.TOOLKIT.get()));

        // Initialize component cache
        if (COMPONENT_CACHE == null) {
            buildComponentCache();
        }
    }

    private void buildComponentCache() {
        COMPONENT_CACHE = new HashMap<>();
        INGREDIENT_CACHE = new HashMap<>();

        // Group all tool components by their type
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem comp) {
                COMPONENT_CACHE.computeIfAbsent(comp.getComponentType(), k -> new ArrayList<>())
                        .add(new ItemStack(item));
            }
        }

        // Sort each list by material tier
        for (var entry : COMPONENT_CACHE.entrySet()) {
            var list = entry.getValue();
            list.sort((a, b) -> {
                var matA = ((com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem)a.getItem()).getMaterial();
                var matB = ((com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem)b.getItem()).getMaterial();
                return Integer.compare(matA.getTier(), matB.getTier());
            });

            // Create Ingredient from the list
            INGREDIENT_CACHE.put(entry.getKey(), Ingredient.of(list.stream()));
        }
    }

    @Override
    public RecipeType<ToolkitRecipe> getRecipeType() {
        return RECIPE_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.arsenalcrafting.toolkit");
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
    public void setRecipe(IRecipeLayoutBuilder builder, ToolkitRecipe recipe, IFocusGroup focuses) {

        // --- INGREDIENT SLOTS WITH ALL POSSIBLE MATERIALS ---

        // Head slot - use Ingredient for proper JEI cycling
        Ingredient headIngredient = getIngredientForType(recipe.toolType().getHeadType());
        builder.addSlot(RecipeIngredientRole.INPUT, HEAD_SLOT_X, HEAD_SLOT_Y)
                .addIngredients(headIngredient)
                .setSlotName("head")
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.literal("Tool Head").withStyle(ChatFormatting.GOLD));
                    tooltip.add(Component.literal("Click arrows to cycle materials").withStyle(ChatFormatting.GRAY));

                    // Show current material if possible
                    Optional<ItemStack> displayed = slotView.getDisplayedIngredient(VanillaTypes.ITEM_STACK);
                    displayed.ifPresent(stack -> {
                        if (stack.getItem() instanceof com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem comp) {
                            tooltip.add(Component.empty());
                            tooltip.add(Component.literal("Current: " + comp.getMaterial().getName()).withStyle(comp.getMaterial().getColor()));
                        }
                    });
                });

        // Core slot
        Ingredient coreIngredient = getIngredientForType(ToolComponentType.CORE);
        builder.addSlot(RecipeIngredientRole.INPUT, CORE_SLOT_X, CORE_SLOT_Y)
                .addIngredients(coreIngredient)
                .setSlotName("core")
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.literal("Tool Core").withStyle(ChatFormatting.LIGHT_PURPLE));
                    tooltip.add(Component.literal("Click arrows to cycle materials").withStyle(ChatFormatting.GRAY));

                    Optional<ItemStack> displayed = slotView.getDisplayedIngredient(VanillaTypes.ITEM_STACK);
                    displayed.ifPresent(stack -> {
                        if (stack.getItem() instanceof com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem comp) {
                            tooltip.add(Component.empty());
                            tooltip.add(Component.literal("Current: " + comp.getMaterial().getName()).withStyle(comp.getMaterial().getColor()));
                        }
                    });
                });

        // Rod slot
        Ingredient rodIngredient = getIngredientForType(recipe.toolType().getRodType());
        builder.addSlot(RecipeIngredientRole.INPUT, ROD_SLOT_X, ROD_SLOT_Y)
                .addIngredients(rodIngredient)
                .setSlotName("rod")
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.literal("Tool Rod").withStyle(ChatFormatting.YELLOW));
                    tooltip.add(Component.literal("Click arrows to cycle materials").withStyle(ChatFormatting.GRAY));

                    Optional<ItemStack> displayed = slotView.getDisplayedIngredient(VanillaTypes.ITEM_STACK);
                    displayed.ifPresent(stack -> {
                        if (stack.getItem() instanceof com.arsenalcrafting.mod.items.tool.toolparts.ToolComponentItem comp) {
                            tooltip.add(Component.empty());
                            tooltip.add(Component.literal("Current: " + comp.getMaterial().getName()).withStyle(comp.getMaterial().getColor()));
                        }
                    });
                });

        // --- OUTPUT SLOT - Shows the selected combination ---
        ItemStack resultTool = createToolWithMaterials(
                recipe.toolType(),
                recipe.headMaterial(),
                recipe.rodMaterial(),
                recipe.coreMaterial()
        );

        builder.addSlot(RecipeIngredientRole.OUTPUT, OUTPUT_SLOT_X, OUTPUT_SLOT_Y)
                .addItemStack(resultTool)
                .setSlotName("output")
                .addTooltipCallback((slotView, tooltip) -> {
                    tooltip.add(Component.literal("Assembled Tool").withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD));
                    tooltip.add(Component.empty());
                    tooltip.add(Component.literal("Current Selection:").withStyle(ChatFormatting.GOLD));

                    // Get currently selected materials from the slots
                    String headMat = getSelectedMaterial(focuses, "head");
                    String rodMat = getSelectedMaterial(focuses, "rod");
                    String coreMat = getSelectedMaterial(focuses, "core");

                    tooltip.add(Component.literal("  Head: " + headMat).withStyle(ChatFormatting.WHITE));
                    tooltip.add(Component.literal("  Rod: " + rodMat).withStyle(ChatFormatting.WHITE));
                    tooltip.add(Component.literal("  Core: " + coreMat).withStyle(ChatFormatting.WHITE));
                    tooltip.add(Component.empty());

                    // Calculate stats based on selected materials
                    addToolStats(tooltip, headMat, rodMat, coreMat);
                    tooltip.add(Component.empty());

                    tooltip.add(Component.literal("Compatible Augments:").withStyle(ChatFormatting.AQUA));
                    addCompatibleAugments(tooltip, recipe.toolType().getToolType());
                    tooltip.add(Component.empty());
                    tooltip.add(Component.literal("← → Click arrows to change materials").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
                });

        // --- AUGMENT PREVIEW SECTION ---
        ArsenalToolType toolType = recipe.toolType().getToolType();
        List<AugmentManager.AugmentDefinition> compatibleAugments = AugmentManager.getAugmentsForToolType(toolType);

        int augmentIndex = 0;
        for (AugmentManager.AugmentDefinition augmentDef : compatibleAugments) {
            if (augmentIndex < AUGMENT_SLOTS.length) {
                int x = AUGMENT_SLOTS[augmentIndex][0];
                int y = AUGMENT_SLOTS[augmentIndex][1];

                ItemStack augmentStack = getAugmentStack(augmentDef.id(), 1);
                if (!augmentStack.isEmpty()) {
                    builder.addSlot(RecipeIngredientRole.CATALYST, x, y)
                            .addItemStack(augmentStack)
                            .setSlotName("augment_" + augmentIndex)
                            .addTooltipCallback((slotView, tooltip) -> {
                                tooltip.add(Component.literal(augmentDef.name()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD));
                                tooltip.add(Component.literal("Compatible with " + toolType.name()).withStyle(ChatFormatting.GRAY));
                                tooltip.add(Component.empty());
                                tooltip.add(Component.literal(augmentDef.description()).withStyle(ChatFormatting.DARK_GRAY));

                                if (augmentDef.hasTiers()) {
                                    tooltip.add(Component.empty());
                                    tooltip.add(Component.literal("Tiers: 1-" + augmentDef.maxTier()).withStyle(ChatFormatting.YELLOW));
                                }

                                // Show all tiers if available
                                if (augmentDef.hasTiers()) {
                                    tooltip.add(Component.empty());
                                    tooltip.add(Component.literal("Available Tiers:").withStyle(ChatFormatting.GOLD));
                                    for (int t = 1; t <= augmentDef.maxTier(); t++) {
                                        tooltip.add(Component.literal("  Tier " + t).withStyle(ChatFormatting.WHITE));
                                    }
                                }
                            });
                    augmentIndex++;
                }
            }
        }

        // If there are more augments than slots, add a note
        if (compatibleAugments.size() > AUGMENT_SLOTS.length) {
            // You could add a text note somewhere, but JEI doesn't easily support text in the recipe area
        }
    }

    private String getSelectedMaterial(IFocusGroup focuses, String slotName) {
        // This would need to get the currently selected item from the slot
        // For now, return a default
        return "Selected";
    }

    private Ingredient getIngredientForType(ToolComponentType type) {
        return INGREDIENT_CACHE.getOrDefault(type, Ingredient.EMPTY);
    }

    private ItemStack createToolWithMaterials(ToolType toolType, AlloyMaterial head, AlloyMaterial rod, AlloyMaterial core) {
        ItemStack tool = new ItemStack(toolType.getToolItem());
        ToolAssemblyData assemblyData = new ToolAssemblyData(
                head.getName(),
                rod.getName(),
                core.getName()
        );
        tool.set(ModComponents.TOOL_ASSEMBLY_DATA.get(), assemblyData);
        return tool;
    }

    private void addToolStats(List<Component> tooltip, String headMat, String rodMat, String coreMat) {
        // This would need to look up the actual materials
        // For now, show placeholder
        tooltip.add(Component.literal("Tool Statistics:").withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.literal("  ⚔ Durability: Combined").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("  ⛏ Efficiency: Based on materials").withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.literal("  ⚡ Damage: Material dependent").withStyle(ChatFormatting.WHITE));
    }

    private void addCompatibleAugments(List<Component> tooltip, ArsenalToolType toolType) {
        List<AugmentManager.AugmentDefinition> augments = AugmentManager.getAugmentsForToolType(toolType);
        if (augments.isEmpty()) {
            tooltip.add(Component.literal("  None").withStyle(ChatFormatting.GRAY));
        } else {
            // Show first 4, then indicate more
            for (int i = 0; i < Math.min(4, augments.size()); i++) {
                AugmentManager.AugmentDefinition def = augments.get(i);
                tooltip.add(Component.literal("  • " + def.name()).withStyle(ChatFormatting.AQUA));
            }
            if (augments.size() > 4) {
                tooltip.add(Component.literal("  ...and " + (augments.size() - 4) + " more").withStyle(ChatFormatting.GRAY));
                tooltip.add(Component.literal("  (Scroll down to see all)").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
            }
        }
    }

    private ItemStack getAugmentStack(String augmentId, int tier) {
        for (var entry : ModItems.ITEMS.getEntries()) {
            Item item = entry.get();
            if (item instanceof com.arsenalcrafting.mod.items.augments.UniversalAugmentItem augment) {
                if (augment.getAugmentId().equals(augmentId)) {
                    if (!augment.hasTiers() || augment.getTier() == tier) {
                        return new ItemStack(item);
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public List<Component> getTooltipStrings(ToolkitRecipe recipe, IRecipeSlotsView recipeSlotsView, double mouseX, double mouseY) {
        // Check if mouse is over the area where we want to show additional info
        if (mouseX >= 10 && mouseX <= 80 && mouseY >= 70 && mouseY <= 85) {
            List<Component> tooltips = new ArrayList<>();
            tooltips.add(Component.literal("Available Materials:").withStyle(ChatFormatting.GOLD));

            // List all materials
            for (AlloyMaterial mat : AlloyMaterial.values()) {
                tooltips.add(Component.literal("  • " + mat.getName()).withStyle(mat.getColor()));
            }
            return tooltips;
        }
        return Collections.emptyList();
    }

    // Recipe record
    public record ToolkitRecipe(
            ToolType toolType,
            AlloyMaterial headMaterial,
            AlloyMaterial rodMaterial,
            AlloyMaterial coreMaterial
    ) {
        public ArsenalToolType getArsenalToolType() {
            return toolType.getToolType();
        }

        // Static method to create a single recipe per tool type
        public static List<ToolkitRecipe> createTemplates() {
            List<ToolkitRecipe> templates = new ArrayList<>();
            AlloyMaterial defaultMat = AlloyMaterial.IRON; // Default shown material

            for (ToolType toolType : ToolType.values()) {
                templates.add(new ToolkitRecipe(toolType, defaultMat, defaultMat, defaultMat));
            }
            return templates;
        }
    }
}