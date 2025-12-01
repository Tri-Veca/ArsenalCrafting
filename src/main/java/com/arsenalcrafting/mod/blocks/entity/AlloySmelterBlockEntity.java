package com.arsenalcrafting.mod.blocks.entity;

import com.arsenalcrafting.mod.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

// Placeholder for the complex multiblock core handling liquid metals and alloying.
// This empty class structure resolves the dependency in ModBlockEntities.
//will change from core to controller
public class AlloySmelterBlockEntity extends BlockEntity implements MenuProvider {
    public AlloySmelterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ALLOY_SMELTER_CORE.get(), pos, state);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.arsenalcrafting.alloy_smelter_core");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        // Returns null for now until AlloySmelterMenu is implemented
        return null;
    }
}