package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.CastingStationBlockEntity;
import com.arsenalcrafting.mod.component.ToolComponentType;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.arsenalcrafting.mod.registry.ModBlocks;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

public class CastingStationBlock extends BaseEntityBlock {
    public static final MapCodec<CastingStationBlock> CODEC = simpleCodec(CastingStationBlock::new);

    public CastingStationBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CastingStationBlockEntity(pos, state);
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                  BlockEntityType<T> type) {
        if (level.isClientSide()) return null;
        return createTickerHelper(type, ModBlockEntities.CASTING_STATION.get(),
                (level1, pos, state1, be) -> CastingStationBlockEntity.tick(level1, pos, state1, be));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level,
                                              BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hitResult) {
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CastingStationBlockEntity station)) return ItemInteractionResult.FAIL;

        // ── SHIFT CLICK — remove mold or bucket ───────────────────────────────
        if (player.isShiftKeyDown()) {
            if (station.isSetting()) {
                player.displayClientMessage(Component.literal("Cannot remove while casting"), true);
                return ItemInteractionResult.SUCCESS;
            }
            if (station.getMode() == CastingStationBlockEntity.CastMode.MOLD
                    && station.getMoldComponentId() != null
                    && station.getFluidAmount() == 0) {
                String moldId = station.getMoldComponentId();

                // Check if it's an ingot mold first
                if (moldId.equals("ingot")) {
                    if (station.isBronzeMold()) {
                        Block.popResource(level, pos, new ItemStack(ModBlocks.BRONZE_INGOT_MOLD.get().asItem()));
                    } else {
                        Block.popResource(level, pos, new ItemStack(ModBlocks.INGOT_MOLD.get().asItem()));
                    }
                } else {
                    ToolComponentType type = ToolComponentType.fromId(moldId);
                    if (type != null) {
                        var moldHolder = station.isBronzeMold()
                                ? ModBlocks.BRONZE_MOLD_BLOCKS.get(type)
                                : ModBlocks.MOLD_BLOCKS.get(type);
                        if (moldHolder != null)
                            Block.popResource(level, pos, new ItemStack(moldHolder.get().asItem()));
                    }
                }
                station.clearMold();
                player.displayClientMessage(Component.literal("Mold removed"), true);
                return ItemInteractionResult.SUCCESS;
            }
            if (station.getMode() == CastingStationBlockEntity.CastMode.BUCKET
                    && station.getFluidAmount() == 0) {
                Block.popResource(level, pos, new ItemStack(Items.BUCKET));
                station.clearBucket();
                player.displayClientMessage(Component.literal("Bucket removed"), true);
                return ItemInteractionResult.SUCCESS;
            }
            player.displayClientMessage(Component.literal("Nothing to remove"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // ── TAKE FINISHED ITEM ────────────────────────────────────────────────
        if (station.isReady()) {
            ItemStack result = station.collectOutput();
            if (!result.isEmpty()) {
                if (!player.addItem(result)) {
                    player.drop(result, false);
                }
                player.displayClientMessage(Component.literal("Collected " + result.getHoverName().getString()), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        // Don't allow changes while active
        if (station.isSetting()) return ItemInteractionResult.FAIL;
        if (station.getFluidAmount() > 0) return ItemInteractionResult.FAIL;
        if (station.getMode() != CastingStationBlockEntity.CastMode.NONE) return ItemInteractionResult.FAIL;

        // ── EMPTY BUCKET ──────────────────────────────────────────────────────
        if (stack.getItem() == Items.BUCKET) {
            station.setBucket();
            if (!player.isCreative()) stack.shrink(1);
            player.displayClientMessage(
                    Component.literal("Bucket placed — ready to receive fluid"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // ── INGOT MOLD (regular) ──────────────────────────────────────────────
        if (stack.getItem() == ModBlocks.INGOT_MOLD.get().asItem()) {
            station.setMold("ingot", false);
            if (!player.isCreative()) stack.shrink(1);
            player.displayClientMessage(Component.literal("Ingot mold placed"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // ── BRONZE INGOT MOLD ─────────────────────────────────────────────────
        if (stack.getItem() == ModBlocks.BRONZE_INGOT_MOLD.get().asItem()) {
            station.setMold("ingot", true);
            if (!player.isCreative()) stack.shrink(1);
            player.displayClientMessage(Component.literal("Bronze ingot mold placed"), true);
            return ItemInteractionResult.SUCCESS;
        }

        // ── STANDARD MOLDS ────────────────────────────────────────────────────
        for (var entry : ModBlocks.MOLD_BLOCKS.entrySet()) {
            if (stack.getItem() == entry.getValue().get().asItem()) {
                station.setMold(entry.getKey().getId(), false);
                if (!player.isCreative()) stack.shrink(1);
                player.displayClientMessage(
                        Component.literal(entry.getKey().getDisplayName() + " mold placed"), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        // ── BRONZE MOLDS ──────────────────────────────────────────────────────
        for (var entry : ModBlocks.BRONZE_MOLD_BLOCKS.entrySet()) {
            if (stack.getItem() == entry.getValue().get().asItem()) {
                station.setMold(entry.getKey().getId(), true);
                if (!player.isCreative()) stack.shrink(1);
                player.displayClientMessage(
                        Component.literal("Bronze " + entry.getKey().getDisplayName() + " mold placed"), true);
                return ItemInteractionResult.SUCCESS;
            }
        }

        return ItemInteractionResult.FAIL;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                               Player player, BlockHitResult hitResult) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof CastingStationBlockEntity station)) return InteractionResult.PASS;

        // ── COLLECT OUTPUT ────────────────────────────────────────────────────
        if (station.isReady()) {
            ItemStack output = station.collectOutput();
            if (!output.isEmpty()) {
                if (!player.addItem(output)) player.drop(output, false);
                player.displayClientMessage(Component.literal("Cast complete!"), true);
            }
            return InteractionResult.SUCCESS;
        }

        // ── STATUS MESSAGES ───────────────────────────────────────────────────
        if (station.isSetting()) {
            int pct = station.getSettingTime() > 0
                    ? (station.getSettingProgress() * 100 / station.getSettingTime()) : 0;
            player.displayClientMessage(
                    Component.literal("Setting... " + pct + "%"), true);
            return InteractionResult.SUCCESS;
        }

        if (station.getMode() == CastingStationBlockEntity.CastMode.NONE) {
            player.displayClientMessage(
                    Component.literal("Place a mold or empty bucket, then pour fluid with a valve faucet"), true);
            return InteractionResult.SUCCESS;
        }

        if (station.getFluidAmount() == 0) {
            player.displayClientMessage(
                    Component.literal("Ready — pour fluid using a valve faucet"), true);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos,
                            BlockState newState, boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()) {
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof CastingStationBlockEntity station) {
                if (station.getMode() == CastingStationBlockEntity.CastMode.BUCKET
                        && station.getFluidAmount() == 0) {
                    Block.popResource(level, pos, new ItemStack(Items.BUCKET));
                }
                if (station.getMode() == CastingStationBlockEntity.CastMode.MOLD
                        && station.getMoldComponentId() != null
                        && station.getFluidAmount() == 0) {
                    String moldId = station.getMoldComponentId();

                    // Check if it's an ingot mold
                    if (moldId.equals("ingot")) {
                        if (station.isBronzeMold()) {
                            Block.popResource(level, pos, new ItemStack(ModBlocks.BRONZE_INGOT_MOLD.get().asItem()));
                        } else {
                            Block.popResource(level, pos, new ItemStack(ModBlocks.INGOT_MOLD.get().asItem()));
                        }
                    } else {
                        ToolComponentType type = ToolComponentType.fromId(moldId);
                        if (type != null) {
                            var moldHolder = station.isBronzeMold()
                                    ? ModBlocks.BRONZE_MOLD_BLOCKS.get(type)
                                    : ModBlocks.MOLD_BLOCKS.get(type);
                            if (moldHolder != null)
                                Block.popResource(level, pos, new ItemStack(moldHolder.get().asItem()));
                        }
                    }
                }
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}