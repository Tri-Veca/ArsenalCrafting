package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
import com.arsenalcrafting.mod.component.DynamicComponents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ArsenalPickaxeItem extends PickaxeItem {
    private final AlloyMaterial baseMaterial;

    public ArsenalPickaxeItem(AlloyMaterial material, Properties properties) {
        super(new DummyToolTier(material), properties);
        this.baseMaterial = material;
    }

    public AlloyMaterial getBaseMaterial() {
        return baseMaterial;
    }


    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();

        // Test mode: Install ALL augments that support pickaxes
        if (TestModeConfig.isTestModeEnabled()) {
            System.out.println("[TestMode] Installing all pickaxe augments on new pickaxe");

            for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
                var handler = AugmentManager.getHandler(def.id());
                if (handler != null && handler.supportsToolType(ArsenalToolType.PICKAXE)) {
                    // Check if this specific augment is enabled in test mode
                    if (TestModeConfig.isAugmentEnabled(def.id())) {
                        AugmentManager.install(def.id(), stack, def.hasTiers() ? def.maxTier() : 1);
                        System.out.println("[TestMode] Installed: " + def.name());
                    }
                }
            }
        }

        return stack;
    }

    // Custom Tier implementation to read stats from AlloyMaterial
    private static class DummyToolTier implements Tier {
        private final AlloyMaterial material;

        public DummyToolTier(AlloyMaterial material) {
            this.material = material;
        }

        @Override
        public int getUses() {
            return material.getDurability();
        }

        @Override
        public float getSpeed() {
            return material.getEfficiency();
        }

        @Override
        public float getAttackDamageBonus() {
            return material.getDamage();
        }

        @Override
        public TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.INCORRECT_FOR_IRON_TOOL;
        }

        @Override
        public int getEnchantmentValue() {
            return 15;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    }

    /**
     * Overrides the block breaking logic to check for and delegate to augments.
     */
    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockState state,
                             @NotNull BlockPos pos, @NotNull LivingEntity entity) {

        if (!level.isClientSide) {
            System.out.println("[ArsenalPickaxe] Processing augments for block: " +
                    state.getBlock().getName().getString());

            // Process through AugmentProcessor with PICKAXE tool type
            AugmentProcessor.ProcessResult result = AugmentProcessor.processBlockBreak(
                    stack,
                    ArsenalToolType.PICKAXE,
                    level,
                    state,
                    pos,
                    entity
            );

            if (result.handled) {
                System.out.println("[ArsenalPickaxe] Augments handled drops - SKIPPING super.mineBlock()");
                EquipmentSlot slot = entity.getMainHandItem() == stack
                        ? EquipmentSlot.MAINHAND
                        : EquipmentSlot.OFFHAND;

                stack.hurtAndBreak(1, entity, slot);

                // Apply combo effects if any
                if (result.specialCombo != null) {
                    System.out.println("[ArsenalPickaxe] Special combo: " + result.specialCombo);
                    // Add any combo-specific effects here
                }

                return true; // This should prevent vanilla drops!
            } else {
                System.out.println("[ArsenalPickaxe] No augment handled this block, using vanilla");
            }
        }

        // No augment handled it, use vanilla behavior
        return super.mineBlock(stack, level, state, pos, entity);
    }

    // Tooltip to show base stats and augment slots
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {

        AlloyMaterial currentMaterial = baseMaterial; // default

        // =======================
        // TOOL ASSEMBLY DATA (if any)
        // =======================
        ToolAssemblyData assemblyData = stack.getOrDefault(
                com.arsenalcrafting.mod.registry.ModComponents.TOOL_ASSEMBLY_DATA.get(),
                ToolAssemblyData.EMPTY
        );

        if (!assemblyData.isEmpty()) {
            tooltip.add(Component.literal("")); // Separator
            tooltip.add(Component.translatable("item.arsenalcrafting.tool_parts").withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable("item.arsenalcrafting.head_material", assemblyData.headMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.rod_material", assemblyData.rodMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.literal("")); // Separator

            // Try to map head/rod strings back to AlloyMaterial
            AlloyMaterial headMat = AlloyMaterial.fromName(assemblyData.headMaterial());
            AlloyMaterial rodMat  = AlloyMaterial.fromName(assemblyData.rodMaterial());

            AlloyMaterial best = AlloyMaterial.highestOf(currentMaterial, headMat, rodMat);
            if (best != null) {
                currentMaterial = best;
            }
        }

        // =======================
        // ACTIVE AUGMENTS
        // =======================
        boolean hasAnyAugments = false;
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            if (AugmentManager.isInstalled(def.id(), stack)) {
                if (!hasAnyAugments) {
                    hasAnyAugments = true;
                }

                Component augmentName = Component.translatable(AugmentManager.getTranslationKey(def.id()))
                        .withStyle(ChatFormatting.GOLD);

                if (def.hasTiers()) {
                    int tier = AugmentManager.getTier(def.id(), stack);
                    augmentName = Component.translatable(
                            "augment.arsenalcrafting." + def.id() +".tier" + String.valueOf(tier))
                    .withStyle(ChatFormatting.GOLD);
                }

                tooltip.add(augmentName);

                if (TestModeConfig.isAugmentEnabled(def.id())) {
                    tooltip.add(Component.literal("TEST MODE: " + def.name())
                            .withStyle(ChatFormatting.RED));
                }
            }
        }

        // =======================
        // MATERIAL BASE STATS (using highest material)
        // =======================
        tooltip.add(Component.literal("")); // Separator


        if(!assemblyData.isEmpty()){
            AlloyMaterial headMat = AlloyMaterial.fromName(assemblyData.headMaterial());
            AlloyMaterial rodMat  = AlloyMaterial.fromName(assemblyData.rodMaterial());

            tooltip.add(Component.translatable("item.arsenalcrafting.repairability", headMat.getName())
                    .withStyle(headMat.getColor()));

            tooltip.add(Component.translatable("item.arsenalcrafting.durability", rodMat.getDurability())
                    .withStyle(rodMat.getColor()));

            tooltip.add(Component.translatable("item.arsenalcrafting.efficiency",
                            String.format("%.1f", headMat.getEfficiency()))
                    .withStyle(ChatFormatting.BLUE));

        }




        // =======================
        // AUGMENT SLOTS
        // =======================
        int totalSlots = currentMaterial.getToolAugmentSlots();
        int usedSlots = countUsedAugmentSlots(stack);

        tooltip.add(Component.literal("")); // Separator
        tooltip.add(
                Component.translatable(
                        "item.arsenalcrafting.augment_slots",
                        usedSlots + "/" + totalSlots
                ).withStyle(ChatFormatting.LIGHT_PURPLE)
        );
    }

    // Add this helper method to count used augment slots
    private int countUsedAugmentSlots(ItemStack stack) {
        int usedSlots = 0;

        // Loop ALL augments from the registry
        for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
            if (AugmentManager.isInstalled(def.id(), stack)) {
                usedSlots++;
            }
        }

        return usedSlots;
    }

    // Prevent Enchantments on Arsenal Tools
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}