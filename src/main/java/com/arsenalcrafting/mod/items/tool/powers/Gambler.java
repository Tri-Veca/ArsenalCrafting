package com.arsenalcrafting.mod.items.tool.powers;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

import java.util.List;

public class Gambler {
    private static final float PROC_CHANCE = 0.07f; // 7% chance

    // Statistics tracking
    private static int totalCalls = 0;
    private static int successfulProcs = 0;
    private static int rareDrops = 0;
    private static int uncommonDrops = 0;
    private static int commonDrops = 0;

    // LOOT TABLES (like treasure chests!)
    private static final List<ItemStack> MINING_COMMON = List.of(
            new ItemStack(Items.TORCH, 1),
            new ItemStack(Items.COAL, 1),
            new ItemStack(Items.RAW_COPPER, 1)
    );

    private static final List<ItemStack> MINING_UNCOMMON = List.of(
            new ItemStack(Items.RAW_IRON, 1),
            new ItemStack(Items.RAW_GOLD, 1),
            new ItemStack(Items.LAPIS_LAZULI, 1)
    );

    private static final List<ItemStack> MINING_RARE = List.of(
            new ItemStack(Items.DIAMOND, 1),
            new ItemStack(Items.EMERALD, 1),
            new ItemStack(Items.AMETHYST_SHARD, 1)
    );

    // 📥 Method: Try to drop bonus loot when mining
    public static boolean tryProcMining(Level level, BlockPos pos, RandomSource random) {
        totalCalls++;

        System.out.println("[Gambler] Call #" + totalCalls + " - Rolling dice...");

        float roll = random.nextFloat();
        System.out.println("[Gambler] Rolled: " + roll + " (need < " + PROC_CHANCE + ")");

        if (roll < PROC_CHANCE) {
            successfulProcs++;

            // Calculate current success rate
            float currentRate = (successfulProcs * 100.0f) / totalCalls;
            System.out.println("[Gambler] PROC #" + successfulProcs + "! Rate: " +
                    String.format("%.1f", currentRate) + "%");

            // Pick rarity
            float rarityRoll = random.nextFloat();
            List<ItemStack> loot;

            if (rarityRoll < 0.02f) {
                loot = MINING_RARE;
                rareDrops++;
                System.out.println("[Gambler] RARE loot! (Total rares: " + rareDrops + ")");
            } else if (rarityRoll < 0.27f) {
                loot = MINING_UNCOMMON;
                uncommonDrops++;
                System.out.println("[Gambler] UNCOMMON loot!");
            } else {
                loot = MINING_COMMON;
                commonDrops++;
                System.out.println("[Gambler] COMMON loot");
            }

            // Drop the item!
            ItemStack drop = loot.get(random.nextInt(loot.size())).copy();
            Block.popResource(level, pos, drop);
            System.out.println("[Gambler] Dropped: " + drop.getItem().getName(drop).getString());

            // Print statistics every 10 procs
            if (successfulProcs % 10 == 0) {
                printStatistics();
            }

            return true;
        }

        System.out.println("[Gambler]  No bonus this time");

        // Print statistics every 50 total calls
        if (totalCalls % 50 == 0) {
            printStatistics();
        }

        return false;
    }

    private static void printStatistics() {
        System.out.println("\n=== GAMBLER STATISTICS ===");
        System.out.println("Total ore mined: " + totalCalls);
        System.out.println("Successful procs: " + successfulProcs);
        System.out.println("Success rate: " +
                String.format("%.2f", (successfulProcs * 100.0f) / totalCalls) + "%");
        System.out.println("Rare drops: " + rareDrops +
                " (" + String.format("%.2f", (rareDrops * 100.0f) / totalCalls) + "%)");
        System.out.println("Uncommon drops: " + uncommonDrops);
        System.out.println("Common drops: " + commonDrops);
        System.out.println("Expected success: 7.00%");
        System.out.println("Expected rare rate: 0.14%");
        System.out.println("=========================\n");
    }

    // 🗡️ Method: Try to drop bonus loot when killing (for weapons)
    public static boolean tryProcKilling(LivingEntity target, RandomSource random) {
        if (random.nextFloat() < PROC_CHANCE) {
            // Similar loot table for mobs (you'd fill this in)
            // ...
            return true;
        }
        return false;
    }
}