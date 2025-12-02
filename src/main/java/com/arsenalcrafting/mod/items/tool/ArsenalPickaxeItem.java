package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
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
        // Retrieve dynamic material data from Data Components
        AlloyMaterial currentMaterial = baseMaterial; // Start with base material

        // Fetch tool assembly data using the modern component system (getOrDefault)
        ToolAssemblyData assemblyData = stack.getOrDefault(ModComponents.TOOL_ASSEMBLY_DATA.get(), ToolAssemblyData.EMPTY);

        if (!assemblyData.isEmpty()) {
            tooltip.add(Component.literal("")); // Separator
            tooltip.add(Component.translatable("item.arsenalcrafting.tool_parts").withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable("item.arsenalcrafting.head_material", assemblyData.headMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.rod_material", assemblyData.rodMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.literal("")); // Separator
        }

        // Display augment status if present
        boolean autoSmeltEnabled = stack.getOrDefault(ModComponents.AUTO_SMELT_ENABLED.get(), false);
        boolean oreMinerEnabled = stack.getOrDefault(ModComponents.ORE_MINER_ENABLED.get(), false);
        if (autoSmeltEnabled) {
            tooltip.add(Component.translatable("augment.arsenalcrafting.auto_smelt").withStyle(ChatFormatting.GOLD));

            // Only show test mode warning if we're actually in test mode
            if (TestModeConfig.isAutoSmeltEnabled()) {
                tooltip.add(Component.literal("TEST MODE: Auto Smelt").withStyle(ChatFormatting.RED));
            }

        }
        if (oreMinerEnabled) {
            tooltip.add(Component.translatable("augment.arsenalcrafting.ore_miner").withStyle(ChatFormatting.GOLD));

            // Only show test mode warning if we're actually in test mode
            if (TestModeConfig.isOreMinerEnabled()) {
                tooltip.add(Component.literal("TEST MODE: Ore Miner").withStyle(ChatFormatting.RED));
            }

        }

        // Title and Durability (Base Stats)
        tooltip.add(Component.translatable("item.arsenalcrafting.tool_material", currentMaterial.getName())
                .withStyle(currentMaterial.getColor()));
        tooltip.add(Component.translatable("item.arsenalcrafting.durability", currentMaterial.getDurability())
                .withStyle(currentMaterial.getColor()));

        // Efficiency and Augment Modifier
        tooltip.add(Component.translatable("item.arsenalcrafting.efficiency", String.format("%.1f", currentMaterial.getEfficiency()))
                .withStyle(ChatFormatting.BLUE));
        tooltip.add(Component.translatable("item.arsenalcrafting.repairability", String.format("%.2f", currentMaterial.getModifier()))
                .withStyle(ChatFormatting.DARK_GREEN));

        // Augment Slots
        int totalSlots = currentMaterial.getToolAugmentSlots();
        int usedSlots = countUsedAugmentSlots(stack);

        tooltip.add(Component.literal("")); // Empty line for separation
        tooltip.add(Component.translatable("item.arsenalcrafting.augment_slots",
                        usedSlots + "/" + totalSlots)
                .withStyle(ChatFormatting.LIGHT_PURPLE));
    }

    // Add this helper method to count used augment slots
    private int countUsedAugmentSlots(ItemStack stack) {
        int usedSlots = 0;

        // Count each augment type that's installed
        if (stack.getOrDefault(ModComponents.AUTO_SMELT_ENABLED.get(), false)) {
            usedSlots++;
        }

        if (stack.getOrDefault(ModComponents.ORE_MINER_ENABLED.get(), false)) {
            usedSlots++;
        }
        // Add more augment checks here as you implement them:
        // if (stack.get(ModComponents.LIFE_STEAL_ENABLED.get())) { usedSlots++; }
        // if (stack.get(ModComponents.FORTUNE_ENABLED.get())) { usedSlots++; }

        return usedSlots;
    }

    // Prevent Enchantments on Arsenal Tools
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}