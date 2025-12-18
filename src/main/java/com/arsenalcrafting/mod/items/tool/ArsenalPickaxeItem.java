package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentHandler;
import com.arsenalcrafting.mod.core.augments.AugmentHandlers;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.core.augments.AugmentProcessor;
import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.items.tool.powers.Greedy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
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
                float durability = (float) rodMat.getDurability();

                // DYNAMIC CHECK: Get whatever core is currently installed
                AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());
                if (coreMat != null) {
                    CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                    if (def != null) {
                        // Ask the handler to modify the durability
                        durability = def.handler().modifyMaxDurability(stack, durability);


                         //System.out.println("[DEBUG] Core " + def.id() + " modified durability to: " + durability);
                    }
                }
                return (int) durability;
            }
        }
        return baseMaterial.getDurability();
    }



    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // Get the actual head material from the tool assembly
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        AlloyMaterial material = getBaseMaterial(); // Default fallback

        if (data != null && !data.isEmpty()) {
            // HEAD material determines mining capability
            AlloyMaterial headMat = AlloyMaterial.fromName(data.headMaterial());
            if (headMat != null) {
                material = headMat;
            }
        }

        System.out.println("[DEBUG] isCorrectToolForDrops: Using " + material.getName() +
                " (tier " + material.getTier() + ") for block: " +
                state.getBlock().getName().getString());

        // First check if block is mineable with pickaxe
        if (!state.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            System.out.println("[DEBUG] Block is not mineable with pickaxe");
            return false;
        }

        // Then check if this material tier can mine it
        TagKey<Block> incorrectTag = AlloyMaterial.getToolBreakTag(material);
        boolean isIncorrect = state.is(incorrectTag);

        System.out.println("[DEBUG] Is in incorrect tag? " + isIncorrect + " (tag: " + incorrectTag.location() + ")");

        return !isIncorrect;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // First check if we can mine this block at all
        if (!isCorrectToolForDrops(stack, state)) {
            System.out.println("[DEBUG] getDestroySpeed: Not correct tool, returning 1.0");
            return 1.0f;
        }

        // 1. Start with vanilla Pickaxe speed logic
        float speed = super.getDestroySpeed(stack, state);
        System.out.println("[DEBUG] getDestroySpeed: Vanilla speed = " + speed);

        // 2. Apply head material efficiency
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (data != null && !data.isEmpty()) {
            AlloyMaterial headMat = AlloyMaterial.fromName(data.headMaterial());
            if (headMat != null) {
                speed = headMat.getEfficiency();
                System.out.println("[DEBUG] getDestroySpeed: Head material efficiency = " + speed);
            }

            // 3. Apply core modifier (SpeedBoost, etc.)
            AlloyMaterial coreMat = AlloyMaterial.fromName(data.coreMaterial());
            if (coreMat != null) {
                CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                if (def != null) {
                    speed = def.handler().modifyMiningSpeed(stack, speed);
                    System.out.println("[DEBUG] getDestroySpeed: After core modifier = " + speed);
                }
            }
        }

        return speed;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!(entity instanceof Player player)) return;

        if (selected) {

            ArsenalTool.tickHeldToolAugments(player, stack);
            AugmentProcessor.processToolUpdate(stack, player, ArsenalToolType.PICKAXE);
        } else {
            // Tool not held → cleanup
            AugmentHandler handler = AugmentManager.getHandler("light");
            if (handler instanceof AugmentHandlers.Light light) {
                light.onToolUnequip(stack, player);
            }
            // Only process if the tool is currently held in the main or off hand

        }

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
            return AlloyMaterial.getToolBreakTag(material);
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

        // Logic should only run on the server to manage data components and effects
        if (!level.isClientSide) {

            // 1. DYNAMIC CORE LOGIC (Process Tin Momentum / Copper Efficiency etc.)
            ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());
            if (assemblyData != null) {
                AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());
                CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat);

                // This triggers the core's specific behavior (e.g., Momentum.onBlockMined for Tin)
                if (coreDef != null) {
                    coreDef.handler().onBlockDestroyed(stack, level, state, pos, entity);
                }
            }

            // 2. AUGMENT PROCESSING
            // Check if any augments are installed and enabled
            ToolAugmentSlots slots = getAugmentSlots(stack);
            boolean hasEnabledAugments = false;

            for (int i = 0; i < 4; i++) {
                String augmentId = slots.getAugmentInSlot(i);
                if (augmentId != null && isAugmentEnabled(stack, augmentId)) {
                    hasEnabledAugments = true;
                    break;
                }
            }

            // If enabled augments exist, try to let the AugmentProcessor handle the break
            if (hasEnabledAugments) {
                AugmentProcessor.ProcessResult result = AugmentProcessor.processBlockBreak(
                        stack,
                        ArsenalToolType.PICKAXE,
                        level,
                        state,
                        pos,
                        entity
                );

                // If an augment handled the drops (like Smelting or Voiding), skip vanilla logic
                if (result.handled) {
                    EquipmentSlot slot = entity.getMainHandItem() == stack
                            ? EquipmentSlot.MAINHAND
                            : EquipmentSlot.OFFHAND;

                    // Manually apply durability damage since we are bypassing super.mineBlock()
                    stack.hurtAndBreak(1, entity, slot);

                    // Handle any special augment combo effects
                    if (result.specialCombo != null) {
                        System.out.println("[ArsenalPickaxe] Special combo: " + result.specialCombo);
                    }

                    return true; // Successfully handled by augments
                }
            }
        }

        // 3. VANILLA FALLBACK
        // No enabled augment handled the block, or we are on the client: use default behavior
        return super.mineBlock(stack, level, state, pos, entity);
    }



    // Tooltip to show base stats and augment slots
    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        // 1. Restore the Augment List (Magnetism, etc.)
        appendArsenalTooltip(stack, context, tooltip, flag);

        ToolAssemblyData assemblyData = stack.get(TOOL_ASSEMBLY_DATA.get());
        if (assemblyData != null && !assemblyData.isEmpty()) {
            tooltip.add(Component.literal(""));
            tooltip.add(Component.translatable("item.arsenalcrafting.tool_parts").withStyle(ChatFormatting.DARK_AQUA));

            // Material Names
            tooltip.add(Component.translatable("item.arsenalcrafting.head_material", assemblyData.headMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.rod_material", assemblyData.rodMaterial()).withStyle(ChatFormatting.WHITE));
            tooltip.add(Component.translatable("item.arsenalcrafting.core_material", assemblyData.coreMaterial()).withStyle(ChatFormatting.WHITE));

            AlloyMaterial headMat = AlloyMaterial.fromName(assemblyData.headMaterial());
            AlloyMaterial rodMat = AlloyMaterial.fromName(assemblyData.rodMaterial());
            AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());

            if (headMat != null && rodMat != null && coreMat != null) {
                tooltip.add(Component.literal(""));


                // --- DYNAMIC STATS ---
                float finalDurability = (float) rodMat.getDurability();
                float finalEfficiency = headMat.getEfficiency();

                CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat);
                if (coreDef != null) {
                    // This ensures Gold (Greedy) modifies the numbers before they are displayed
                    finalDurability = coreDef.handler().modifyMaxDurability(stack, finalDurability);
                    finalEfficiency = coreDef.handler().modifyMiningSpeed(stack, finalEfficiency);
                }


                tooltip.add(Component.translatable("item.arsenalcrafting.repairability", headMat.getName())
                        .withStyle(headMat.getColor()));

                // --- DISPLAY DURABILITY (Flat number, no %) ---
                tooltip.add(Component.translatable("item.arsenalcrafting.durability")
                        .append(": " + (int) finalDurability)
                        .withStyle(rodMat.getColor()));

                // --- DISPLAY EFFICIENCY (Flat number, no %) ---
                tooltip.add(Component.translatable("item.arsenalcrafting.efficiency")
                        .append(": " + String.format("%.1f", finalEfficiency))
                        .withStyle(headMat.getColor()));

                // --- CORE POWER INFO ---
                if (coreDef != null) {
                    tooltip.add(Component.literal("• ")
                            .append(Component.translatable("core.arsenalcrafting." + coreDef.id())
                                    .withStyle(coreMat.getColor())));

                    if (Screen.hasShiftDown()) {
                        tooltip.add(Component.translatable("core.arsenalcrafting." + coreDef.id() + ".desc")
                                .withStyle(ChatFormatting.DARK_GRAY));
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