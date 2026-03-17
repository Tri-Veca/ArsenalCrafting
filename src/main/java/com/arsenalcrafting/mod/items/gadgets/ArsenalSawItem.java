package com.arsenalcrafting.mod.items.gadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Saw — power-consuming 3×3 area mining tool.
 *
 * - Stone tool mining speed (no material bonus)
 * - Mines a 3×3 grid perpendicular to the face the player is looking at
 * - Consumes energy per block broken — does NOT use durability
 * - Requires energy to function — dead saw mines at hand speed
 *
 * Power is stored in the item itself via IEnergyStorage capability.
 * Register the item capability in ModItems.registerCapabilities().
 */
public class ArsenalSawItem extends AxeItem {

    // ── CONSTANTS ─────────────────────────────────────────────────────────────

    public static final int MAX_ENERGY     = 10_000;   // FE capacity
    public static final int ENERGY_PER_BLOCK = 50;     // FE per block broken
    public static final int TRANSFER_RATE  = 1_000;    // FE/t max charge rate

    private static final float STONE_SPEED = 1f;     // matches stone pickaxe

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────

    public ArsenalSawItem(Properties properties) {
        super(SawTier.INSTANCE, properties.stacksTo(1));
    }

    // ── ENERGY ────────────────────────────────────────────────────────────────

    public static int getEnergy(ItemStack stack) {
        var tag = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (tag == null) return 0;
        var nbt = tag.copyTag();
        return nbt.contains("ArsenalEnergy") ? nbt.getInt("ArsenalEnergy") : 0;
    }

    public static void setEnergy(ItemStack stack, int energy) {
        var existing = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY);
        var nbt = existing.copyTag();
        nbt.putInt("ArsenalEnergy", Math.max(0, Math.min(MAX_ENERGY, energy)));
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(nbt));
    }

    public static boolean hasEnoughEnergy(ItemStack stack) {
        return getEnergy(stack) >= ENERGY_PER_BLOCK;
    }

    public static int consumeEnergy(ItemStack stack, int amount) {
        int current = getEnergy(stack);
        int consumed = Math.min(current, amount);
        setEnergy(stack, current - consumed);
        return consumed;
    }

    // ── MINING ────────────────────────────────────────────────────────────────

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) return 1.0f; // dead — hand speed
        if (!state.is(BlockTags.MINEABLE_WITH_AXE)) return 1.0f;
        return STONE_SPEED;
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return super.mineBlock(stack, level, state, pos, entity);
        }

        if (!hasEnoughEnergy(stack)) return false;

        consumeEnergy(stack, ENERGY_PER_BLOCK);

        // If breaking a log — fell the whole tree
        if (state.is(BlockTags.LOGS)) {
            fellTree(stack, level, pos, player);
            return true;
        }

        // Otherwise mine 3×3 area
        List<BlockPos> area = get3x3Area(pos, player);
        for (BlockPos extra : area) {
            if (extra.equals(pos)) continue;
            if (!level.isLoaded(extra)) continue;

            BlockState extraState = level.getBlockState(extra);
            if (extraState.isAir()) continue;
            if (!extraState.is(BlockTags.MINEABLE_WITH_AXE)) continue;
            if (!hasEnoughEnergy(stack)) break;

            float hardness = extraState.getDestroySpeed(level, extra);
            if (hardness < 0 || hardness > 5.0f) continue;

            consumeEnergy(stack, ENERGY_PER_BLOCK);
            level.destroyBlock(extra, true, player);
        }

        return true;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) return false;
        return state.is(BlockTags.MINEABLE_WITH_AXE);
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return hasEnoughEnergy(player.getMainHandItem());
    }

    // ── 3×3 HELPER ────────────────────────────────────────────────────────────

    /**
     * Fells an entire tree by BFS from the broken log.
     * Stops at leaves and non-log blocks.
     */
    private static void fellTree(ItemStack stack, Level level, BlockPos origin, Player player) {
        java.util.Queue<BlockPos> queue = new java.util.ArrayDeque<>();
        java.util.Set<BlockPos> visited = new java.util.HashSet<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && hasEnoughEnergy(stack)) {
            BlockPos current = queue.poll();
            BlockState currentState = level.getBlockState(current);

            if (!currentState.is(BlockTags.LOGS)) continue;

            consumeEnergy(stack, ENERGY_PER_BLOCK);
            level.destroyBlock(current, true, player);

            // Search adjacent blocks for more logs (including diagonals)
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 1; dy++) { // only up — don't go down
                    for (int dz = -1; dz <= 1; dz++) {
                        BlockPos neighbor = current.offset(dx, dy, dz);
                        if (visited.contains(neighbor)) continue;
                        if (!level.isLoaded(neighbor)) continue;
                        if (level.getBlockState(neighbor).is(BlockTags.LOGS)) {
                            visited.add(neighbor);
                            queue.add(neighbor);
                        }
                    }
                }
            }

            // Safety cap — don't fell absurdly large trees
            if (visited.size() > 256) break;
        }
    }


    /**
     * Returns a 3×3 grid of positions centered on pos,
     * perpendicular to the face the player is looking at.
     */
    public static List<BlockPos> get3x3Area(BlockPos origin, Player player) {
        List<BlockPos> positions = new ArrayList<>();
        Direction facing = Direction.getNearest(
                player.getLookAngle().x,
                player.getLookAngle().y,
                player.getLookAngle().z);

        // The two axes perpendicular to the facing direction
        Direction[] perp = getPerpendicularAxes(facing);
        Direction a = perp[0];
        Direction b = perp[1];

        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                BlockPos p = origin
                        .relative(a, i)
                        .relative(b, j);
                positions.add(p);
            }
        }
        return positions;
    }

    private static Direction[] getPerpendicularAxes(Direction facing) {
        return switch (facing) {
            case UP, DOWN -> new Direction[]{ Direction.NORTH, Direction.EAST };
            case NORTH, SOUTH -> new Direction[]{ Direction.EAST, Direction.UP };
            case EAST, WEST -> new Direction[]{ Direction.NORTH, Direction.UP };
        };
    }

    // ── DURABILITY — no vanilla durability, energy only ───────────────────────

    @Override
    public void appendHoverText(ItemStack stack,
                                net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        int energy = ArsenalSawItem.getEnergy(stack);
        float pct  = (float) energy / MAX_ENERGY * 100f;
        tooltip.add(net.minecraft.network.chat.Component.literal(
                        String.format("⚡ %,d / %,d FE (%.1f%%)", energy, MAX_ENERGY, pct))
                .withStyle(pct > 60 ? net.minecraft.ChatFormatting.GREEN
                        : pct > 25 ? net.minecraft.ChatFormatting.YELLOW
                        : net.minecraft.ChatFormatting.RED));
        if (energy <= 0)
            tooltip.add(net.minecraft.network.chat.Component.literal(
                            "No power — charge in a Power Bank")
                    .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
    }

    @Override
    public boolean isEnchantable(ItemStack stack) { return false; }

    @Override
    public boolean isFoil(ItemStack stack) {
        // Glow when charged
        return getEnergy(stack) > 0;
    }

    @Override
    public boolean isDamageable(ItemStack stack) { return false; }

    // ── TIER ─────────────────────────────────────────────────────────────────

    private enum SawTier implements Tier {
        INSTANCE;

        @Override public int getUses() { return Integer.MAX_VALUE; } // no durability
        @Override public float getSpeed() { return STONE_SPEED; }
        @Override public float getAttackDamageBonus() { return 1.5f; }
        @Override public net.minecraft.tags.TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.NEEDS_STONE_TOOL;
        }
        @Override public int getEnchantmentValue() { return 0; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    }
}