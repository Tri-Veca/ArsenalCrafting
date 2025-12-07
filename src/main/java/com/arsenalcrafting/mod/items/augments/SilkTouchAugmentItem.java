package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class SilkTouchAugmentItem extends Item implements Augment, InstallableAugmentItem {
    public SilkTouchAugmentItem(Properties properties) {
        super(properties);
    }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("silk_touch"), true);
        System.out.println("[Toolkit] Silk-Touch augment installed!");
    }

    public static boolean onBlockMined(ItemStack tool, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        // Check if block is already air
        if (state.isAir()) {
            System.out.println("[SilkTouch] Block is already air, skipping");
            return false;
        }

        // Don't silk touch certain blocks
        if (!canSilkTouch(state)) {
            System.out.println("[SilkTouch] Cannot silk touch this block: " + state.getBlock());
            return false;
        }

        // Get the block item to drop
        ItemStack blockItem = state.getBlock().asItem().getDefaultInstance();

        if (blockItem.isEmpty()) {
            System.out.println("[SilkTouch] Block has no item form: " + state.getBlock());
            return false;
        }

        System.out.println("[SilkTouch] Dropping block item: " + blockItem.getItem());

        // Drop the block item itself
        Block.popResource(serverLevel, pos, blockItem);

        // Remove the original block (we handled drops ourselves)
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        return true;
    }

    private static boolean canSilkTouch(BlockState state) {
        // Don't silk touch blocks that drop nothing or have special handling
        Block block = state.getBlock();

        // Examples of blocks that shouldn't be silk touched
        if (block == Blocks.BEDROCK ||
                block == Blocks.SPAWNER ||
                block == Blocks.END_PORTAL_FRAME ||
                block == Blocks.BARRIER ||
                block == Blocks.COMMAND_BLOCK ||
                block == Blocks.STRUCTURE_BLOCK ||
                block == Blocks.JIGSAW ||
                block == Blocks.LIGHT) {
            return false;
        }

        // Check if block has an item form
        if (block.asItem() == null || block.asItem() == ItemStack.EMPTY.getItem()) {
            return false;
        }

        return true;
    }
}