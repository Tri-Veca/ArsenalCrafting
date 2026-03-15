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
import com.arsenalcrafting.mod.items.tool.powers.Momentum;
import com.arsenalcrafting.mod.registry.ModComponents;
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
import net.minecraft.world.item.component.ItemAttributeModifiers;
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
    public ToolComponentType getRodType() {
        return ToolComponentType.SHOVEL_TOOL_ROD;
    }

    @Override
    public ToolComponentType getHeadComponentType() {
        return null;
    }

    @Override
    public ToolComponentType getRodComponentType() {
        return null;
    }

    @Override
    public ItemAttributeModifiers getAttributeModifiers(ItemStack stack) {
        return null;
    }

    @Override
    public float getAssembledDamage(ItemStack stack) {
        return 0;
    }

    @Override
    public float getAssembledAttackSpeed(ItemStack stack) {
        return 0;
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

    //Gui stuff
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

    //Durability Stuff
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
        if (BrokenToolHandler.isBroken(stack)) return false;
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

    //Efficiency Stuff
    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        // Broken tools mine at hand speed
        if (BrokenToolHandler.isBroken(stack)) return 1.0f;

        // Check if we can mine this block at all
        if (!isCorrectToolForDrops(stack, state)) {
            return 1.0f;
        }

        // Get base speed from super
        float speed = super.getDestroySpeed(stack, state);

        // Use UnifiedProcessor which handles WorkHorse ×30 for hard blocks
        return UnifiedProcessor.getDestroySpeed(stack, state, speed);
    }

    //Anything that requires constant updates
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!(entity instanceof Player player)) return;

        if (selected) {
            UnifiedProcessor.processToolTick(stack, player);
        } else {
            // Tool is no longer selected - clean up light
            AugmentHandler handler = AugmentManager.getHandler("light");
            if (handler instanceof AugmentHandlers.Light lightHandler) {
                if (AugmentManager.isActive("light", stack)) {
                    lightHandler.removePlayerLight(player);
                }
            }
        }
    }


    @Override
    public boolean canAttackBlock(BlockState state, net.minecraft.world.level.Level level,
                                  BlockPos pos, net.minecraft.world.entity.player.Player player) {
        if (BrokenToolHandler.isBroken(player.getMainHandItem())) return false;
        return super.canAttackBlock(state, level, pos, player);
    }

    //We now use Unified Processor here!
    //Controls what happens when the player mines a block
    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level level, @NotNull BlockState state,
                             @NotNull BlockPos pos, @NotNull LivingEntity entity) {
        if (BrokenToolHandler.isBroken(stack)) return false;

        // Process block break through UnifiedProcessor (handles cores + augments)
        if (UnifiedProcessor.processBlockBreak(stack, level, state, pos, entity)) {
            EquipmentSlot slot = entity.getMainHandItem() == stack
                    ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
            stack.hurtAndBreak(1, entity, slot);
            return true;
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
        if (BrokenToolHandler.interceptDamage(stack, amount, entity)) return 0;
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

        //Get Generic Tool Tips from ArsenalTool Interface
        appendArsenalTooltip(stack, context, tooltip, flag);
    }
}