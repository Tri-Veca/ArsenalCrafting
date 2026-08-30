package com.arsenalcrafting.mod.loot;

import com.arsenalcrafting.mod.core.alloy.AlloyMaterial;
import com.arsenalcrafting.mod.registry.ModItems;
import com.arsenalcrafting.mod.registry.ModLootModifiers;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.neoforge.common.loot.AddTableLootModifier;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

public class GravelLootModifier extends LootModifier {

    public static final MapCodec<GravelLootModifier> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, GravelLootModifier::new));

    // Flint drops at ~14.3% (1/7) chance — we go lower for rarity
    private static final float RAW_TIN_CHANCE = 0.1f;

    public GravelLootModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> generatedLoot,
                                                 LootContext context) {
        // Check if the block being broken is gravel
        var blockState = context.getParam(net.minecraft.world.level.storage.loot.parameters.LootContextParams.BLOCK_STATE);
        if (blockState == null || blockState.getBlock() != net.minecraft.world.level.block.Blocks.GRAVEL) {
            return generatedLoot;
        }

        if (context.getRandom().nextFloat() < RAW_TIN_CHANCE) {
            var rawTin = ModItems.RAW_ITEMS.get(AlloyMaterial.TIN);
            if (rawTin != null) {
                generatedLoot.add(new ItemStack(rawTin.get()));
            }
        }
        return generatedLoot;
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}