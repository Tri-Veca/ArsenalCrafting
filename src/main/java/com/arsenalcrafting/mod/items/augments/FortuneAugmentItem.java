package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.AugmentManager;
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


    public static boolean isOre(BlockState state){
        return state.is(BlockTags.COAL_ORES) ||
                state.is(BlockTags.COPPER_ORES) ||
                state.is(BlockTags.DIAMOND_ORES) ||
                state.is(BlockTags.GOLD_ORES) ||
                state.is(BlockTags.EMERALD_ORES) ||
                state.is(BlockTags.IRON_ORES) ||
                state.is(BlockTags.LAPIS_ORES) ||
                state.is(BlockTags.REDSTONE_ORES);


    }

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

        // Only skip if Auto-Smelt AND Ore Miner are BOTH active
        boolean autoSmeltActive = AugmentManager.isActive("auto_smelt", tool);
        boolean oreMinerActive = AugmentManager.isActive("ore_miner", tool);

        if (autoSmeltActive && oreMinerActive) {
            // All three: Auto-Smelt + Fortune + Ore Miner
            // Ore Miner will handle fortune internally
            System.out.println("[Fortune] Skipping - All three augments active (handled by Ore Miner)");
            return false;
        }

        if (autoSmeltActive && !oreMinerActive) {
            // Just Auto-Smelt + Fortune (no Ore Miner)
            // Auto-Smelt already smelted the ore, can't fortune smelted items
            System.out.println("[Fortune] Skipping - Auto-Smelt already processed this ore");
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

        int tier = tool.getOrDefault(DynamicComponents.getTierComponent("fortune"), 1);
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

        level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        return true;
    }
}