package com.arsenalcrafting.mod.blocks;

import com.arsenalcrafting.mod.blocks.entity.ToolkitBlockEntity;
import com.arsenalcrafting.mod.client.menu.ToolkitMenu;
import com.arsenalcrafting.mod.registry.ModBlockEntities;
import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// Toolkit is a functional block that opens a GUI (menu) and stores parts to craft the modular tool.
public class ToolkitBlock extends BaseEntityBlock {

    // 1. Define the MapCodec for serialization/deserialization.
    public static final MapCodec<ToolkitBlock> CODEC = simpleCodec(ToolkitBlock::new);

    // 2. Override the codec() method to return our custom CODEC.
    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    public ToolkitBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH));
    }

    // --- State and Placement Logic ---

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.HORIZONTAL_FACING);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        // Sets the block to face the player, ensuring it's opposite the direction they are facing.
        return this.defaultBlockState().setValue(BlockStateProperties.HORIZONTAL_FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        // BaseEntityBlock requires this to render the block model correctly.
        return RenderShape.MODEL;
    }



    /**
     * Handles block interaction (right-click) logic. This is the canonical override
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            // Client side returns success to visually swing the arm.
            return InteractionResult.SUCCESS;
        }

        // Server side logic to open the GUI
        BlockEntity entity = level.getBlockEntity(pos);

        if (entity instanceof ToolkitBlockEntity toolkitEntity && player instanceof ServerPlayer serverPlayer) {

            // Create the menu provider for the custom GUI
            MenuProvider menuProvider = new SimpleMenuProvider(
                    (id, inventory, pPlayer) -> new ToolkitMenu(id, inventory, toolkitEntity),
                    Component.translatable("screen.arsenalcrafting.toolkit_title")
            );

            // Open the GUI menu, passing the BlockPos for context
            serverPlayer.openMenu(menuProvider, pos);


            return InteractionResult.CONSUME;
        }

        return InteractionResult.PASS;
    }

    // --- Block Entity Management ---

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ToolkitBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        // Registers the tick method for the block entity if it matches the registered type.
        return createTickerHelper(type, ModBlockEntities.TOOLKIT.get(), ToolkitBlockEntity::tick);
    }

    // Called when the block is broken or replaced
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving) {
        // Check if the block being removed is actually this block, not just a state change.
        if (!state.is(newState.getBlock())) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ToolkitBlockEntity toolkitEntity) {
                // Drop inventory contents
                toolkitEntity.drops();
            }
            // Ensure the super method is called last
            super.onRemove(state, level, pos, newState, isMoving);
        }
    }
}