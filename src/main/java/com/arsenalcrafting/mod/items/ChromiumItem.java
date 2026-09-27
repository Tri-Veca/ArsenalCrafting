package com.arsenalcrafting.mod.items;

import com.arsenalcrafting.mod.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class ChromiumItem extends Item {

    public ChromiumItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState state = level.getBlockState(pos);

        ItemDebug.use("Chromium", "useOn at " + pos + " block=" + state.getBlock() + " side=" + (level.isClientSide ? "CLIENT" : "SERVER"));
        // Note: useOn fires on both sides; debug above shows which side triggered

        boolean isIronOre = state.is(BlockTags.IRON_ORES);
        if (!isIronOre) {
            ItemDebug.use("Chromium", "Not iron ore, skipping");
            return InteractionResult.PASS;
        }

        boolean isDay = level.isDay();
        if (!isDay) {
            ItemDebug.use("Chromium", "Nighttime, skipping");
            return InteractionResult.PASS;
        }

        if (!level.isClientSide) {
            BlockState rutileState = ModBlocks.RUTILE_BUDDING.get().defaultBlockState();
            boolean success = level.setBlock(pos, rutileState, 3);
            ItemDebug.use("Chromium", "setBlock=" + success + " result=" + level.getBlockState(pos).getBlock());

            if (context.getPlayer() != null && !context.getPlayer().isCreative()) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
