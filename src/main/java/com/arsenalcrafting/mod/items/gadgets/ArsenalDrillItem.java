package com.arsenalcrafting.mod.items.gadgets;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Drill — power-consuming 3×3 area mining tool.
 *
 * - Works on shovel blocks (dirt, sand, gravel, clay, snow, etc.)
 * - Also handles stone/ore like a pickaxe (dual mode)
 * - Stone tool mining speed
 * - Consumes energy per block — no durability
 * - Dead drill mines at hand speed
 *
 * Power stored in item NBT, same pattern as ArsenalSawItem.
 */
public class ArsenalDrillItem extends ShovelItem {

    // ── CONSTANTS ─────────────────────────────────────────────────────────────

    public static final int MAX_ENERGY      = 10_000;
    public static final int ENERGY_PER_BLOCK = 50;
    public static final int TRANSFER_RATE   = 1_000;

    private static final float STONE_SPEED  = 1.5f;

    // ── CONSTRUCTOR ───────────────────────────────────────────────────────────

    public ArsenalDrillItem(Properties properties) {
        super(DrillTier.INSTANCE, properties.stacksTo(1));
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

    /** Returns true if this block is an ore — drill cannot mine ores. */
    private static boolean isOre(BlockState state) {
        return state.is(BlockTags.COAL_ORES)
                || state.is(BlockTags.COPPER_ORES)
                || state.is(BlockTags.DIAMOND_ORES)
                || state.is(BlockTags.EMERALD_ORES)
                || state.is(BlockTags.GOLD_ORES)
                || state.is(BlockTags.IRON_ORES)
                || state.is(BlockTags.LAPIS_ORES)
                || state.is(BlockTags.REDSTONE_ORES);

    }

    private static boolean isDrillable(BlockState state) {
        return (state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || state.is(BlockTags.MINEABLE_WITH_PICKAXE))
                && !isOre(state);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) return 1.0f;
        if (!isDrillable(state)) return 1.0f;
        return STONE_SPEED;
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        if (!hasEnoughEnergy(stack)) return false;
        return isDrillable(state);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state,
                             BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return super.mineBlock(stack, level, state, pos, entity);
        }

        if (!hasEnoughEnergy(stack)) return false;

        consumeEnergy(stack, ENERGY_PER_BLOCK);

        // Mine 3×3 area — shovel + pickaxe blocks, no ores
        List<BlockPos> area = ArsenalSawItem.get3x3Area(pos, player);
        for (BlockPos extra : area) {
            if (extra.equals(pos)) continue;
            if (!level.isLoaded(extra)) continue;

            BlockState extraState = level.getBlockState(extra);
            if (extraState.isAir()) continue;
            if (!isDrillable(extraState)) continue;
            if (!hasEnoughEnergy(stack)) break;

            float hardness = extraState.getDestroySpeed(level, extra);
            if (hardness < 0 || hardness > 5.0f) continue;

            consumeEnergy(stack, ENERGY_PER_BLOCK);
            level.destroyBlock(extra, true, player);
        }

        return true;
    }

    @Override
    public boolean canAttackBlock(BlockState state, Level level, BlockPos pos, Player player) {
        return hasEnoughEnergy(player.getMainHandItem());
    }

    // ── DURABILITY ────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack,
                                net.minecraft.world.item.Item.TooltipContext context,
                                java.util.List<net.minecraft.network.chat.Component> tooltip,
                                net.minecraft.world.item.TooltipFlag flag) {
        int energy = ArsenalDrillItem.getEnergy(stack);
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

    @Override public boolean isEnchantable(ItemStack stack) { return false; }
    @Override public boolean isDamageable(ItemStack stack) { return false; }

    @Override
    public boolean isFoil(ItemStack stack) {
        return getEnergy(stack) > 0;
    }

    // ── TIER ─────────────────────────────────────────────────────────────────

    private enum DrillTier implements Tier {
        INSTANCE;

        @Override public int getUses() { return Integer.MAX_VALUE; }
        @Override public float getSpeed() { return STONE_SPEED; }
        @Override public float getAttackDamageBonus() { return 2.0f; }
        @Override public net.minecraft.tags.TagKey<Block> getIncorrectBlocksForDrops() {
            return BlockTags.NEEDS_IRON_TOOL;
        }
        @Override public int getEnchantmentValue() { return 0; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.EMPTY; }
    }
}