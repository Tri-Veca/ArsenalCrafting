package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.items.augments.AutoSmeltAugmentItem;
import com.arsenalcrafting.mod.items.augments.OreMinerAugmentItem;
import com.arsenalcrafting.mod.registry.ModComponents;
import com.arsenalcrafting.mod.ArsenalCrafting;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
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
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity entity) {

        if (!level.isClientSide) {
            System.out.println("[ArsenalPickaxeItem] mineBlock intercepted for: " + state.getBlock().getName().getString());
        }

        // --- BEGIN TEST MODE ---
        if (TestModeConfig.isAutoSmeltEnabled() && !level.isClientSide) {
            if (!stack.has(ModComponents.AUTO_SMELT_ENABLED.get())) {
                stack.set(ModComponents.AUTO_SMELT_ENABLED.get(), true);
                System.out.println("[TestMode] Auto-Smelt forced onto stack during mining");

            }

            if(!stack.has(ModComponents.ORE_MINER_ENABLED.get())){
                stack.set(ModComponents.ORE_MINER_ENABLED.get(), true);
                System.out.println("[TestMode] Ore-Miner forced onto stack during mining");
            }
        }
        // --- END TEST MODE ---

        // --- Augment Check and Delegation ---
        if (!level.isClientSide) {

            // Check if the tool has the Auto-Smelt component enabled.
            boolean autoSmeltEnabled = stack.getOrDefault(ModComponents.AUTO_SMELT_ENABLED.get(), false);


            if (autoSmeltEnabled) {
                // --- DEBUG 2: Confirms the augment is enabled ---
                System.out.println("[ArsenalPickaxeItem] Auto Smelt AUGMENT FOUND! Delegating now.");

                // Delegate the block break handling to the augment class.
                boolean dropsHandled = AutoSmeltAugmentItem.onBlockMined(stack, level, state, pos, entity);

                // --- DEBUG 3: Confirm the result of the augment call ---
                if (dropsHandled) {
                    System.out.println("[ArsenalPickaxeItem] Augment finished: DROPS HANDLED (Smelted item was dropped).");
                } else {
                    System.out.println("[ArsenalPickaxeItem] Augment finished: DROPS NOT HANDLED (No recipe found, or block not harvestable).");
                }

                // We MUST still call super.mineBlock to ensure durability is consumed.
                return super.mineBlock(stack, level, state, pos, entity); // NEED TO FIND DIFFERENT WAY BECAUSE THEN WE DONT GO THE ORE MINER!!!!!
            } else {
                // --- DEBUG 4: Confirms the augment is not on the tool ---
                System.out.println("[ArsenalPickaxeItem] Auto Smelt AUGMENT NOT FOUND. Consuming durability.");
            }

            // Check if the tool has the Ore-Miner component enabled.
            boolean oreMinerEnabled = stack.getOrDefault(ModComponents.ORE_MINER_ENABLED.get(), false);

            if (oreMinerEnabled) {
                // --- DEBUG 2: Confirms the augment is enabled ---
                System.out.println("[ArsenalPickaxeItem] Ore Miner AUGMENT FOUND! Delegating now.");

                // Delegate the block break handling to the augment class.
                boolean dropsHandled = OreMinerAugmentItem.onBlockMined(stack, level, state, pos, entity);

                // --- DEBUG 3: Confirm the result of the augment call ---
                if (dropsHandled) {
                    System.out.println("[ArsenalPickaxeItem] Augment finished: DROPS HANDLED (Ore mine items were dropped).");
                } else {
                    System.out.println("[ArsenalPickaxeItem] Augment finished: DROPS NOT HANDLED (idk, or block not harvestable).");
                }

                // We MUST still call super.mineBlock to ensure durability is consumed.
                //return super.mineBlock(stack, level, state, pos, entity);

                // Return true because in the onBlockMined it consumes the durabillity

            } else {
                // --- DEBUG 4: Confirms the augment is not on the tool ---
                System.out.println("[ArsenalPickaxeItem] Ore Miner AUGMENT NOT FOUND. Consuming durability.");
            }
        }

        // If no augment, or on client side, proceed normally (consume durability and let vanilla handle drops).
        return super.mineBlock(stack, level, state, pos, entity);
    }


    // Tooltip to show base stats and augment slots
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
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