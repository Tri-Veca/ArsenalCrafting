package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.AugmentHandlers;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import com.arsenalcrafting.mod.registry.ModComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.crafting.SingleRecipeInput;

import java.util.List;
import java.util.Optional;

/**
 * The physical item representing the Auto Smelt augment.
 * This class now extends Item and contains the static utility methods for
 * handling the smelting logic when a block is mined.
 */
public class AutoSmeltAugmentItem extends Item implements Augment, InstallableAugmentItem {

    public AutoSmeltAugmentItem(Properties properties) {
        super(properties);
    }

    /**
     * Handles the drops when a block is mined with an Auto Smelt enabled tool.
     * This method looks up a smelting recipe and drops the result instead of the raw block item.
     * Converted to static so the pickaxe can call it directly as a utility method.
     * @return true if the drops were successfully handled (smelted and block removed), false otherwise.
     */

    public static boolean onBlockMined(ItemStack tool, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        // Check if block is already air
        if (state.isAir()) {
            System.out.println("[AutoSmelt] Block is already air, skipping");
            return false;
        }

        List<ItemStack> rawDrops = Block.getDrops(
                state,
                serverLevel,
                pos,
                level.getBlockEntity(pos),
                entity,
                tool
        );

        if (rawDrops.isEmpty()) {
            return false;
        }

        boolean successfullySmeltedAnyItem = false;
        boolean droppedAnyItem = false;

        for (ItemStack rawDrop : rawDrops) {
            Optional<ItemStack> smeltedResult = findSmeltingResult(serverLevel.getRecipeManager(), rawDrop, serverLevel);

            if (smeltedResult.isPresent()) {
                ItemStack resultStack = smeltedResult.get();

                if (!ItemStack.isSameItem(rawDrop, resultStack)) {
                    resultStack.setCount(resultStack.getCount() * rawDrop.getCount());
                    System.out.println("[AutoSmelt] Smelting ore: " + rawDrop.getItem() + " -> " + resultStack.getItem() + " x" + resultStack.getCount());
                } else {
                    resultStack.setCount(rawDrop.getCount());
                    System.out.println("[AutoSmelt] No transformation for: " + rawDrop.getItem() + " (dropping original count)");
                }

                Block.popResource(serverLevel, pos, resultStack);
                successfullySmeltedAnyItem = true;
            } else {
                Block.popResource(serverLevel, pos, rawDrop);
                droppedAnyItem = true;
            }
        }


        if (successfullySmeltedAnyItem || droppedAnyItem) {
            System.out.println("[AutoSmelt] Processing complete - DESTROYING block at " + pos);

            // ALWAYS destroy the block after processing
            boolean destroyed = level.destroyBlock(pos, false);
            System.out.println("[AutoSmelt] destroyBlock returned: " + destroyed);

            // If destroyBlock failed, force it to air
            if (!destroyed) {
                System.out.println("[AutoSmelt] Forcing block to air");
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }

            return true;
        }

        return false;
    }

    /**
     * Tries to find a valid Smelting or Blasting recipe result for an input ItemStack.
     * Converted to static to match the calling function.
     * @param recipeManager The RecipeManager instance.
     * @param input The raw item drop (e.g., Raw Iron).
     * @param level The Level context needed for recipe lookup.
     * @return An Optional containing the smelted result ItemStack.
     */
  public static Optional<ItemStack> findSmeltingResult(RecipeManager recipeManager, ItemStack input, Level level) {

        // 1. Create the canonical single-slot recipe input wrapper.
        // SingleRecipeInput replaced the old RecipeInput class from older versions.
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);

        // 2. Prioritize Blasting recipe (RecipeType<BlastingRecipe>)
        Optional<RecipeHolder<BlastingRecipe>> blastingRecipe = recipeManager.getRecipeFor(
                RecipeType.BLASTING,
                recipeInput,
                level
        );

        if (blastingRecipe.isPresent()) {
            // Use get().value().getResultItem(null).copy() to get a mutable copy of the result
            return Optional.of(blastingRecipe.get().value().getResultItem(null).copy());
        }

        // 3. Fallback to regular Smelting recipe (RecipeType<SmeltingRecipe>)
        Optional<RecipeHolder<SmeltingRecipe>> smeltingRecipe = recipeManager.getRecipeFor(
                RecipeType.SMELTING,
                recipeInput,
                level
        );

        if (smeltingRecipe.isPresent()) {
            // Use get().value().getResultItem(null).copy() to get a mutable copy of the result
            return Optional.of(smeltingRecipe.get().value().getResultItem(null).copy());
        }

        return Optional.empty();
    }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("auto_smelt"), true);
        System.out.println("[Toolkit] Auto-Smelt augment installed!");
    }
}