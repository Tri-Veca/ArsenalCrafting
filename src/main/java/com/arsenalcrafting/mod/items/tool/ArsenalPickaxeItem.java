package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
import com.arsenalcrafting.mod.core.augments.AugmentType;
import com.arsenalcrafting.mod.registry.ModComponents;
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

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();

        // Clean, centralized test mode logic
        if (TestModeConfig.isAutoSmeltEnabled()) {
            stack.set(ModComponents.AUTO_SMELT_ENABLED.get(), true);
            System.out.println("[TestMode] Pre-installed Auto-Smelt on new pickaxe");
        }
        // Clean, centralized test mode logic
        if (TestModeConfig.isOreMinerEnabled()) {
            stack.set(ModComponents.ORE_MINER_ENABLED.get(), true);
            System.out.println("[TestMode] Pre-installed Ore-Miner on new pickaxe");
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
                    ArsenalToolType.PICKAXE,  // Use ToolType enum
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
                return true; // This should prevent vanilla drops!
            }

                // Apply combo effects if any
                if (result.specialCombo != null) {
                    System.out.println("[ArsenalPickaxe] Special combo: " + result.specialCombo);
                    // Add any combo-specific effects here
                }

                return true; // Drops were handled by augments
            } else {
                System.out.println("[ArsenalPickaxe] No augment handled this block, using vanilla");
            }


        // No augment handled it, use vanilla behavior
        return super.mineBlock(stack, level, state, pos, entity);
    }

    // Tooltip to show base stats and augment slots
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {

        AlloyMaterial currentMaterial = baseMaterial;

        // =======================
        // PART MATERIALS
        // =======================
        ToolAssemblyData assemblyData = stack.getOrDefault(
                ModComponents.TOOL_ASSEMBLY_DATA.get(),
                ToolAssemblyData.EMPTY
        );

        if (!assemblyData.isEmpty()) {
            tooltip.add(Component.literal("")); // Separator
            tooltip.add(Component.translatable("item.arsenalcrafting.tool_parts")
                    .withStyle(ChatFormatting.DARK_AQUA));

            tooltip.add(Component.translatable(
                    "item.arsenalcrafting.head_material",
                    assemblyData.headMaterial()
            ).withStyle(ChatFormatting.WHITE));

            tooltip.add(Component.translatable(
                    "item.arsenalcrafting.rod_material",
                    assemblyData.rodMaterial()
            ).withStyle(ChatFormatting.WHITE));

            tooltip.add(Component.literal("")); // Separator
        }


        // =======================
        // AUGMENTS
        // =======================
        List<AugmentType> activeAugments =
                AugmentProcessor.getActiveAugments(stack, ArsenalToolType.PICKAXE);

        if (!activeAugments.isEmpty()) {
            tooltip.add(Component.literal("")); // Separator

            for (AugmentType augment : activeAugments) {

                // ⭐ FORTUNE SPECIAL CASE: show Tier
                if (augment == AugmentType.FORTUNE) {
                    int tier = stack.getOrDefault(ModComponents.FORTUNE_TIER.get(), 1);

                    tooltip.add(
                            Component.translatable(augment.translationKey())
                                    .append(" ")
                                    .append(Component.literal("Tier " + tier).withStyle(ChatFormatting.YELLOW))
                                    .withStyle(ChatFormatting.GOLD)
                    );
                }

                // Normal augments
                else {
                    tooltip.add(
                            Component.translatable(augment.translationKey())
                                    .withStyle(ChatFormatting.GOLD)
                    );
                }

                // TEST MODE line
                if (augment.isEnabledInTestMode()) {
                    tooltip.add(
                            Component.literal("TEST MODE: " + augment.id())
                                    .withStyle(ChatFormatting.RED)
                    );
                }
            }
        }


        // =======================
        // MATERIAL BASE STATS
        // =======================
        tooltip.add(Component.translatable("item.arsenalcrafting.tool_material", currentMaterial.getName())
                .withStyle(currentMaterial.getColor()));

        tooltip.add(Component.translatable("item.arsenalcrafting.durability", currentMaterial.getDurability())
                .withStyle(currentMaterial.getColor()));

        tooltip.add(Component.translatable("item.arsenalcrafting.efficiency",
                        String.format("%.1f", currentMaterial.getEfficiency()))
                .withStyle(ChatFormatting.BLUE));

        tooltip.add(Component.translatable("item.arsenalcrafting.repairability",
                        String.format("%.2f", currentMaterial.getModifier()))
                .withStyle(ChatFormatting.DARK_GREEN));


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

        // Loop ALL augments from the enum
        for (AugmentType augment : AugmentType.values()) {

            // Each augment has its own data component flag
            if (stack.getOrDefault(augment.flag(), false)) {
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