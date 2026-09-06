package com.arsenalcrafting.mod.items.tool.powers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;

public final class PerfectStrike {
    private PerfectStrike() {}

    private static final float CHANCE = 0.08f;
    private static float extraStrikeDamage = 0f;

    public static float getExtraStrikeDamage() {
        return extraStrikeDamage;
    }

    public static void tryPerfectStrike(ItemStack tool, Level level, BlockState state, BlockPos pos, LivingEntity entity) {
        if (level.isClientSide) return;
        if (!(entity instanceof Player)) return;
        if (level.random.nextFloat() >= CHANCE) return;

        level.destroyBlock(pos, true, entity);

        if (isOre(state)) {
            level.destroyBlock(pos, false, entity);
        }

        if (level instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    10, 0.3, 0.3, 0.3, 0.1);
        }

        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.PLAYERS, 0.5f, 1.2f);
    }

    public static void tryExtraStrike(
            ItemStack weapon,
            LivingEntity attacker,
            Entity target,
            float damage
    ) {
        // Reset the result for THIS attack
        extraStrikeDamage = 0f;

        if (attacker.level().isClientSide) return;
        if (!(attacker instanceof Player player)) return;
        if (attacker.level().random.nextFloat() >= CHANCE) return;

        // Extra Strike triggered
        extraStrikeDamage = damage;

        System.out.println(
                "[PerfectStrike] Extra Strike triggered! Damage: " + damage
        );

        if (target instanceof LivingEntity livingTarget) {
            livingTarget.hurt(
                    attacker.damageSources().playerAttack(player),
                    damage
            );
        }
    }

    private static boolean isOre(BlockState state) {
        Block b = state.getBlock();
        return b == Blocks.IRON_ORE || b == Blocks.DEEPSLATE_IRON_ORE ||
                b == Blocks.GOLD_ORE || b == Blocks.DEEPSLATE_GOLD_ORE ||
                b == Blocks.COPPER_ORE || b == Blocks.DEEPSLATE_COPPER_ORE ||
                b == Blocks.COAL_ORE || b == Blocks.DEEPSLATE_COAL_ORE ||
                b == Blocks.DIAMOND_ORE || b == Blocks.DEEPSLATE_DIAMOND_ORE ||
                b == Blocks.EMERALD_ORE || b == Blocks.DEEPSLATE_EMERALD_ORE ||
                b == Blocks.LAPIS_ORE || b == Blocks.DEEPSLATE_LAPIS_ORE ||
                b == Blocks.REDSTONE_ORE || b == Blocks.DEEPSLATE_REDSTONE_ORE ||
                b == Blocks.NETHER_QUARTZ_ORE ||
                b == Blocks.NETHER_GOLD_ORE ||
                b == Blocks.ANCIENT_DEBRIS;
    }
}