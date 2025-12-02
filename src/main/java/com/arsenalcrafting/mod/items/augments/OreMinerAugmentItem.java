package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.core.augments.Augment;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

public class OreMinerAugmentItem extends Item implements Augment {

    public OreMinerAugmentItem(Properties properties) {
        super(properties);
    }

    public static boolean isOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES);
    }


    public static boolean onBlockMined(ItemStack tool, Level level, BlockState state, BlockPos pos, LivingEntity entity){
        // Ensure we are only running this server-side
        if (!(level instanceof ServerLevel serverLevel)) {
            return false;
        }

        // Only ores
        if (!isOre(state)) {
            return false;
        }

        if (!(entity instanceof Player player)) {
            return false;
        }

        // vein-mine here
        veinMineOre(serverLevel, pos, state, player, tool);

        return true;

    }

    private static final int MAX_VEIN_BLOCKS = 64; // safety limit

    private static void veinMineOre(ServerLevel level,
                                    BlockPos origin,
                                    BlockState targetState,
                                    Player player,
                                    ItemStack tool) {

        // We start from the original pos, but remember: in Item#mineBlock the center
        // is already broken, so we never try to destroy origin, only neighbors.
        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        queue.add(origin);
        visited.add(origin);

        int broken = 0;

        while (!queue.isEmpty() && broken < MAX_VEIN_BLOCKS) {
            BlockPos current = queue.remove();

            for (Direction dir : Direction.values()) {
                BlockPos next = current.relative(dir);

                if (visited.contains(next)) continue;
                visited.add(next);

                BlockState other = level.getBlockState(next);

                // same ore block?
                if (!other.is(targetState.getBlock())) continue;

                // break it and drop loot with correct tool + player
                boolean destroyed = level.destroyBlock(next, true, player);
                if (destroyed) {
                    broken++;

                    // damage tool once per block (optional)
                    tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }

                // keep searching from this block too
                if (broken < MAX_VEIN_BLOCKS) {
                    queue.add(next);
                }
            }
        }
    }
}
