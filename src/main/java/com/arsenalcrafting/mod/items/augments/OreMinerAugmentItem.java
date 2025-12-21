package com.arsenalcrafting.mod.items.augments;

import com.arsenalcrafting.mod.component.DynamicComponents;
import com.arsenalcrafting.mod.core.augments.Augment;
import com.arsenalcrafting.mod.core.augments.InstallableAugmentItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class OreMinerAugmentItem extends Item implements Augment, InstallableAugmentItem {
    private static final int MAX_VEIN_BLOCKS = 64;

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

    /**
     * Normal vein mining with vanilla drops.
     */
    public static boolean veinMineNormal(ServerLevel level, BlockPos origin,
                                         BlockState targetState, Player player,
                                         ItemStack tool) {
        // Get the exact ore block we started with
        Block originalOreBlock = targetState.getBlock();

        // Safety check
        if (!isOre(targetState)) {
            return false;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        // Only add neighbors that are the EXACT SAME ore block
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);

            if (neighborState.getBlock() == originalOreBlock) {
                queue.add(neighbor);
                visited.add(neighbor);
            }
        }
        visited.add(origin);

        int broken = 0;

        while (!queue.isEmpty() && broken < MAX_VEIN_BLOCKS) {
            BlockPos current = queue.remove();
            BlockState currentState = level.getBlockState(current);

            // Only mine if it's the exact same ore block
            if (currentState.getBlock() != originalOreBlock) continue;

            // Vanilla drops
            boolean destroyed = level.destroyBlock(current, true, player);

            if (destroyed) {
                broken++;
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }

            if (broken < MAX_VEIN_BLOCKS) {
                for (Direction dir : Direction.values()) {
                    BlockPos next = current.relative(dir);
                    if (!visited.contains(next)) {
                        BlockState nextState = level.getBlockState(next);

                        // Only expand to blocks that are the EXACT SAME ore
                        if (nextState.getBlock() == originalOreBlock) {
                            queue.add(next);
                            visited.add(next);
                        }
                    }
                }
            }
        }

        System.out.println("[OreMiner] Mined " + broken + " additional ore blocks with normal vein mining");
        return broken > 0;
    }

    /**
     * Vein mining with smelting.
     * IMPORTANT: Origin block is already processed by Auto-Smelt, so skip it!
     */
    public static boolean veinMineWithSmelting(ServerLevel level, BlockPos origin,
                                               BlockState targetState, Player player,
                                               ItemStack tool) {
        // Get the exact ore block we started with
        Block originalOreBlock = targetState.getBlock();

        // Safety check
        if (!isOre(targetState)) {
            return false;
        }

        Queue<BlockPos> queue = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();

        // Only add neighbors that are the EXACT SAME ore block
        for (Direction dir : Direction.values()) {
            BlockPos neighbor = origin.relative(dir);
            BlockState neighborState = level.getBlockState(neighbor);

            if (neighborState.getBlock() == originalOreBlock) {
                queue.add(neighbor);
                visited.add(neighbor);
            }
        }
        visited.add(origin); // Mark origin as visited but NOT processed

        int broken = 0;

        while (!queue.isEmpty() && broken < MAX_VEIN_BLOCKS) {
            BlockPos current = queue.remove();
            BlockState currentState = level.getBlockState(current);

            // Skip if not same ore type
            if (currentState.getBlock() != originalOreBlock) continue;

            // Get vanilla drops
            List<ItemStack> rawDrops = Block.getDrops(
                    currentState, level, current,
                    level.getBlockEntity(current), player, tool);

            // Smelt each drop using Auto-Smelt's helper
            for (ItemStack rawDrop : rawDrops) {
                Optional<ItemStack> smeltedResult =
                        AutoSmeltAugmentItem.findSmeltingResult(
                                level.getRecipeManager(), rawDrop, level);

                if (smeltedResult.isPresent()) {
                    ItemStack result = smeltedResult.get().copy();
                    // Check if items are different (ore -> ingot)
                    if (!ItemStack.isSameItem(rawDrop, result)) {
                        result.setCount(result.getCount() * rawDrop.getCount());
                    } else {
                        result.setCount(rawDrop.getCount());
                    }
                    Block.popResource(level, current, result);
                } else {
                    Block.popResource(level, current, rawDrop);
                }
            }

            // Remove block
            boolean destroyed = level.destroyBlock(current, false);
            if (destroyed) {
                broken++;
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }

            if (broken < MAX_VEIN_BLOCKS) {
                for (Direction dir : Direction.values()) {
                    BlockPos next = current.relative(dir);
                    if (!visited.contains(next)) {
                        BlockState nextState = level.getBlockState(next);

                        // Only expand to blocks that are the EXACT SAME ore
                        if (nextState.getBlock() == originalOreBlock) {
                            queue.add(next);
                            visited.add(next);
                        }
                    }
                }
            }
        }

        System.out.println("[OreMiner+AutoSmelt] Mined " + broken + " additional ore blocks");
        return broken > 0;
    }

    public static List<BlockPos> findConnectedOres(Level level, BlockPos start, BlockState startState) {
        List<BlockPos> vein = new ArrayList<>();
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new LinkedList<>();

        if (!isOre(startState)) {
            return vein;
        }

        // Get the exact ore block we started with
        Block originalOreBlock = startState.getBlock();

        queue.add(start);
        visited.add(start);

        int maxBlocks = 64;

        while (!queue.isEmpty() && vein.size() < maxBlocks) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);

            // Only add to vein if it's the EXACT SAME ore block
            if (currentState.getBlock() == originalOreBlock) {
                vein.add(current);
            } else {
                continue; // Skip this block and don't expand from it
            }

            for (BlockPos offset : new BlockPos[]{
                    current.north(), current.south(), current.east(),
                    current.west(), current.above(), current.below()
            }) {
                if (!visited.contains(offset) && vein.size() < maxBlocks) {
                    BlockState neighborState = level.getBlockState(offset);
                    if (neighborState.getBlock() == originalOreBlock) {
                        queue.add(offset);
                        visited.add(offset);
                    }
                }
            }
        }

        return vein;
    }

    public static boolean veinMineWithFortune(ServerLevel level, BlockPos origin,
                                              BlockState originState, Player player,
                                              ItemStack tool) {
        List<BlockPos> vein = findConnectedOres(level, origin, originState);

        if (vein.isEmpty()) {
            return false;
        }

        System.out.println("[OreMiner] Mining " + vein.size() + " ores with Fortune");

        // IMPORTANT: Remove origin from vein if it's included
        vein.remove(origin);

        Map<BlockPos, BlockState> originalStates = new HashMap<>();
        Map<BlockPos, BlockEntity> originalEntities = new HashMap<>();

        // Get the exact ore block we started with
        Block originalOreBlock = originState.getBlock();

        for (BlockPos pos : vein) {
            BlockState stateAtPos = level.getBlockState(pos);
            // Only remove if it's the exact same ore
            if (stateAtPos.getBlock() == originalOreBlock) {
                originalStates.put(pos, stateAtPos);
                originalEntities.put(pos, level.getBlockEntity(pos));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        boolean anyDropped = false;
        for (BlockPos pos : vein) {
            BlockState originalState = originalStates.get(pos);
            BlockEntity originalEntity = originalEntities.get(pos);

            if (originalState == null || originalState.isAir()) continue;

            if (FortuneAugmentItem.onBlockMined(tool, level, originalState, pos, player)) {
                anyDropped = true;
                tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
            }
        }

        return anyDropped;
    }

    public static boolean veinMineWithFortuneAndSmelting(ServerLevel level, BlockPos origin,
                                                         BlockState originState, Player player,
                                                         ItemStack tool) {
        List<BlockPos> vein = findConnectedOres(level, origin, originState);

        if (vein.isEmpty()) {
            return false;
        }

        System.out.println("[OreMiner] Mining " + vein.size() + " ores with Fortune + Auto-Smelt");

        // CRITICAL: Remove origin from vein - Auto-Smelt already processed it
        vein.remove(origin);

        Map<BlockPos, BlockState> originalStates = new HashMap<>();
        Map<BlockPos, BlockEntity> originalEntities = new HashMap<>();

        // Get the exact ore block we started with
        Block originalOreBlock = originState.getBlock();

        for (BlockPos pos : vein) {
            BlockState stateAtPos = level.getBlockState(pos);
            // Only remove if it's the exact same ore
            if (stateAtPos.getBlock() == originalOreBlock) {
                originalStates.put(pos, stateAtPos);
                originalEntities.put(pos, level.getBlockEntity(pos));
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
        }

        boolean anyDropped = false;
        int blocksMined = 0;

        for (BlockPos pos : vein) {
            BlockState originalState = originalStates.get(pos);
            BlockEntity originalEntity = originalEntities.get(pos);

            if (originalState == null || originalState.isAir()) {
                continue;
            }

            int fortuneLevel = getFortuneLevel(tool);
            List<ItemStack> drops = Block.getDrops(
                    originalState,
                    level,
                    pos,
                    originalEntity,
                    player,
                    tool
            );

            List<ItemStack> fortunedDrops = applyFortuneToDrops(drops, fortuneLevel, level.getRandom());

            for (ItemStack drop : fortunedDrops) {
                if (drop.isEmpty()) continue;

                Optional<ItemStack> smeltedResult = AutoSmeltAugmentItem.findSmeltingResult(
                        level.getRecipeManager(), drop, level);

                if (smeltedResult.isPresent()) {
                    ItemStack result = smeltedResult.get();
                    result.setCount(result.getCount() * drop.getCount());
                    Block.popResource(level, pos, result);
                    System.out.println("[OreMiner] Fortune+Smelt: " + drop.getItem() +
                            " -> " + result.getItem() + " x" + result.getCount());
                } else {
                    Block.popResource(level, pos, drop);
                }

                anyDropped = true;
            }

            blocksMined++;
            tool.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
        }

        System.out.println("[OreMiner] Mined " + blocksMined + " blocks with Fortune+Smelt vein mining");
        return anyDropped;
    }

    private static int getFortuneLevel(ItemStack tool) {
        if (tool.has(DynamicComponents.getEnabledComponent("fortune"))) {
            int tier = tool.getOrDefault(DynamicComponents.getTierComponent("fortune"), 1);
            return switch (tier) {
                case 1 -> 2;
                case 2 -> 3;
                case 3 -> 4;
                default -> 0;
            };
        }
        return 0;
    }

    private static List<ItemStack> applyFortuneToDrops(List<ItemStack> drops, int fortuneLevel, RandomSource random) {
        if (fortuneLevel <= 0) return drops;

        List<ItemStack> result = new ArrayList<>();
        for (ItemStack drop : drops) {
            if (drop.isEmpty()) continue;

            int baseCount = drop.getCount();
            int k = random.nextInt(fortuneLevel + 2) - 1;
            if (k < 0) k = 0;
            int totalCount = baseCount * (k + 1);

            ItemStack fortuned = drop.copy();
            fortuned.setCount(totalCount);
            result.add(fortuned);
        }
        return result;
    }

    @Override
    public void installOnTool(ItemStack tool) {
        tool.set(DynamicComponents.getEnabledComponent("ore_miner"), true);
        System.out.println("[Toolkit] Ore-Miner augment installed!");
    }
}