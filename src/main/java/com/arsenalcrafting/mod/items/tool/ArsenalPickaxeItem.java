package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static com.arsenalcrafting.mod.registry.ModComponents.TOOL_ASSEMBLY_DATA;

public class ArsenalPickaxeItem extends PickaxeItem implements ArsenalTool {
    private final ArsenalToolType toolType = ArsenalToolType.PICKAXE;
    private final AlloyMaterial baseMaterial;

    public ArsenalPickaxeItem(AlloyMaterial material, Properties properties) {
        super(new DummyToolTier(material), properties);
        this.baseMaterial = material;
    }

    @Override
    public ArsenalToolType getToolType() {
        return toolType;
    }

    @Override
    public AlloyMaterial getBaseMaterial() {
        return baseMaterial;
    }

    // Override use method to handle shift-right click for GUI
    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Handle shift-right click for GUI
        InteractionResultHolder<ItemStack> arsenalResult = onToolUse(level, player, hand);
        if (arsenalResult.getResult().consumesAction()) {
            return arsenalResult;
        }

        return super.use(level, player, hand);
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
                        // Install to first available slot
                        int freeSlot = getFirstFreeSlot(stack);
                        if (freeSlot >= 0) {
                            // Use the immutable method from ArsenalTool interface
                            setAugmentInSlot(stack, freeSlot, def.id());
                            AugmentManager.install(def.id(), stack, def.hasTiers() ? def.maxTier() : 1);
                            System.out.println("[TestMode] Installed: " + def.name() + " in slot " + freeSlot);
                        }
                    }
                }
            }
        }

        return stack;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());

        if (assemblyData != null && !assemblyData.isEmpty()) {
            AlloyMaterial rodMat = AlloyMaterial.fromName(assemblyData.rodMaterial());
            if (rodMat != null) {
                return rodMat.getDurability();
            }
        }
        return baseMaterial.getDurability();
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // 1. Start with vanilla Pickaxe speed logic
        float speed = super.getDestroySpeed(stack, state);

        // 2. Apply head material efficiency
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial headMat = AlloyMaterial.fromName(data.headMaterial());
            if (headMat != null) {
                speed = headMat.getEfficiency();
            }

            // 3. Apply core modifier (SpeedBoost, etc.)
            AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
            if (coreMat != null) {
                CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                if (def != null) {
                    speed = def.handler().modifyMiningSpeed(stack, speed);
                }
            }
        }

        return speed;
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
            return 0; // Arsenal tools cannot be enchanted
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY; // Arsenal tools use repair kits
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

            // Only process augments that are enabled
            ToolAugmentSlots slots = getAugmentSlots(stack);
            boolean hasEnabledAugments = false;

            for (int i = 0; i < 4; i++) {
                String augmentId = slots.getAugmentInSlot(i);
                if (augmentId != null && isAugmentEnabled(stack, augmentId)) {
                    hasEnabledAugments = true;
                    break;
                }
            }

            if (hasEnabledAugments) {
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
        }

        // No enabled augment handled it, use vanilla behavior
        return super.mineBlock(stack, level, state, pos, entity);
    }



    // Tooltip to show base stats and augment slots
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        // Call ArsenalTool tooltip logic
        appendArsenalTooltip(stack, context, tooltip, flag);

        // Add material stats
        ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());

        if (assemblyData != null && !assemblyData.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("item.arsenalcrafting.tool_parts").withStyle(ChatFormatting.DARK_AQUA));
            tooltip.add(Component.translatable("item.arsenalcrafting.head_material", assemblyData.headMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.rod_material", assemblyData.rodMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.core_material", assemblyData.coreMaterial()).withStyle(ChatFormatting.WHITE));

            AlloyMaterial headMat = AlloyMaterial.fromName(assemblyData.headMaterial());
            AlloyMaterial rodMat = AlloyMaterial.fromName(assemblyData.rodMaterial());
            AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());

            if (headMat != null && rodMat != null && coreMat != null) {
                tooltip.add(Component.literal(""));
                tooltip.add(Component.translatable("item.arsenalcrafting.repairability", headMat.getName())
                        .withStyle(headMat.getColor()));
                tooltip.add(Component.translatable("item.arsenalcrafting.durability", rodMat.getDurability())
                        .withStyle(rodMat.getColor()));
                tooltip.add(Component.translatable("item.arsenalcrafting.efficiency",
                                String.format("%.1f", headMat.getEfficiency()))
                        .withStyle(headMat.getColor()));

                CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat);


                if (coreDef != null) {
                    tooltip.add(Component.literal("• ")
                            .append(Component.translatable("core.arsenalcrafting." + coreDef.id())
                                    .withStyle(coreMat.getColor()))); //Core Tooltip

                    float base = headMat.getEfficiency();
                    float modified = coreDef.handler().modifyMiningSpeed(stack, base);

                    /*tooltip.add(Component.literal("Core Speed Bonus: +" +
                                    String.format("%.2f", modified - base))
                            .withStyle(ChatFormatting.GREEN)); //Not sure if final, good for debugging

                     */

                    if (Screen.hasShiftDown()) {
                        tooltip.add(Component.translatable("core.arsenalcrafting." + coreDef.id() + ".desc")
                                .withStyle(ChatFormatting.DARK_GRAY)); //Shift for description of ability
                    }

                }
            }
        }
    }

    // Prevent Enchantments on Arsenal Tools
    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }
}