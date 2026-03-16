package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.GeneratorBlockEntity;
import com.arsenalcrafting.mod.blocks.entity.PowerBankBlockEntity;
import com.arsenalcrafting.mod.client.menu.FossilGeneratorMenu;
import com.arsenalcrafting.mod.client.menu.PowerBankMenu;
import com.arsenalcrafting.mod.power.GeneratorType;
import com.arsenalcrafting.mod.power.PowerTier;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;


//TODO: Fix the bug where the max capacity for a genertor changes to 80 (Bronze on all the generators)

/**
 * Fossil Fuel Generator — burns coal/charcoal/coal blocks → FE.
 * One fuel slot, tier-scaled output. Priority 1 generator type.
 */
public class FossilGeneratorBlock extends BaseGeneratorBlock {

    private final PowerTier tier;

    public FossilGeneratorBlock(Properties props, PowerTier tier) {
        super(props);
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GeneratorBlockEntity(
                GeneratorType.FOSSIL, tier, pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level,
                                            BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof GeneratorBlockEntity bank) {
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new FossilGeneratorMenu(id, inv, bank),
                        Component.translatable("block.arsenalcrafting.fossil_generator")
                ), buf -> buf.writeBlockPos(pos));
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        // Codec not needed for block entities — returning null is fine for non-serialized blocks
        return null;
    }
}