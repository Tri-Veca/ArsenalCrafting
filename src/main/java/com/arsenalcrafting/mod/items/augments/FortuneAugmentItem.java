package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class FortuneAugmentItem extends Item implements Augment, InstallableAugmentItem {
    private final int tier;

    public FortuneAugmentItem(int tier, Properties properties) {
        super(properties);
        this.tier = tier;
    }

    public int getTier() {
        return tier;
    }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("fortune"), true);
        tool.set(DynamicComponents.getTierComponent("fortune"), tier);
        System.out.println("[Toolkit] Fortune tier " + tier + " installed!");
    }
    //Only Allow Ores to be vein mined
    public static boolean isOre(BlockState state){
        if (!(state.is(BlockTags.COAL_ORES) ||
                state.is(BlockTags.COPPER_ORES) ||
                state.is(BlockTags.DIAMOND_ORES) ||
                state.is(BlockTags.GOLD_ORES) ||
                state.is(BlockTags.EMERALD_ORES) ||
                state.is(BlockTags.IRON_ORES) ||
                state.is(BlockTags.LAPIS_ORES) ||
                state.is(BlockTags.REDSTONE_ORES))) {
            return false;
        }
        return true;
    }

    /**
     * Called from the Fortune augment handler when a block is mined.
     * Returns true if this augment handled drops (so vanilla should be skipped).
     */
    public static boolean onBlockMined(ItemStack tool, Level level,
                                       BlockState state, BlockPos pos,
                                       LivingEntity entity) {

        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        if (state.isAir()) {
            return false;
        }

        if (!isOre(state)){
            return false;
        }

        List<ItemStack> drops = Block.getDrops(
                state,
                serverLevel,
                pos,
                level.getBlockEntity(pos),
                entity,
                tool
        );

        if (drops.isEmpty()) {
            return false;
        }

        // Read tier from the tool, default 1
        int tier = tool.getOrDefault(DynamicComponents.getTierComponent("fortune"), 1);
        // Map tiers to "virtual Fortune level": 1 -> 2, 2 -> 3, 3 -> 4
        int fortuneLevel = switch (tier) {
            case 1 -> 2;
            case 2 -> 3;
            case 3 -> 4;
            default -> 2;
        };

        RandomSource random = serverLevel.getRandom();

        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            int baseCount = drop.getCount();

            // Vanilla-style fortune math:
            // k = random(0..fortuneLevel+1) - 1
            // if k < 0 -> 0
            // multiplier = k + 1
            int k = random.nextInt(fortuneLevel + 2) - 1;
            if (k < 0) {
                k = 0;
            }

            int totalCount = baseCount * (k + 1);
            drop.setCount(totalCount);

            if (!drop.isEmpty()) {
                Block.popResource(level, pos, drop);
            }
        }


        // Remove the original block (we handled drops ourselves)
        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

        return true;
    }
}
