package com.arsenalcrafting.mod.items.tool.powers;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public final class WorkHorse {
    private WorkHorse() {}

    private static final float HARDNESS_THRESHOLD = 10.0f;

    public static boolean isHardBlock(BlockState state) {
        // Check if block hardness is >= 10
        float hardness = state.getDestroySpeed(null, null);
        return hardness >= HARDNESS_THRESHOLD;
    }

    public static boolean isCobweb(BlockState state) {
        return state.is(net.minecraft.world.level.block.Blocks.COBWEB);
    }

    private static final float SLOWNESS_PROC_CHANCE = 0.07f;

    public static void trySlownessProc(LivingEntity target, net.minecraft.util.RandomSource random) {
        if (random.nextFloat() >= SLOWNESS_PROC_CHANCE) return;
        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 60, 1)); // 3s, Slowness 2
    }

    // Tilling: borrows vanilla's own HoeItem logic rather than reimplementing
// the dirt/grass -> farmland conversion table by hand.
    public static boolean tryHoeInteraction(Level level, BlockPos pos, BlockState state, Player player) {
        net.minecraft.world.level.block.state.BlockState tilled = null;

        if (state.is(net.minecraft.world.level.block.Blocks.GRASS_BLOCK)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT)
                || state.is(net.minecraft.world.level.block.Blocks.DIRT_PATH)
                || state.is(net.minecraft.world.level.block.Blocks.ROOTED_DIRT)) {
            tilled = net.minecraft.world.level.block.Blocks.FARMLAND.defaultBlockState();
        }

        if (tilled == null) return false;

        // Only till when the block above is air, matching vanilla hoe behavior.
        BlockState above = level.getBlockState(pos.above());
        if (!above.isAir() && !(above.getBlock() instanceof net.minecraft.world.level.block.BushBlock)) return false;

        level.playSound(player, pos, net.minecraft.sounds.SoundEvents.HOE_TILL, net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
        level.setBlock(pos, tilled, 11);
        return true;
    }

    // Shearing: covers pumpkin carving and cobweb -> string, the two most
// common shear-on-block interactions. Extend this list if you want more
// (e.g. beehive honey collection) — same pattern, one more branch.
    // Expand the existing block list — vines and leaves shear identically (drop themselves),
// beehive/bee nest give honeycomb only when full (vanilla LEVEL property == 5).
    public static boolean tryShearInteraction(Level level, BlockPos pos, BlockState state, Player player) {
        if (state.is(net.minecraft.world.level.block.Blocks.PUMPKIN)) {
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.CARVED_PUMPKIN.defaultBlockState(), 11);
            dropItem(level, pos, new ItemStack(net.minecraft.world.item.Items.PUMPKIN_SEEDS, 4));
            return true;
        }
        if (isCobweb(state)) {
            level.destroyBlock(pos, false);
            dropItem(level, pos, new ItemStack(net.minecraft.world.item.Items.COBWEB, 1));
            return true;
        }
        if (state.is(net.minecraft.tags.BlockTags.LEAVES) || state.is(net.minecraft.world.level.block.Blocks.VINE)) {
            level.destroyBlock(pos, false);
            dropItem(level, pos, new ItemStack(state.getBlock()));
            return true;
        }
        if (state.getBlock() instanceof net.minecraft.world.level.block.BeehiveBlock
                && state.getValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL) >= 5) {
            level.setBlock(pos, state.setValue(net.minecraft.world.level.block.BeehiveBlock.HONEY_LEVEL, 0), 11);
            dropItem(level, pos, new ItemStack(net.minecraft.world.item.Items.HONEYCOMB, 3));
            level.playSound(player, pos, net.minecraft.sounds.SoundEvents.BEEHIVE_SHEAR,
                    net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
            return true;
        }
        return false;
    }

    private static void dropItem(Level level, BlockPos pos, ItemStack stack) {
        net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
                level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        level.addFreshEntity(drop);
    }

    // Entity shearing — borrows vanilla's real ShearsItem logic. Safe to borrow here,
// unlike the hoe case, since interactLivingEntity takes the stack explicitly
// rather than re-reading it from the player's hand internally.
    public static boolean tryShearEntity(Player player, LivingEntity target, InteractionHand hand) {
        if (player.level().isClientSide) return false;
        ItemStack dummyShears = new ItemStack(net.minecraft.world.item.Items.SHEARS);
        InteractionResult result = dummyShears.getItem().interactLivingEntity(dummyShears, player, target, hand);
        return result.consumesAction();
    }

}