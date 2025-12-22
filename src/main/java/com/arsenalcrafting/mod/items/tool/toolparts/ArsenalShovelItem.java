package com.arsenalcrafting.mod.items.tool;

import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.core.UnifiedProcessor;
import com.arsenalcrafting.mod.component.ToolAssemblyData;
import com.arsenalcrafting.mod.config.TestModeConfig;
import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.core.augments.AugmentHandler;
import com.arsenalcrafting.mod.core.augments.AugmentHandlers;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
import com.arsenalcrafting.mod.items.augments.EfficiencyAugmentItem;
import com.arsenalcrafting.mod.items.tool.powers.CoreManager;
import com.arsenalcrafting.mod.items.tool.powers.Sturdy;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
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
import java.util.function.Consumer;

import static com.arsenalcrafting.mod.registry.ModComponents.TOOL_ASSEMBLY_DATA;

public class ArsenalShovelItem extends ShovelItem implements ArsenalTool {
    private final ArsenalToolType toolType = ArsenalToolType.SHOVEL;
    private final AlloyMaterial baseMaterial;

    public ArsenalShovelItem(AlloyMaterial material, Properties properties) {
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
    @Override
    public ToolComponentType getHeadType() {
        return ToolComponentType.SHOVEL_HEAD;
    }


    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionResultHolder<ItemStack> arsenalResult = onToolUse(level, player, hand);
        if (arsenalResult.getResult().consumesAction()) {
            return arsenalResult;
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack getDefaultInstance() {
        ItemStack stack = super.getDefaultInstance();
        if (TestModeConfig.isTestModeEnabled()) {
            System.out.println("[TestMode] Installing all shovel augments on new shovel");
            for (AugmentManager.AugmentDefinition def : AugmentManager.getAll()) {
                var handler = AugmentManager.getHandler(def.id());
                if (handler != null && handler.supportsToolType(ArsenalToolType.SHOVEL)) {
                    if (TestModeConfig.isAugmentEnabled(def.id())) {
                        int freeSlot = getFirstFreeSlot(stack);
                        if (freeSlot >= 0) {
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
                AlloyMaterial coreMat = AlloyMaterial.fromName(assemblyData.coreMaterial());
                if (coreMat != null) {
                    CoreManager.CoreDefinition def = CoreManager.getForMaterial(coreMat);
                    if (def != null) {
                        durability = def.handler().modifyMaxDurability(stack, durability);
                    }
                }
                return (int) durability;
            }
        }
        return baseMaterial.getDurability();
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        // FIRST: Must be mineable with shovel
        if (!state.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            return false;
        }

        // SECOND: Use UnifiedProcessor to check cores
        if (UnifiedProcessor.isCorrectToolForDrops(stack, state)) {
            return true;
        }

        // THIRD: Normal tier-based check
        ToolAssemblyData data = stack.get(TOOL_ASSEMBLY_DATA.get());
        AlloyMaterial material = getBaseMaterial();

        if (data != null && !data.isEmpty()) {
            AlloyMaterial headMat = AlloyMaterial.fromName(data.headMaterial());
            if (headMat != null) {
                material = headMat;
            }
        }

        TagKey<Block> incorrectTag = AlloyMaterial.getToolBreakTag(material);
        boolean isIncorrect = state.is(incorrectTag);
        return !isIncorrect;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Check if we can mine this block at all
        if (!isCorrectToolForDrops(stack, state)) {
            return 1.0f;
        }

        // Get base speed from super
        float speed = super.getDestroySpeed(stack, state);

        // Use UnifiedProcessor (shovels don't need WorkHorse ×30 logic)
        return UnifiedProcessor.getDestroySpeed(stack, state, speed);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!(entity instanceof Player player)) return;

        if (selected) {
            UnifiedProcessor.processToolTick(stack, player);
        }
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockState state,
                             @NotNull BlockPos pos, @NotNull LivingEntity entity) {

        if (!level.isClientSide) {
            // Call Momentum for shovel mining too
            com.arsenalcrafting.mod.items.tool.powers.Momentum.onBlockMined(stack, entity);

            // Process block break through UnifiedProcessor (handles cores + augments)
            if (UnifiedProcessor.processBlockBreak(stack, level, state, pos, entity)) {
                EquipmentSlot slot = entity.getMainHandItem() == stack
                        ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                stack.hurtAndBreak(1, entity, slot);
                return true;
            }
        }

        // VANILLA BREAK with durability save check
        boolean shouldDamage = true;
        if (entity != null && level.getRandom() != null) {
            shouldDamage = !UnifiedProcessor.shouldSaveDurability(stack, level.getRandom());
        }

        if (shouldDamage) {
            return super.mineBlock(stack, level, state, pos, entity);
        }

        return true;
    }

    @Override
    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, @Nullable T entity, Consumer<Item> onBroken) {
        // Check for Sturdy core
        if (entity != null) {
            if (UnifiedProcessor.shouldSaveDurability(stack, entity.getRandom())) {
                return 0; // No durability consumed
            }
        }
        return super.damageItem(stack, amount, entity, onBroken);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, @Nullable Item.TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        // Use the ArsenalTool interface tooltip
        appendArsenalTooltip(stack, context, tooltip, flag);

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

                // --- DYNAMIC STATS ---
                float finalDurability = (float) rodMat.getDurability();
                float finalEfficiency = headMat.getEfficiency();

                CoreManager.CoreDefinition coreDef = CoreManager.getForMaterial(coreMat);
                if (coreDef != null) {
                    finalDurability = coreDef.handler().modifyMaxDurability(stack, finalDurability);
                    finalEfficiency = coreDef.handler().modifyMiningSpeed(stack, finalEfficiency);
                }

                if (isAugmentEnabled(stack, "efficiency")) {
                    finalEfficiency += EfficiencyAugmentItem.getSpeedBonus(stack, finalEfficiency);
                }

                tooltip.add(Component.translatable("item.arsenalcrafting.repairability", headMat.getName())
                        .withStyle(headMat.getColor()));

                tooltip.add(Component.translatable("item.arsenalcrafting.durability")
                        .append(": " + (int) finalDurability)
                        .withStyle(rodMat.getColor()));

                tooltip.add(Component.translatable("item.arsenalcrafting.efficiency")
                        .append(": " + String.format("%.1f", finalEfficiency))
                        .withStyle(headMat.getColor()));

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

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return false;
    }

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
            return 0;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.EMPTY;
        }
    }
}